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
    public SessionDurationPriceEntity(PricingGroupEntity pricingGroup, PlanEntity plan,
            int durationMinutes, Money sessionValue, LocalDate validFrom) {
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
            throw new BusinessRuleException("Preço por duração só é permitido em grupos DURATION_BASED.");
        }
        if (!plan.isActive()) {
            throw new BusinessRuleException("O plano está inativo.");
        }
        if (plan.getSessionCount() == null) {
            throw new BusinessRuleException("Preço por duração exige um plano com quantidade de sessões (sessionCount).");
        }
    }

    private static void validateDurationAndSessionValue(int durationMinutes, Money sessionValue) {
        if (durationMinutes < 1) {
            throw new BusinessRuleException("A duração da sessão deve ser de no mínimo 1 minuto.");
        }
        if (sessionValue == null || !sessionValue.isPositive()) {
            throw new BusinessRuleException("O valor da sessão deve ser maior que zero.");
        }
    }

    private void requireCurrent() {
        if (!isCurrent()) {
            throw new BusinessRuleException("Esta linha de preço já foi encerrada.");
        }
    }

    // validTo igual a validFrom é aceito: reajuste no mesmo dia gera um intervalo
    // vazio (linha substituída antes de valer um dia inteiro).
    private void validateValidTo(LocalDate validTo) {
        if (validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("O fim da vigência não pode ser anterior ao início.");
        }
    }
}
