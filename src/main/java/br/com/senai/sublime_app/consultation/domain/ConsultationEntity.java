package br.com.senai.sublime_app.consultation.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.provider.domain.Percentage;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Lançamento de um atendimento cobrado. Nunca é editado: para corrigir, inativa e
// lança de novo. Os valores financeiros são snapshot do momento do lançamento (nunca
// mudam se o catálogo ou a comissão mudarem depois). O service resolve QUAL linha de
// preço usar (depende do banco); a entidade valida a coerência e calcula o resto.
// Ver domain-model.md, seção Consultation.
@Entity
@Table(name = "consultation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsultationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Quem compareceu: o titular ou o beneficiário do contrato, ou um paciente sem contrato
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    // Contrato do atendimento: debita o item (caso 1) ou dá o plano do de-para (caso 2).
    // Nulo só no Avulso (caso 3). Referencia a raiz do agregado, nunca o item.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    // Técnica clinicamente executada (a linha de preço só aponta para o grupo)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technique_id", nullable = false)
    private TechniqueEntity technique;

    // Obrigatório em todos os grupos; no Pilates em Grupo só registra a duração da aula
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    // Data do atendimento: referência da vigência do contrato, não a data do lançamento
    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsultationStatus status;

    // Caso do preço (1, 2 ou 3), calculado aqui; só CONTRACT_ITEM debita o saldo do item
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_source", nullable = false, length = 30)
    private PricingSource pricingSource;

    // Exclusive arc: a linha de preço usada (por duração OU por frequência)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_duration_price_id")
    private SessionDurationPriceEntity sessionDurationPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_frequency_price_id")
    private SessionFrequencyPriceEntity sessionFrequencyPrice;

    // Snapshots (convertidos para DECIMAL pelo PercentageConverter/MoneyConverter)
    @Column(name = "commission_percentage_applied", nullable = false, precision = 5, scale = 2)
    private Percentage commissionPercentageApplied;

    @Column(name = "base_value", nullable = false, precision = 10, scale = 2)
    private Money baseValue;

    @Column(name = "payout_value", nullable = false, precision = 10, scale = 2)
    private Money payoutValue;

    // Soft delete: atendimento inativo não conta no saldo nem no repasse
    @Column(nullable = false)
    private boolean active = true;

    // Construtor público de negócio: a linha de preço já vem resolvida pelo service
    // (casos 1 a 3); o resto (caso do preço, valor, comissão e repasse) é calculado aqui.
    public ConsultationEntity(PatientEntity patient, ContractEntity contract, ProviderEntity provider,
            TechniqueEntity technique, int durationMinutes, LocalDate occurredAt, ConsultationStatus status,
            SessionDurationPriceEntity durationPrice, SessionFrequencyPriceEntity frequencyPrice) {
        validateActiveParticipants(patient, provider, technique);
        validateDurationAndStatus(durationMinutes, status);
        validateOccurredAt(occurredAt);
        validatePrice(technique, durationMinutes, durationPrice, frequencyPrice);
        validateContractCovers(contract, patient, occurredAt);
        this.patient = patient;
        this.contract = contract;
        this.provider = provider;
        this.technique = technique;
        this.durationMinutes = durationMinutes;
        this.occurredAt = occurredAt;
        this.status = status;
        this.pricingSource = pricingSourceFor(contract, technique.getPricingGroup());
        this.sessionDurationPrice = durationPrice;
        this.sessionFrequencyPrice = frequencyPrice;
        this.baseValue = sessionValueOf(durationPrice, frequencyPrice);
        this.commissionPercentageApplied = provider.commissionPercentageFor(originOf(patient, contract).getReferringProvider());
        this.payoutValue = payoutFor(baseValue, commissionPercentageApplied);
        this.active = true;
    }

    // Soft delete. O lançamento nunca é editado: para corrigir, inativa e lança de novo.
    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // ---------------------------------------------------------------------------
    // Cálculos
    // ---------------------------------------------------------------------------

    // Caso do preço: sem contrato → Avulso; contrato com item do grupo da técnica →
    // item do contrato; senão → de-para. Público e estático para o modal dos contratos
    // vinculados (GET /api/consultations/eligible-contracts) usar exatamente a mesma conta.
    public static PricingSource pricingSourceFor(ContractEntity contract, PricingGroupEntity pricingGroup) {
        if (contract == null) {
            return PricingSource.SINGLE_SESSION;
        }
        return contract.findItemOf(pricingGroup).isPresent()
                ? PricingSource.CONTRACT_ITEM
                : PricingSource.EQUIVALENT_PLAN;
    }

    // Valor da sessão: cópia do valor da linha usada
    private static Money sessionValueOf(SessionDurationPriceEntity durationPrice,
            SessionFrequencyPriceEntity frequencyPrice) {
        return durationPrice != null ? durationPrice.getSessionValue() : frequencyPrice.getSessionValue();
    }

    // Origem da comissão: o titular do contrato (casos 1 e 2, mesmo quando quem
    // compareceu foi o beneficiário); sem contrato, o próprio paciente atendido
    private static PatientEntity originOf(PatientEntity patient, ContractEntity contract) {
        return contract != null ? contract.getPatient() : patient;
    }

    // Repasse = valor × percentual, arredondado com HALF_UP pelo próprio Percentage
    private static Money payoutFor(Money baseValue, Percentage percentage) {
        return Money.of(percentage.applyTo(baseValue.amount()));
    }

    // ---------------------------------------------------------------------------
    // Validações
    // ---------------------------------------------------------------------------

    private void requireActive() {
        if (!active) {
            throw new ConflictException("O atendimento já está inativo.");
        }
    }

    // Registro inativo não entra em cadastro novo (se o paciente estiver inativo, o
    // admin o reativa antes de lançar)
    private static void validateActiveParticipants(PatientEntity patient, ProviderEntity provider,
            TechniqueEntity technique) {
        if (patient == null || provider == null || technique == null) {
            throw new BusinessRuleException("Paciente, prestador e técnica são obrigatórios.");
        }
        if (!patient.isActive()) {
            throw new BusinessRuleException("O paciente está inativo.");
        }
        if (!provider.isActive()) {
            throw new BusinessRuleException("O prestador está inativo.");
        }
        if (!technique.isActive()) {
            throw new BusinessRuleException("A técnica está inativa.");
        }
    }

    private static void validateDurationAndStatus(int durationMinutes, ConsultationStatus status) {
        if (durationMinutes < 1) {
            throw new BusinessRuleException("A duração do atendimento deve ser de no mínimo 1 minuto.");
        }
        if (status == null) {
            throw new BusinessRuleException("O status do atendimento é obrigatório.");
        }
    }

    private static void validateOccurredAt(LocalDate occurredAt) {
        if (occurredAt == null || occurredAt.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data do atendimento é obrigatória e não pode ser futura.");
        }
    }

    // Exatamente uma linha (exclusive arc), do grupo da técnica atendida. Na linha por
    // duração, a duração é a do atendimento (a travada só vale na mesma duração; nas
    // outras, entra a linha atual na duração do atendimento). Compara ids: relação
    // LAZY pode vir como proxy do Hibernate.
    private static void validatePrice(TechniqueEntity technique, int durationMinutes,
            SessionDurationPriceEntity durationPrice, SessionFrequencyPriceEntity frequencyPrice) {
        if ((durationPrice == null) == (frequencyPrice == null)) {
            throw new BusinessRuleException("O atendimento precisa de exatamente um preço: por duração ou por frequência.");
        }
        PricingGroupEntity lineGroup = durationPrice != null ? durationPrice.getPricingGroup() : frequencyPrice.getPricingGroup();
        if (!lineGroup.getId().equals(technique.getPricingGroup().getId())) {
            throw new BusinessRuleException("O preço não pertence ao grupo de preço da técnica atendida.");
        }
        if (durationPrice != null && durationPrice.getDurationMinutes() != durationMinutes) {
            throw new BusinessRuleException("A duração do preço não corresponde à duração do atendimento.");
        }
    }

    // O contrato informado precisa estar ativo, cobrir a data do atendimento e ter o
    // paciente como titular ou beneficiário. Se ele cobre ou não o grupo da técnica
    // define o caso do preço (pricingSource), não a validade.
    private static void validateContractCovers(ContractEntity contract, PatientEntity patient, LocalDate occurredAt) {
        if (contract == null) {
            return;
        }
        if (!contract.isActive()) {
            throw new BusinessRuleException("O contrato informado está inativo (substituído por aditivo ou encerrado).");
        }
        if (occurredAt.isBefore(contract.getStartDate()) || occurredAt.isAfter(contract.getEndDate())) {
            throw new BusinessRuleException("O contrato informado não cobre a data do atendimento.");
        }
        if (!isHolderOrBeneficiary(contract, patient)) {
            throw new BusinessRuleException("O paciente não é titular nem beneficiário do contrato informado.");
        }
    }

    private static boolean isHolderOrBeneficiary(ContractEntity contract, PatientEntity patient) {
        boolean isHolder = contract.getPatient().getId().equals(patient.getId());
        boolean isBeneficiary = contract.getBeneficiary() != null
                && contract.getBeneficiary().getId().equals(patient.getId());
        return isHolder || isBeneficiary;
    }
}
