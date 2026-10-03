package br.com.senai.sublime_app.pricing.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Regra de unicidade: só uma linha vigente (validTo = null) por (pricingGroup, weeklyFrequency, plan).
// MySQL não suporta unique index parcial (com WHERE), então a garantia no banco virá na
// migration via coluna gerada: current_flag = IF(valid_to IS NULL, 1, NULL) + UNIQUE
// (pricing_group_id, plan_id, weekly_frequency, current_flag) — linhas históricas têm
// current_flag NULL e ficam fora da regra. Até lá, garantir no service, na hora do reajuste.
@Entity
@Table(name = "session_frequency_price")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SessionFrequencyPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pricing_group_id", nullable = false)
    private PricingGroupEntity pricingGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "weekly_frequency", nullable = false)
    private int weeklyFrequency;

    // Convertido para DECIMAL pelo MoneyConverter (autoApply)
    @Column(name = "session_value", nullable = false, precision = 10, scale = 2)
    private Money sessionValue;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    // Exclusivo: a linha vale em [validFrom, validTo). Ver domain-model.md.
    @Column(name = "valid_to")
    private LocalDate validTo;

    // Construtor publico para criar uma linha de preço vigente a partir de validFrom.
    // Achar e fechar a linha vigente da mesma combinação (reajuste) é orquestração do service.
    public SessionFrequencyPriceEntity(PricingGroupEntity pricingGroup, PlanEntity plan,
            int weeklyFrequency, Money sessionValue, LocalDate validFrom) {
        validatePricingGroupAndPlan(pricingGroup, plan);
        validateFrequencyAndSessionValue(weeklyFrequency, sessionValue);
        this.pricingGroup = pricingGroup;
        this.plan = plan;
        this.weeklyFrequency = weeklyFrequency;
        this.sessionValue = sessionValue;
        this.validFrom = validFrom;
    }

    // Linha vigente = ainda não encerrada por um reajuste (validTo = null)
    public boolean isCurrent() {
        return validTo == null;
    }

    // Encerra a linha num reajuste. validTo é exclusivo, então a linha nova abre na mesma data.
    public void close(LocalDate validTo) {
        requireCurrent();
        validateValidTo(validTo);
        this.validTo = validTo;
    }

    // A linha precisa estar na tabela do pricingModel do grupo. O plano não precisa
    // de sessionCount: no Grupo o plano é mensalidade, não pacote de sessões.
    private static void validatePricingGroupAndPlan(PricingGroupEntity pricingGroup, PlanEntity plan) {
        if (pricingGroup.getPricingModel() != PricingModel.FREQUENCY_BASED) {
            throw new BusinessRuleException("Frequency-based prices are only allowed for FREQUENCY_BASED pricing groups.");
        }
        if (!plan.isActive()) {
            throw new BusinessRuleException("Plan is inactive.");
        }
    }

    private static void validateFrequencyAndSessionValue(int weeklyFrequency, Money sessionValue) {
        if (weeklyFrequency < 1) {
            throw new BusinessRuleException("weeklyFrequency must be at least 1.");
        }
        if (sessionValue == null || !sessionValue.isPositive()) {
            throw new BusinessRuleException("sessionValue must be greater than zero.");
        }
    }

    private void requireCurrent() {
        if (!isCurrent()) {
            throw new BusinessRuleException("Price is already closed.");
        }
    }

    // validTo igual a validFrom é aceito: reajuste no mesmo dia gera um intervalo
    // vazio (linha substituída antes de valer um dia inteiro).
    private void validateValidTo(LocalDate validTo) {
        if (validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("validTo must not be before validFrom.");
        }
    }
}
