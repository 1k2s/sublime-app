package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.GroupPlanFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.GroupPlanPriceEntity;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "contracts")
@Getter
@Setter(AccessLevel.PROTECTED)
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
    @JoinColumn(name = "group_plan_price_id")
    private GroupPlanPriceEntity groupPlanPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_plan_frequency_price_id")
    private GroupPlanFrequencyPriceEntity groupPlanFrequencyPrice;

    @Column(nullable = false)
    private boolean active = true;

    public ContractEntity(PatientEntity patient, PatientEntity beneficiary, TechniqueEntity technique,
                          PlanEntity plan, Integer weeklyFrequency, LocalDate startDate, LocalDate endDate,
                          PaymentMethod paymentMethod, GroupPlanPriceEntity groupPlanPrice,
                          GroupPlanFrequencyPriceEntity groupPlanFrequencyPrice) {
        if ((groupPlanPrice != null && groupPlanFrequencyPrice != null) ||
            (groupPlanPrice == null && groupPlanFrequencyPrice == null)) {
            throw new IllegalArgumentException(
                "Exclusive arc violation: A contract must have either a groupPlanPrice or a groupPlanFrequencyPrice, not both or neither."
            );
        }
        this.patient = patient;
        this.beneficiary = beneficiary;
        this.technique = technique;
        this.plan = plan;
        this.weeklyFrequency = weeklyFrequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentMethod = paymentMethod;
        this.groupPlanPrice = groupPlanPrice;
        this.groupPlanFrequencyPrice = groupPlanFrequencyPrice;
        this.active = true;
    }

    public void update(Integer weeklyFrequency, LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        this.weeklyFrequency = weeklyFrequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentMethod = paymentMethod;
    }

    public void deactivate() {
        this.active = false;
    }
}