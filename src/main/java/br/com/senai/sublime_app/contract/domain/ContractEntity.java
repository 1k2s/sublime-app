package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private PatientEntity beneficiary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technique_id", nullable = false)
    private TechniqueEntity technique;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "weekly_frequency", nullable = false)
    private Integer weeklyFrequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_duration_price_id")
    private SessionDurationPriceEntity sessionDurationPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_frequency_price_id")
    private SessionFrequencyPriceEntity sessionFrequencyPrice;

    @Column(nullable = false)
    private boolean active = true;

    // Versão anterior deste contrato (aditivo). Null na primeira versão.
    // unique: cada versão tem no máximo uma sucessora — impede que duas alterações
    // simultâneas do mesmo contrato gerem duas versões "filhas".
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id", unique = true)
    private ContractEntity previousContract;

    // ---------------------------------------------------------------------------
    // Criação (primeira versão do contrato)
    // ---------------------------------------------------------------------------

    /**
     * Cria um contrato com preço por duração (grupos DURATION_BASED).
     * A tabela por duração não tem frequência: ela é negociação e vem de fora.
     */
    public static ContractEntity withDurationPrice(PatientEntity patient, PatientEntity beneficiary,
            TechniqueEntity technique, SessionDurationPriceEntity price, Integer weeklyFrequency,
            LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        return buildWithDurationPrice(null, patient, beneficiary, technique, price, weeklyFrequency,
                startDate, endDate, paymentMethod);
    }

    /**
     * Cria um contrato com preço por frequência (grupos FREQUENCY_BASED).
     * Não recebe weeklyFrequency: ela faz parte do preço e é copiada da linha,
     * tornando impossível um contrato com frequência diferente da do preço travado.
     */
    public static ContractEntity withFrequencyPrice(PatientEntity patient, PatientEntity beneficiary,
            TechniqueEntity technique, SessionFrequencyPriceEntity price,
            LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        return buildWithFrequencyPrice(null, patient, beneficiary, technique, price,
                startDate, endDate, paymentMethod);
    }

    // ---------------------------------------------------------------------------
    // Alteração (aditivo): o contrato nunca é editado no lugar. Cada mudança gera
    // uma nova versão, que aponta para esta (previousContract), e esta é inativada.
    // O titular é sempre copiado: trocar o titular é um contrato novo, não um aditivo.
    // ---------------------------------------------------------------------------

    /** Gera a próxima versão deste contrato com preço por duração. */
    public ContractEntity amendWithDurationPrice(PatientEntity beneficiary, TechniqueEntity technique,
            SessionDurationPriceEntity price, Integer weeklyFrequency,
            LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        requireAmendable();
        ContractEntity newVersion = buildWithDurationPrice(this, this.patient, beneficiary, technique, price,
                weeklyFrequency, startDate, endDate, paymentMethod);
        this.active = false;
        return newVersion;
    }

    /** Gera a próxima versão deste contrato com preço por frequência. */
    public ContractEntity amendWithFrequencyPrice(PatientEntity beneficiary, TechniqueEntity technique,
            SessionFrequencyPriceEntity price, LocalDate startDate, LocalDate endDate,
            PaymentMethod paymentMethod) {
        requireAmendable();
        ContractEntity newVersion = buildWithFrequencyPrice(this, this.patient, beneficiary, technique, price,
                startDate, endDate, paymentMethod);
        this.active = false;
        return newVersion;
    }

    // Só a versão ativa pode ser alterada: uma versão inativa já foi substituída
    // ou encerrada. Contrato ativo porém vencido pode (é assim que se prorroga).
    private void requireAmendable() {
        if (!active) {
            throw new ConflictException("Só a versão ativa do contrato pode ser alterada.");
        }
    }

    // ---------------------------------------------------------------------------
    // Montagem comum à criação e à alteração. "previous" é a versão anterior
    // (null na criação): o que for igual a ela é aceito como está, o que for novo
    // precisa ser válido — ex: manter a linha de preço travada é permitido mesmo
    // após um reajuste; trocar de linha exige uma linha vigente.
    // ---------------------------------------------------------------------------

    private static ContractEntity buildWithDurationPrice(ContractEntity previous, PatientEntity patient,
            PatientEntity beneficiary, TechniqueEntity technique, SessionDurationPriceEntity price,
            Integer weeklyFrequency, LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        Long previousPriceId = previous != null && previous.sessionDurationPrice != null
                ? previous.sessionDurationPrice.getId() : null;
        validatePrice(price.isCurrent() || price.getId().equals(previousPriceId),
                price.getPricingGroup(), technique, PricingModel.DURATION_BASED);
        validateWeeklyFrequency(weeklyFrequency);
        return new ContractEntity(previous, patient, beneficiary, technique, price.getPlan(), weeklyFrequency,
                startDate, endDate, paymentMethod, price, null);
    }

    private static ContractEntity buildWithFrequencyPrice(ContractEntity previous, PatientEntity patient,
            PatientEntity beneficiary, TechniqueEntity technique, SessionFrequencyPriceEntity price,
            LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        Long previousPriceId = previous != null && previous.sessionFrequencyPrice != null
                ? previous.sessionFrequencyPrice.getId() : null;
        validatePrice(price.isCurrent() || price.getId().equals(previousPriceId),
                price.getPricingGroup(), technique, PricingModel.FREQUENCY_BASED);
        return new ContractEntity(previous, patient, beneficiary, technique, price.getPlan(),
                price.getWeeklyFrequency(), startDate, endDate, paymentMethod, null, price);
    }

    // Privado: toda criação passa pelas fábricas/aditivos acima, que aplicam as
    // regras de preço. O exclusive arc é garantido por construção — cada caminho
    // passa um preço e null no outro. A linha de preço é a fonte da verdade do plano.
    private ContractEntity(ContractEntity previous, PatientEntity patient, PatientEntity beneficiary,
            TechniqueEntity technique, PlanEntity plan, Integer weeklyFrequency, LocalDate startDate,
            LocalDate endDate, PaymentMethod paymentMethod, SessionDurationPriceEntity sessionDurationPrice,
            SessionFrequencyPriceEntity sessionFrequencyPrice) {
        validateParties(previous, patient, beneficiary, technique);
        validatePeriod(startDate, endDate);
        this.previousContract = previous;
        this.patient = patient;
        this.beneficiary = beneficiary;
        this.technique = technique;
        this.plan = plan;
        this.weeklyFrequency = weeklyFrequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentMethod = paymentMethod;
        this.sessionDurationPrice = sessionDurationPrice;
        this.sessionFrequencyPrice = sessionFrequencyPrice;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * A linha de preço escolhida precisa ser coerente com a técnica-âncora:
     * aceita (vigente, ou a mesma já travada na versão anterior), do mesmo grupo
     * da técnica, e de uma tabela compatível com o pricingModel do grupo
     * (protege contra linha cadastrada na tabela errada).
     *
     * Compara ids em vez de entidades: relações LAZY podem vir como proxy do
     * Hibernate, e equals entre proxy e entidade real não é confiável.
     */
    private static void validatePrice(boolean priceAccepted, PricingGroupEntity priceGroup,
            TechniqueEntity technique, PricingModel expectedModel) {
        if (!priceAccepted) {
            throw new BusinessRuleException("O preço selecionado não está mais vigente. Use o preço atual.");
        }
        if (!priceGroup.getId().equals(technique.getPricingGroup().getId())) {
            throw new BusinessRuleException("A técnica não pertence ao grupo de preço do preço selecionado.");
        }
        if (priceGroup.getPricingModel() != expectedModel) {
            throw new BusinessRuleException(
                    "A tabela do preço selecionado não corresponde ao modelo de precificação do grupo da técnica.");
        }
    }

    // Registros inativos (soft delete) existem, mas não podem ENTRAR num contrato.
    // Na alteração, o que já estava na versão anterior é aceito como está: o titular
    // (sempre copiado), e beneficiário/técnica quando não foram trocados.
    private static void validateParties(ContractEntity previous, PatientEntity patient,
            PatientEntity beneficiary, TechniqueEntity technique) {
        if (previous == null && !patient.isActive()) {
            throw new BusinessRuleException("O paciente titular está inativo.");
        }
        Long previousBeneficiaryId = previous != null && previous.beneficiary != null
                ? previous.beneficiary.getId() : null;
        if (beneficiary != null && !beneficiary.isActive() && !beneficiary.getId().equals(previousBeneficiaryId)) {
            throw new BusinessRuleException("O beneficiário está inativo.");
        }
        Long previousTechniqueId = previous != null ? previous.technique.getId() : null;
        if (!technique.isActive() && !technique.getId().equals(previousTechniqueId)) {
            throw new BusinessRuleException("A técnica está inativa.");
        }
    }

    // No preço por duração a frequência é negociação e vem de fora (no preço por
    // frequência ela é copiada da linha de preço, então não passa por aqui).
    private static void validateWeeklyFrequency(Integer weeklyFrequency) {
        if (weeklyFrequency == null || weeklyFrequency < 1) {
            throw new BusinessRuleException("A frequência semanal é obrigatória para preço por duração e deve ser de no mínimo 1 sessão.");
        }
    }

    // Invariante da vigência: validada aqui (e não no service) para valer em
    // qualquer caminho que crie ou altere o contrato.
    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("A data de término não pode ser anterior à data de início.");
        }
    }
}