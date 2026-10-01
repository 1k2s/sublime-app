package br.com.senai.sublime_app.pricing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "valid_to")
    private LocalDate validTo;

    // Linha vigente = ainda não encerrada por um reajuste (validTo = null)
    public boolean isCurrent() {
        return validTo == null;
    }
}
