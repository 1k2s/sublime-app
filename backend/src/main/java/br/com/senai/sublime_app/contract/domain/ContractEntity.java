package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.ContractStatus;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    // Número de parcelas, de 1 até os meses do contrato (Installments). O total sai das linhas
    // travadas nos itens; o valor de cada parcela sai do valor parcelado (installmentBase)
    @Column(nullable = false)
    private int installments;

    // 1º vencimento: as parcelas vencem uma por mês a partir dele, no mesmo dia
    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    // Valor parcelado: o total, no cadastro; o total − o pago, no aditivo (mínimo 0). Gravado e
    // nunca muda: o pago pode mudar depois (pagamento retroativo ou inativado), e as parcelas
    // combinadas não podem mudar sozinhas
    @Column(name = "installment_base", nullable = false, precision = 10, scale = 2)
    private Money installmentBase;

    @Column(nullable = false)
    private boolean active = true;

    // Dia em que a versão foi inativada (no aditivo, a data dele). Depois dele, nada vence
    @Column(name = "deactivated_on")
    private LocalDate deactivatedOn;

    // Versão anterior deste contrato (aditivo). Null na primeira versão.
    // unique: cada versão tem no máximo uma sucessora — impede que duas alterações
    // simultâneas do mesmo contrato gerem duas versões "filhas".
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id", unique = true)
    private ContractEntity previousContract;

    // Número do aditivo: 0 na versão original, o da anterior + 1 em cada aditivo; nunca muda.
    // Gravado para a lista não percorrer a cadeia de versões (LAZY: uma consulta por passo).
    @Column(name = "amendment_number", nullable = false)
    private int amendmentNumber;

    // Dia em que a versão foi criada (cadastro ou aditivo), "hoje" no fuso da clínica
    // (ADR 0034), passado pelo service; nunca muda. No aditivo, é a data que calculou o fim.
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    // Itens: parte do agregado — salvos e removidos junto com o contrato (cascade).
    // Coleção permitida porque item e contrato são o mesmo agregado e o mesmo
    // módulo (a proibição de coleção de volta vale entre módulos).
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private List<ContractItemEntity> items = new ArrayList<>();

    // ---------------------------------------------------------------------------
    // Criação (primeira versão do contrato)
    // ---------------------------------------------------------------------------

    // creationDate = hoje, passado pelo service (vira o createdAt da versão). firstDueDate é
    // opcional: sem ele, o 1º vencimento é o início do contrato
    public static ContractEntity create(PatientEntity patient, PatientEntity beneficiary, PlanEntity plan,
            LocalDate startDate, PaymentMethod paymentMethod, int installments, List<ContractItemDraft> drafts,
            LocalDate firstDueDate, LocalDate creationDate) {
        return create(patient, beneficiary, plan, startDate, paymentMethod, installments, drafts, firstDueDate,
                Money.of(BigDecimal.ZERO), creationDate);
    }

    // Com o crédito de contratos anteriores aplicado (o service busca no billing; zero = sem
    // crédito): o valor parcelado é o total − o crédito, no mínimo 0, e com 0 fica 1 parcela de
    // R$ 0,00, como no aditivo. O crédito que passar do total continua no contrato antigo (o
    // aplicado é o total − o valor parcelado, ver getCreditApplied)
    public static ContractEntity create(PatientEntity patient, PatientEntity beneficiary, PlanEntity plan,
            LocalDate startDate, PaymentMethod paymentMethod, int installments, List<ContractItemDraft> drafts,
            LocalDate firstDueDate, Money availableCredit, LocalDate creationDate) {
        ContractEntity contract = new ContractEntity(null, patient, beneficiary, plan, startDate, paymentMethod,
                creationDate);
        contract.addItems(drafts, null);
        contract.endDate = contract.calculateEndDate();
        contract.firstDueDate = firstDueDate != null ? firstDueDate : startDate;
        validateFirstDueDateOnCreation(contract.firstDueDate, startDate);
        contract.installmentBase = amendedInstallmentBase(contract.getTotalValue(), availableCredit);
        contract.installments = installmentsFor(contract.installmentBase, installments,
                contract.getMaxInstallments());
        return contract;
    }

    // ---------------------------------------------------------------------------
    // Alteração (aditivo): o contrato nunca é editado no lugar. Cada mudança gera
    // uma nova versão, que aponta para esta (previousContract), e esta é inativada.
    // Titular, plano e início são sempre copiados.
    // ---------------------------------------------------------------------------

    // amendmentDate = data do aditivo (hoje), passada pelo service. requestedEndDate
    // é opcional: só para estender (prorrogação). As parcelas são escolhidas em todo
    // aditivo e dividem o que falta pagar (paidSoFar = o pago da cadeia de versões, que o
    // service busca no billing). firstDueDate é opcional: sem ele, o sugerido. O máximo de
    // parcelas são os meses do 1º vencimento ao novo fim (o fim é calculado antes).
    public ContractEntity amend(PatientEntity beneficiary, PaymentMethod paymentMethod, int installments,
            LocalDate requestedEndDate, List<ContractItemDraft> drafts, LocalDate firstDueDate, Money paidSoFar,
            LocalDate amendmentDate) {
        requireAmendable();
        ContractEntity newVersion = new ContractEntity(this, this.patient, beneficiary,
                this.plan, this.startDate, paymentMethod, amendmentDate);
        newVersion.addItems(drafts, this);
        newVersion.endDate = calculateAmendedEndDate(drafts, requestedEndDate, amendmentDate);
        newVersion.firstDueDate = firstDueDate != null ? firstDueDate : suggestedFirstDueDate(paidSoFar, amendmentDate);
        validateFirstDueDateOnAmendment(newVersion.firstDueDate, amendmentDate);
        newVersion.installmentBase = amendedInstallmentBase(newVersion.getTotalValue(), paidSoFar);
        newVersion.installments = installmentsFor(newVersion.installmentBase, installments,
                maxInstallmentsBetween(newVersion.firstDueDate, newVersion.endDate));
        newVersion.requireChangedFrom(this, requestedEndDate, amendmentDate);
        this.active = false;
        this.deactivatedOn = amendmentDate;
        return newVersion;
    }

    // Soft delete da versão. Uma versão já inativa (substituída por aditivo ou já
    // encerrada) não pode ser inativada de novo: mesmo 409 do requireAmendable.
    // today = hoje, passado pelo service: depois dele, nada vence
    public void deactivate(LocalDate today) {
        requireActive();
        this.active = false;
        this.deactivatedOn = today;
    }

    // ---------------------------------------------------------------------------
    // Parcelas e vencimentos
    // ---------------------------------------------------------------------------

    // Valor parcelado do aditivo: o total da nova versão − o pago, no mínimo 0 (o que passar do
    // total é saldo credor da conta). Público e estático para a prévia do aditivo
    public static Money amendedInstallmentBase(Money total, Money paidSoFar) {
        return Money.of(total.amount().subtract(paidSoFar.amount()).max(BigDecimal.ZERO));
    }

    // Máximo de parcelas do aditivo: os meses do 1º vencimento ao fim (Installments.maxForPeriod).
    // Público e estático para a prévia do aditivo
    public static int maxInstallmentsBetween(LocalDate firstDueDate, LocalDate endDate) {
        return Installments.maxForPeriod(firstDueDate, endDate);
    }

    // O pago que vale para as parcelas desta versão: o pago da cadeia − o pago antes desta versão
    // (o total − o valor parcelado), entre 0 e o valor parcelado
    public Money paidTowardInstallments(Money chainPaid) {
        BigDecimal paidBefore = getTotalValue().amount().subtract(installmentBase.amount());
        BigDecimal toward = chainPaid.amount().subtract(paidBefore);
        return Money.of(toward.max(BigDecimal.ZERO).min(installmentBase.amount()));
    }

    // O 1º vencimento sugerido para um aditivo nesta data: o dia de vencimento desta versão no mês
    // da data; se a parcela deste mês já está paga, ou se o dia já passou, o mesmo dia no mês
    // seguinte. Público para a prévia do aditivo
    public LocalDate suggestedFirstDueDate(Money chainPaid, LocalDate date) {
        int dueDay = firstDueDate.getDayOfMonth();
        YearMonth month = YearMonth.from(date);
        LocalDate candidate = dayIn(month, dueDay);
        if (isInstallmentPaidIn(month, chainPaid) || candidate.isBefore(date)) {
            return dayIn(month.plusMonths(1), dueDay);
        }
        return candidate;
    }

    // Quantas parcelas desta versão vencem na data ou depois dela
    public int remainingInstallmentsOn(LocalDate date) {
        return (int) getInstallmentSchedule().stream()
                .filter(installment -> !installment.dueDate().isBefore(date))
                .count();
    }

    private boolean isInstallmentPaidIn(YearMonth month, Money chainPaid) {
        List<Installments.Scheduled> schedule = getInstallmentSchedule();
        List<Money> paidPerInstallment = getInstallmentValues().distribute(paidTowardInstallments(chainPaid));
        for (int index = 0; index < schedule.size(); index++) {
            Installments.Scheduled installment = schedule.get(index);
            if (YearMonth.from(installment.dueDate()).equals(month)) {
                return paidPerInstallment.get(index).amount().compareTo(installment.value().amount()) >= 0;
            }
        }
        return false;
    }

    // O dia no mês; no mês curto, o último dia (o dia 31 em fevereiro cai no dia 28 ou 29)
    private static LocalDate dayIn(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    // Com o valor parcelado zero (o pago cobre o total), 1 parcela de R$ 0,00, qualquer que seja o
    // pedido; senão, as pedidas, de 1 ao máximo
    private static int installmentsFor(Money installmentBase, int requested, int maxInstallments) {
        if (!installmentBase.isPositive()) {
            return 1;
        }
        validateInstallments(requested, maxInstallments);
        return requested;
    }

    // Vigente na data: ativo e com o fim a partir dela (endDate inclusivo: o último dia ainda
    // conta). Mesma regra das consultas de vigente do ContractRepository. Ativo com o fim antes
    // da data = vencido, aguardando a decisão do administrador.
    public boolean isCurrentOn(LocalDate date) {
        return active && !endDate.isBefore(date);
    }

    // O status na data. balance: o saldo da conta (pago − devido), do billing; só importa no
    // contrato ativo que passou do fim (nulo nos outros): zerado = encerrado; senão, vencido
    public ContractStatus statusOn(LocalDate date, boolean superseded, BigDecimal balance) {
        if (superseded) {
            return ContractStatus.SUPERSEDED;
        }
        if (!active) {
            return ContractStatus.CANCELED;
        }
        if (isCurrentOn(date)) {
            return ContractStatus.CURRENT;
        }
        return balance != null && balance.signum() == 0 ? ContractStatus.CLOSED : ContractStatus.EXPIRED;
    }

    // O crédito de contratos anteriores aplicado no cadastro: o que o total tem a mais que o valor
    // parcelado. Na versão original, é o crédito (no aditivo, essa diferença é o pago até então)
    public Money getCreditApplied() {
        return Money.of(getTotalValue().amount().subtract(installmentBase.amount()));
    }

    // Valor do contrato: a soma dos itens (sessões × valor da linha travada), sem desconto
    public Money getTotalValue() {
        return ContractItemValue.sumOf(items.stream().map(ContractItemEntity::getValue).toList());
    }

    // Máximo de parcelas: os meses do contrato, pelas semanas da vigência (Installments)
    public int getMaxInstallments() {
        return Installments.maxFor(getDurationInWeeks());
    }

    // O valor parcelado dividido nas parcelas combinadas, com os centavos que sobram na primeira
    public Installments getInstallmentValues() {
        return Installments.split(installmentBase, installments);
    }

    // Vencimento e valor de cada parcela (a partir do 1º vencimento)
    public List<Installments.Scheduled> getInstallmentSchedule() {
        return getInstallmentValues().scheduleFrom(firstDueDate);
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

    // Esta versão e todas as anteriores (o acordo inteiro até aqui), da mais nova para a
    // mais antiga: o histórico do detalhe. Usa os getters, não os campos: a versão anterior
    // é LAZY e pode vir como proxy do Hibernate, cujos campos ficam vazios.
    public List<ContractEntity> getVersionChain() {
        List<ContractEntity> chain = new ArrayList<>();
        for (ContractEntity version = this; version != null; version = version.getPreviousContract()) {
            chain.add(version);
        }
        return chain;
    }

    // Ids da cadeia de versões. O saldo de sessões soma os atendimentos da cadeia toda,
    // porque cada aditivo é uma versão nova.
    public List<Long> getVersionChainIds() {
        return getVersionChain().stream().map(ContractEntity::getId).toList();
    }

    // Vigência em semanas (arredondada para cima) — usada nas opções do plano do Grupo.
    // long: é o tipo que o ChronoUnit devolve; convertido para int, o tipo das semanas
    // no resto do código.
    public int getDurationInWeeks() {
        return durationInWeeksBetween(startDate, endDate);
    }

    // Estático para a prévia do aditivo, que tem o novo fim sem ter a nova versão
    public static int durationInWeeksBetween(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (int) ((days + 6) / 7);
    }

    // Privado: toda criação passa por create/amend. "previous" é a versão anterior
    // (null na criação): o que for igual a ela é aceito como está.
    private ContractEntity(ContractEntity previous, PatientEntity patient, PatientEntity beneficiary,
            PlanEntity plan, LocalDate startDate, PaymentMethod paymentMethod, LocalDate createdAt) {
        validateParties(previous, patient, beneficiary);
        validateBeneficiaryIsNotHolder(patient, beneficiary);
        validatePlan(previous, plan);
        this.previousContract = previous;
        this.amendmentNumber = previous != null ? previous.getAmendmentNumber() + 1 : 0;
        this.createdAt = createdAt;
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
        validateItemsMatchPlan(drafts, previous);
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
    // não entra — acompanha o contrato). Plano de mensalidade: o prazo do plano (termWeeks)
    // ou, se algum item por duração passar dele, o desse item; as sessões do item são as do
    // pacote equivalente (planFor). Público e estático para a prévia do formulário
    // (POST /api/contracts/preview) usar exatamente a mesma conta.
    public static int durationWeeksFor(PlanEntity plan, List<SessionFrequency> durationItemFrequencies) {
        validateContractable(plan);
        List<SessionFrequency> frequencies = durationItemFrequencies != null ? durationItemFrequencies : List.of();
        boolean isMonthlyFee = plan.getType() == PlanType.MONTHLY_FEE;
        if (!isMonthlyFee && frequencies.isEmpty()) {
            throw new BusinessRuleException(
                    "Contrato de pacote precisa de pelo menos um item por duração. Contrato só com Pilates em Grupo usa um plano de mensalidade.");
        }
        int planWeeks = isMonthlyFee ? plan.getTermWeeks() : 0;
        if (frequencies.isEmpty()) {
            return planWeeks;
        }
        PlanEntity durationPlan = plan.planFor(PricingModel.DURATION_BASED);
        frequencies.forEach(frequency -> ContractItemEntity.validateFitsMaxTerm(frequency, durationPlan));
        int itemsWeeks = frequencies.stream()
                .mapToInt(frequency -> frequency.weeksToComplete(durationPlan.getSessionCount()))
                .max()
                .getAsInt();
        return Math.max(planWeeks, itemsWeeks);
    }

    // endDate inclusivo: início + prazo − 1 dia (ex: 03/11 + 12 semanas → 25/01)
    public static LocalDate endDateFor(LocalDate startDate, int weeks) {
        return startDate.plusWeeks(weeks).minusDays(1);
    }

    // Aditivo: mantém o fim atual (desta versão). Item por duração incluído (grupo novo)
    // começa a valer na data do aditivo e pode estender o fim; o fim pedido (prorrogação)
    // também. O contrato nunca encolhe. Mudar a frequência não recalcula. Público e pelos
    // rascunhos para a prévia do aditivo (POST /api/contracts/{id}/amendments/preview)
    // usar exatamente a mesma conta, sem gerar a nova versão.
    public LocalDate calculateAmendedEndDate(List<ContractItemDraft> drafts, LocalDate requestedEndDate,
            LocalDate amendmentDate) {
        LocalDate end = endDate;
        for (ContractItemDraft draft : drafts) {
            boolean newDurationItem = draft.isDurationBased() && findItemOf(draft.pricingGroup()).isEmpty();
            if (newDurationItem) {
                ContractItemEntity.validateFitsMaxTerm(draft.frequency(), draft.durationPrice().getPlan());
                LocalDate itemEnd = endDateFor(amendmentDate, draft.weeksToComplete());
                end = itemEnd.isAfter(end) ? itemEnd : end;
            }
        }
        if (requestedEndDate != null) {
            validateRequestedEndDate(requestedEndDate, endDate);
            end = requestedEndDate.isAfter(end) ? requestedEndDate : end;
        }
        return end;
    }

    // ---------------------------------------------------------------------------
    // Validações
    // ---------------------------------------------------------------------------

    // Só a versão ativa pode ser alterada: uma versão inativa já foi substituída
    // ou encerrada. Contrato ativo porém vencido pode (é assim que se prorroga).
    // Público para a prévia do aditivo seguir a mesma regra.
    public void requireAmendable() {
        if (!active) {
            throw new ConflictException("Só a versão ativa do contrato pode ser alterada.");
        }
    }

    // Aditivo igual à versão anterior só criaria um "Aditivo N" vazio no histórico. Conta como
    // alteração: prorrogação pedida, outro beneficiário, outra forma de pagamento, outras
    // parcelas (outro dia de vencimento, ou um número diferente das parcelas que ainda faltam
    // vencer na anterior), item incluído ou removido, ou outra técnica, linha de preço ou
    // frequência num item mantido. As parcelas não comparam as datas: todo aditivo tem um 1º
    // vencimento novo, a partir da data dele.
    private void requireChangedFrom(ContractEntity previous, LocalDate requestedEndDate, LocalDate amendmentDate) {
        if (requestedEndDate == null && hasSameTermsAs(previous, amendmentDate)) {
            throw new BusinessRuleException("Nenhuma alteração no contrato.");
        }
    }

    private boolean hasSameTermsAs(ContractEntity previous, LocalDate amendmentDate) {
        return Objects.equals(idOf(beneficiary), idOf(previous.getBeneficiary()))
                && paymentMethod == previous.getPaymentMethod()
                && firstDueDate.getDayOfMonth() == previous.getFirstDueDate().getDayOfMonth()
                && installments == previous.remainingInstallmentsOn(amendmentDate)
                && hasSameItemsAs(previous);
    }

    // Um item por grupo: mesma quantidade e cada item igual ao do mesmo grupo na anterior
    private boolean hasSameItemsAs(ContractEntity previous) {
        return items.size() == previous.getItems().size()
                && items.stream().allMatch(item -> previous.findItemOf(item.getPricingGroup())
                        .map(item::hasSameTermsAs)
                        .orElse(false));
    }

    // Compara ids: relação LAZY pode vir como proxy do Hibernate
    private static Long idOf(PatientEntity patient) {
        return patient != null ? patient.getId() : null;
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

    // Parcelas de 1 até o máximo (na criação, os meses do contrato; no aditivo, os que faltam)
    private static void validateInstallments(int installments, int maxInstallments) {
        Installments.validateCount(installments, maxInstallments);
    }

    // No cadastro, o 1º vencimento cai no primeiro mês: do início até 1 mês depois. Público para a
    // prévia do cadastro seguir a mesma regra
    public static void validateFirstDueDateOnCreation(LocalDate firstDueDate, LocalDate startDate) {
        if (firstDueDate.isBefore(startDate) || firstDueDate.isAfter(startDate.plusMonths(1))) {
            throw new BusinessRuleException("O 1º vencimento deve ser entre o início do contrato e um mês depois.");
        }
    }

    // No aditivo, nenhuma parcela nasce vencida: o 1º vencimento é a partir da data do aditivo.
    // Público para a prévia do aditivo seguir a mesma regra
    public static void validateFirstDueDateOnAmendment(LocalDate firstDueDate, LocalDate amendmentDate) {
        if (firstDueDate.isBefore(amendmentDate)) {
            throw new BusinessRuleException("No aditivo, o 1º vencimento deve ser a partir da data do aditivo.");
        }
    }

    // O beneficiário é outra pessoa (o acompanhante da dupla, o familiar): o titular não pode
    // ser beneficiário do próprio contrato. Vale na criação e no aditivo. Compara ids: relação
    // LAZY pode vir como proxy do Hibernate.
    private static void validateBeneficiaryIsNotHolder(PatientEntity patient, PatientEntity beneficiary) {
        if (beneficiary != null && beneficiary.getId().equals(patient.getId())) {
            throw new BusinessRuleException("O beneficiário deve ser outro paciente.");
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
    //   item é por duração (o contrato só com o Grupo tem plano de mensalidade);
    // - plano de mensalidade: o item do Grupo usa linha DESTE plano e é obrigatório; itens
    //   por duração usam linhas do pacote EQUIVALENTE (de-para, planFor), o caminho inverso
    //   do item do Grupo no pacote.
    // O plano do item do Grupo num contrato de pacote é validado no service (depende
    // do catálogo de planos). No aditivo, o item com a mesma linha da versão anterior é
    // aceito como está (o de-para pode ter mudado depois).
    private void validateItemsMatchPlan(List<ContractItemDraft> drafts, ContractEntity previous) {
        if (plan.getType() == PlanType.PACKAGE && drafts.stream().noneMatch(ContractItemDraft::isDurationBased)) {
            throw new BusinessRuleException(
                    "Contrato de pacote precisa de pelo menos um item por duração. Contrato só com Pilates em Grupo usa um plano de mensalidade.");
        }
        if (plan.getType() == PlanType.MONTHLY_FEE && drafts.stream().allMatch(ContractItemDraft::isDurationBased)) {
            throw new BusinessRuleException(
                    "Contrato com plano de mensalidade precisa do item do Pilates em Grupo. Para ficar só com itens por duração, faça um contrato novo com um plano de pacote.");
        }
        for (ContractItemDraft draft : drafts) {
            if (!hasSameLineAsPrevious(draft, previous)) {
                validateItemMatchesPlan(draft);
            }
        }
    }

    // Item por duração: linha do plano do contrato (pacote) ou do equivalente dele (mensalidade).
    // Item do Grupo num contrato de mensalidade: linha do próprio plano do contrato
    private void validateItemMatchesPlan(ContractItemDraft draft) {
        PlanEntity requiredPlan;
        if (draft.isDurationBased()) {
            requiredPlan = plan.planFor(PricingModel.DURATION_BASED);
        } else if (plan.getType() == PlanType.MONTHLY_FEE) {
            requiredPlan = plan;
        } else {
            return;
        }
        Long linePlanId = draft.isDurationBased()
                ? draft.durationPrice().getPlan().getId()
                : draft.frequencyPrice().getPlan().getId();
        if (!linePlanId.equals(requiredPlan.getId())) {
            throw new BusinessRuleException(requiredPlan.getId().equals(plan.getId())
                    ? "O preço do item precisa ser do plano do contrato (" + plan.getName() + ")."
                    : "O preço do item precisa ser do plano " + requiredPlan.getName() + " (equivalente do "
                            + plan.getName() + ").");
        }
    }

    // A mesma linha travada no item do mesmo grupo da versão anterior. Compara ids: relação
    // LAZY pode vir como proxy do Hibernate
    private static boolean hasSameLineAsPrevious(ContractItemDraft draft, ContractEntity previous) {
        if (previous == null) {
            return false;
        }
        return previous.findItemOf(draft.pricingGroup())
                .map(previousItem -> draft.isDurationBased()
                        ? previousItem.getSessionDurationPrice() != null
                                && previousItem.getSessionDurationPrice().getId().equals(draft.durationPrice().getId())
                        : previousItem.getSessionFrequencyPrice() != null
                                && previousItem.getSessionFrequencyPrice().getId().equals(draft.frequencyPrice().getId()))
                .orElse(false);
    }

    // Prorrogação: o fim pedido precisa ser depois do fim atual (nunca encolhe)
    private static void validateRequestedEndDate(LocalDate requestedEndDate, LocalDate currentEndDate) {
        if (!requestedEndDate.isAfter(currentEndDate)) {
            throw new BusinessRuleException("A nova data de término precisa ser posterior ao fim atual do contrato.");
        }
    }
}
