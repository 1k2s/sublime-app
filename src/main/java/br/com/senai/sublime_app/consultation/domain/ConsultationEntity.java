package br.com.senai.sublime_app.consultation.domain;


import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.GroupPlanFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.GroupPlanPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consultation")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsultationEntity {
   

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
 
    // N:1 (lazy para não carregar o grafo inteiro), optional false para não permitir nulo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;
 
    // @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn(name = "contract_id", nullable = false)
    // private ContractEntity contract;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technique_id", nullable = false)
    private TechniqueEntity technique;
 
    @NotNull
    @Positive
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
 
    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsultationStatus status;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_plan_price_id")
    private GroupPlanPriceEntity groupPlanPrice;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_plan_frequency_price_id")
    private GroupPlanFrequencyPriceEntity groupPlanFrequencyPrice;

    @Column(name = "commission_percentage_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentageApplied;
 
    @Column(name = "base_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseValue;
    
    // // Construtor publico para criar uma consulta com os dados obrigatórios
    // public ConsultationEntity(
    //         PatientEntity patient,
    //         ContractEntity contract,
    //         ProviderEntity provider,
    //         TechniqueEntity technique,
    //         Integer durationMinutes,
    //         LocalDate occurredAt,
    //         GroupPlanPriceEntity groupPlanPrice,
    //         GroupPlanFrequencyPriceEntity groupPlanFrequencyPrice,
    //         BigDecimal baseValue) {
    //     this.patient = patient;
    //     this.contract = contract;
    //     this.provider = provider;
    //     this.technique = technique;
    //     this.durationMinutes = durationMinutes;
    //     this.occurredAt = occurredAt;
    //     this.groupPlanPrice = groupPlanPrice;
    //     this.groupPlanFrequencyPrice = groupPlanFrequencyPrice;
    //     this.status = ConsultationStatus.SCHEDULED;
 
    //     // snapshot da comissão vigente do provider neste momento
    //     this.commissionPercentageApplied = provider.getCommissionPercentage();
    //     this.baseValue = baseValue;
    //     this.repasseValue = calculateRepasse();
    // }
 
    // // repasse = baseValue * (comissão / 100), arredondado em 2 casas
    // private BigDecimal calculateRepasse() {
    //     return baseValue
    //             .multiply(commissionPercentageApplied)
    //             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    // }
 
    // // update publico para reagendar/ajustar a consulta (não recalcula os snapshots)
    // public void update(Integer durationMinutes, LocalDate occurredAt, TechniqueEntity technique) {
    //     this.durationMinutes = durationMinutes;
    //     this.occurredAt = occurredAt;
    //     this.technique = technique;
    // }
 
    // public void complete() {
    //     requireScheduled();
    //     this.status = ConsultationStatus.COMPLETED;
    // }
 
    // public void cancel() {
    //     requireScheduled();
    //     this.status = ConsultationStatus.CANCELLED;
    // }
 
    // public void markNoShow() {
    //     requireScheduled();
    //     this.status = ConsultationStatus.NO_SHOW;
    // }
 
    // private void requireScheduled() {
    //     if (this.status != ConsultationStatus.SCHEDULED) {
    //         throw new IllegalStateException("Only scheduled consultations can change status. Current: " + status);
    //     }
    // }
}

