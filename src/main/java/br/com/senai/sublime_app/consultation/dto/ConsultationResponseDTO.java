package br.com.senai.sublime_app.consultation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

public record ConsultationResponseDTO(

        Long id,
        Long patientId,
        String patientName,
        // nulo só no Avulso (paciente sem contrato vinculado)
        Long contractId,
        Long providerId,
        String providerName,
        Long techniqueId,
        String techniqueName,
        int durationMinutes,
        LocalDate occurredAt,
        ConsultationStatus status,
        // caso do preço: CONTRACT_ITEM (debita o item), EQUIVALENT_PLAN (de-para), SINGLE_SESSION (Avulso)
        PricingSource pricingSource,
        // exclusive arc: só uma das linhas vem preenchida
        Long sessionDurationPriceId,
        Long sessionFrequencyPriceId,
        String planName,
        BigDecimal baseValue,
        BigDecimal commissionPercentageApplied,
        BigDecimal payoutValue,
        boolean active,
        // Saldo do item depois do lançamento: só na resposta do lançamento (POST) e só
        // quando ele debitou um item por duração. Negativo = saldo esgotado (aviso).
        Integer remainingSessions

) {
    public static ConsultationResponseDTO fromEntity(ConsultationEntity entity) {
        return fromEntity(entity, null);
    }

    public static ConsultationResponseDTO fromEntity(ConsultationEntity entity, Integer remainingSessions) {
        SessionDurationPriceEntity durationPrice = entity.getSessionDurationPrice();
        SessionFrequencyPriceEntity frequencyPrice = entity.getSessionFrequencyPrice();
        PlanEntity plan = durationPrice != null ? durationPrice.getPlan() : frequencyPrice.getPlan();
        return new ConsultationResponseDTO(
                entity.getId(),
                entity.getPatient().getId(),
                entity.getPatient().getName(),
                entity.getContract() != null ? entity.getContract().getId() : null,
                entity.getProvider().getId(),
                entity.getProvider().getName(),
                entity.getTechnique().getId(),
                entity.getTechnique().getName(),
                entity.getDurationMinutes(),
                entity.getOccurredAt(),
                entity.getStatus(),
                entity.getPricingSource(),
                durationPrice != null ? durationPrice.getId() : null,
                frequencyPrice != null ? frequencyPrice.getId() : null,
                plan.getName(),
                entity.getBaseValue().amount(),
                entity.getCommissionPercentageApplied().value(),
                entity.getPayoutValue().amount(),
                entity.isActive(),
                remainingSessions);
    }
}
