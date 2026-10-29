package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Raiz do agregado Contract: cabeçalho (o que vale para o contrato inteiro) + itens
// (um por grupo de preço contratado). Toda criação e alteração de itens passa por
// aqui. Ver domain-model.md, "Contract e ContractItem".
@Entity
@Table(name = "contract")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Titular: nunca muda (trocar o titular = contrato novo)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private PatientEntity beneficiary;

    // Plano único do contrato: nunca muda (trocar o plano = encerrar e fazer outro)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    // Início do acordo: nunca muda (copiado em cada versão)
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Calculado pelo sistema, inclusivo (o último dia ainda conta)
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // varchar, não o enum nativo do MySQL: valor novo no enum não exige migration (ADR 0021)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean active = true;

    // Versão anterior deste contrato (aditivo). Null na primeira versão.
    // unique: cada versão tem no máximo uma sucessora — impede que duas alterações
    // simultâneas do mesmo contrato gerem duas versões "filhas".
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id", unique = true)
    private ContractEntity previousContract;

    // Itens: parte do agregado — salvos e removidos junto com o contrato (cascade).
    // Coleção permitida porque item e contrato são o mesmo agregado e o mesmo
    // módulo (a proibição de coleção de volta vale entre módulos).
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private List<ContractItemEntity> items = new ArrayList<>();

    // ---------------------------------------------------------------------------
    // Criação (primeira versão do contrato)
    // ---------------------------------------------------------------------------

    public static ContractEntity create(PatientEntity patient, PatientEntity beneficiary, PlanEntity plan,
            LocalDate startDate, PaymentMethod paymentMethod, List<ContractItemDraft> drafts) {
        ContractEntity contract = new ContractEntity(null, patient, beneficiary, plan, startDate, paymentMethod);
        contract.addItems(drafts, null);
        contract.endDate = contract.calculateEndDate();
        return contract;
    }

    // ---------------------------------------------------------------------------
    // Alteração (aditivo): o contrato nunca é editado no lugar. Cada mudança gera
    // uma nova versão, que aponta para esta (previousContract), e esta é inativada.
    // Titular, plano e início são sempre copiados.
    // ---------------------------------------------------------------------------

    // amendmentDate = data do aditivo (hoje), passada pelo service. requestedEndDate
    // é opcional: só para estender (prorrogação).
    public ContractEntity amend(PatientEntity beneficiary, PaymentMethod paymentMethod,
            LocalDate requestedEndDate, List<ContractItemDraft> drafts, LocalDate amendmentDate) {
        requireAmendable();
        ContractEntity newVersion = new ContractEntity(this, this.patient, beneficiary,
                this.plan, this.startDate, paymentMethod);
        newVersion.addItems(drafts, this);
        newVersion.endDate = newVersion.calculateAmendedEndDate(this, requestedEndDate, amendmentDate);
        this.active = false;
        return newVersion;
    }

    // Soft delete da versão. Uma versão já inativa (substituída por aditivo ou já
    // encerrada) não pode ser inativada de novo: mesmo 409 do requireAmendable.
    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // Cópia somente leitura: itens só mudam pelo contrato (criação e aditivo)
    public List<ContractItemEntity> getItems() {
        return List.copyOf(items);
    }

    // Item do grupo informado nesta versão (contrato + grupo = item)
    public Optional<ContractItemEntity> findItemOf(PricingGroupEntity pricingGroup) {
        return items.stream()
                .filter(item -> item.getPricingGroup().getId().equals(pricingGroup.getId()))
                .findFirst();
    }

    // Ids desta versão e de todas as anteriores (o acordo inteiro até aqui), da mais
    // nova para a mais antiga. O saldo de sessões soma os atendimentos da cadeia toda,
    // porque cada aditivo é uma versão nova. Usa os getters, não os campos: a versão
    // anterior é LAZY e pode vir como proxy do Hibernate, cujos campos ficam vazios.
    public List<Long> getVersionChainIds() {
        List<Long> ids = new ArrayList<>();
        for (ContractEntity version = this; version != null; version = version.getPreviousContract()) {
            ids.add(version.getId());
        }
        return ids;
    }

    // Vigência em semanas (arredondada para cima) — usada nas opções do plano do Grupo.
    // long: é o tipo que o ChronoUnit devolve; convertido para int, o tipo das semanas
    // no resto do código.
    public int getDurationInWeeks() {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (int) ((days + 6) / 7);
    }

    // Privado: toda criação passa por create/amend. "previous" é a versão anterior
    // (null na criação): o que for igual a ela é aceito como está.
    private ContractEntity(ContractEntity previous, PatientEntity patient, PatientEntity beneficiary,
            PlanEntity plan, LocalDate startDate, PaymentMethod paymentMethod) {
        validateParties(previous, patient, beneficiary);
        validatePlan(previous, plan);
        this.previousContract = previous;
        this.patient = patient;
        this.beneficiary = beneficiary;
        this.plan = plan;
        this.startDate = startDate;
        this.paymentMethod = paymentMethod;
        this.active = true;
    }

    // ---------------------------------------------------------------------------
    // Itens
    // ---------------------------------------------------------------------------

    // Cada item é comparado com o item do mesmo grupo na versão anterior (se houver).
    // Item incluído = grupo novo (sem anterior). Item removido = grupo que não veio.
    private void addItems(List<ContractItemDraft> drafts, ContractEntity previous) {
        validateItemsNotEmptyAndOnePerGroup(drafts);
        validateItemsMatchPlan(drafts);
        for (ContractItemDraft draft : drafts) {
            ContractItemEntity previousItem = previous != null
                    ? previous.findItemOf(draft.pricingGroup()).orElse(null)
                    : null;
            items.add(new ContractItemEntity(this, draft, previousItem));
        }
    }

    // ---------------------------------------------------------------------------
    // Fim do contrato
    // ---------------------------------------------------------------------------

    // Criação: o fim sai do prazo calculado pelos ritmos dos itens por duração
    private LocalDate calculateEndDate() {
        List<SessionFrequency> frequencies = items.stream()
                .filter(ContractItemEntity::isDurationBased)
                .map(ContractItemEntity::getFrequency)
                .toList();
        return endDateFor(startDate, durationWeeksFor(plan, frequencies));
    }

    // Prazo do contrato em semanas: maior prazo entre os itens por duração (o Grupo
    // não entra — acompanha o contrato). Contrato só com Grupo (plano de mensalidade):
    // prazo do plano (termWeeks). Público e estático para a prévia do formulário
    // (GET /api/contracts/term-preview) usar exatamente a mesma conta.
    public static int durationWeeksFor(PlanEntity plan, List<SessionFrequency> durationItemFrequencies) {
        validateContractable(plan);
        if (plan.getType() == PlanType.MONTHLY_FEE) {
            return plan.getTermWeeks();
        }
        if (durationItemFrequencies == null || durationItemFrequencies.isEmpty()) {
            throw new BusinessRuleException(
                    "Contrato de pacote precisa de pelo menos um item por duração. Contrato só com Pilates em Grupo usa um plano de mensalidade.");
        }
        durationItemFrequencies.forEach(frequency -> ContractItemEntity.validateFitsMaxTerm(frequency, plan));
        return durationItemFrequencies.stream()
                .mapToInt(frequency -> frequency.weeksToComplete(plan.getSessionCount()))
                .max()
                .getAsInt();
    }

    // endDate inclusivo: início + prazo − 1 dia (ex: 03/11 + 12 semanas → 25/01)
    public static LocalDate endDateFor(LocalDate startDate, int weeks) {
        return startDate.plusWeeks(weeks).minusDays(1);
    }

    // Aditivo: mantém o fim atual. Item por duração incluído (grupo novo) começa a
    // valer na data do aditivo e pode estender o fim; o fim pedido (prorrogação)
    // também. O contrato nunca encolhe. Mudar a frequência não recalcula.
    private LocalDate calculateAmendedEndDate(ContractEntity previous, LocalDate requestedEndDate,
            LocalDate amendmentDate) {
        LocalDate end = previous.endDate;
        for (ContractItemEntity item : items) {
            boolean newDurationItem = item.isDurationBased() && previous.findItemOf(item.getPricingGroup()).isEmpty();
            if (newDurationItem) {
                LocalDate itemEnd = endDateFor(amendmentDate, item.weeksToComplete());
                end = itemEnd.isAfter(end) ? itemEnd : end;
            }
        }
        if (requestedEndDate != null) {
            validateRequestedEndDate(requestedEndDate, previous.endDate);
            end = requestedEndDate.isAfter(end) ? requestedEndDate : end;
        }
        return end;
    }

    // ---------------------------------------------------------------------------
    // Validações
    // ---------------------------------------------------------------------------

    // Só a versão ativa pode ser alterada: uma versão inativa já foi substituída
    // ou encerrada. Contrato ativo porém vencido pode (é assim que se prorroga).
    private void requireAmendable() {
        if (!active) {
            throw new ConflictException("Só a versão ativa do contrato pode ser alterada.");
        }
    }

    private void requireActive() {
        if (!active) {
            throw new ConflictException("O contrato já está inativo.");
        }
    }

    // Registros inativos (soft delete) existem, mas não podem ENTRAR num contrato.
    // Na alteração, o titular é sempre copiado e o beneficiário mantido é aceito.
    private static void validateParties(ContractEntity previous, PatientEntity patient, PatientEntity beneficiary) {
        if (previous == null && !patient.isActive()) {
            throw new BusinessRuleException("O paciente titular está inativo.");
        }
        Long previousBeneficiaryId = previous != null && previous.beneficiary != null
                ? previous.beneficiary.getId() : null;
        if (beneficiary != null && !beneficiary.isActive() && !beneficiary.getId().equals(previousBeneficiaryId)) {
            throw new BusinessRuleException("O beneficiário está inativo.");
        }
    }

    // O Avulso nunca entra em contrato. Plano novo precisa estar ativo (no aditivo o
    // plano é copiado da versão anterior, então não é revalidado).
    private static void validatePlan(ContractEntity previous, PlanEntity plan) {
        validateContractable(plan);
        if (previous == null && !plan.isActive()) {
            throw new BusinessRuleException("O plano está inativo.");
        }
    }

    private static void validateContractable(PlanEntity plan) {
        if (!plan.getType().isContractable()) {
            throw new BusinessRuleException("O plano avulso não pode ser usado em contrato.");
        }
    }

    // Pelo menos um item (remover todos = encerrar o contrato) e um item por grupo.
    // Compara ids: relação LAZY pode vir como proxy do Hibernate.
    private static void validateItemsNotEmptyAndOnePerGroup(List<ContractItemDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new BusinessRuleException("O contrato precisa ter pelo menos um item.");
        }
        long distinctGroups = drafts.stream()
                .map(draft -> draft.pricingGroup().getId())
                .distinct()
                .count();
        if (distinctGroups != drafts.size()) {
            throw new BusinessRuleException("Cada grupo de preço só pode aparecer em um item do contrato.");
        }
    }

    // Os itens precisam ser compatíveis com o plano do contrato:
    // - plano de pacote: itens por duração usam linhas DESTE plano e pelo menos um
    //   item é por duração (contrato só com Grupo tem plano de mensalidade);
    // - plano de mensalidade: só o item do Grupo, com linha DESTE plano — por isso um
    //   contrato só com Grupo não recebe item por duração (exigiria trocar o plano).
    // O plano do item do Grupo num contrato de pacote é validado no service (depende
    // do catálogo de planos).
    private void validateItemsMatchPlan(List<ContractItemDraft> drafts) {
        if (plan.getType() == PlanType.PACKAGE && drafts.stream().noneMatch(ContractItemDraft::isDurationBased)) {
            throw new BusinessRuleException(
                    "Contrato de pacote precisa de pelo menos um item por duração. Contrato só com Pilates em Grupo usa um plano de mensalidade.");
        }
        for (ContractItemDraft draft : drafts) {
            validateItemMatchesPlan(draft);
        }
    }

    private void validateItemMatchesPlan(ContractItemDraft draft) {
        if (plan.getType() == PlanType.MONTHLY_FEE && draft.isDurationBased()) {
            throw new BusinessRuleException(
                    "Contrato com plano de mensalidade só aceita o item do Pilates em Grupo. Para incluir outros grupos, faça um contrato novo com um plano de pacote.");
        }
        Long linePlanId = draft.isDurationBased()
                ? draft.durationPrice().getPlan().getId()
                : draft.frequencyPrice().getPlan().getId();
        boolean mustUseContractPlan = draft.isDurationBased() || plan.getType() == PlanType.MONTHLY_FEE;
        if (mustUseContractPlan && !linePlanId.equals(plan.getId())) {
            throw new BusinessRuleException("O preço do item precisa ser do plano do contrato (" + plan.getName() + ").");
        }
    }

    // Prorrogação: o fim pedido precisa ser depois do fim atual (nunca encolhe)
    private static void validateRequestedEndDate(LocalDate requestedEndDate, LocalDate currentEndDate) {
        if (!requestedEndDate.isAfter(currentEndDate)) {
            throw new BusinessRuleException("A nova data de término precisa ser posterior ao fim atual do contrato.");
        }
    }
}
