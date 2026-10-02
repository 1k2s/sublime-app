package br.com.senai.sublime_app.pricing.domain;

import java.math.BigDecimal;
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

// Regra de unicidade: só uma linha vigente (validTo = null) por (pricingGroup, durationMinutes, plan).
// MySQL não suporta unique index parcial (com WHERE), então a garantia no banco virá na
// migration via coluna gerada: current_flag = IF(valid_to IS NULL, 1, NULL) + UNIQUE
// (pricing_group_id, plan_id, duration_minutes, current_flag) — linhas históricas têm
// current_flag NULL e ficam fora da regra. Até lá, garantir no service, na hora do reajuste.
@Entity
@Table(name = "session_duration_price")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SessionDurationPriceEntity {

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

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "session_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal sessionValue;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    // Exclusivo: a linha vale em [validFrom, validTo). Ver domain-model.md.
    @Column(name = "valid_to")
    private LocalDate validTo;

    // Construtor publico para criar uma linha de preço vigente a partir de validFrom.
    // Achar e fechar a linha vigente da mesma combinação (reajuste) é orquestração do service.
    public SessionDurationPriceEntity(PricingGroupEntity pricingGroup, PlanEntity plan,
            int durationMinutes, BigDecimal sessionValue, LocalDate validFrom) {
        validatePricingGroupAndPlan(pricingGroup, plan);
        validateDurationAndSessionValue(durationMinutes, sessionValue);
        this.pricingGroup = pricingGroup;
        this.plan = plan;
        this.durationMinutes = durationMinutes;
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

    // A linha precisa estar na tabela do pricingModel do grupo, e o plano precisa
    // ter sessionCount: o saldo de sessões dos grupos por duração parte dele.
    private static void validatePricingGroupAndPlan(PricingGroupEntity pricingGroup, PlanEntity plan) {
        if (pricingGroup.getPricingModel() != PricingModel.DURATION_BASED) {
            throw new BusinessRuleException("Duration-based prices are only allowed for DURATION_BASED pricing groups.");
        }
        if (!plan.isActive()) {
            throw new BusinessRuleException("Plan is inactive.");
        }
        if (plan.getSessionCount() == null) {
            throw new BusinessRuleException("Duration-based prices require a plan with sessionCount.");
        }
    }

    private static void validateDurationAndSessionValue(int durationMinutes, BigDecimal sessionValue) {
        if (durationMinutes < 1) {
            throw new BusinessRuleException("durationMinutes must be at least 1.");
        }
        if (sessionValue == null || sessionValue.compareTo(BigDecimal.ZERO) <= 0) {
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
