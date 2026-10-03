package br.com.senai.sublime_app.consultation.domain;


import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
 
    // N:1 (lazy para não carregar o grafo inteiro), optional false para não permitir nulo
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technique_id", nullable = false)
    private TechniqueEntity technique;
 
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
 
    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsultationStatus status;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_duration_price_id")
    private SessionDurationPriceEntity sessionDurationPrice;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_frequency_price_id")
    private SessionFrequencyPriceEntity sessionFrequencyPrice;

    @Column(name = "commission_percentage_applied", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentageApplied;
 
    @Column(name = "base_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseValue;
}

