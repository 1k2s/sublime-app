package br.com.senai.sublime_app.contract.dto;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;

import java.time.LocalDate;

public record ContractResponseDTO(

        Long id,
        Long patientId,
        Long beneficiaryId,
        Long techniqueId,
        Long planId,
        Integer weeklyFrequency,
        LocalDate startDate,
        LocalDate endDate,
        PaymentMethod paymentMethod,
        Long groupPlanPriceId,
        Long groupPlanFrequencyPriceId,
        boolean active

) {
    public static ContractResponseDTO fromEntity(ContractEntity entity) {
        return new ContractResponseDTO(
                entity.getId(),
                entity.getPatient().getId(),
                entity.getBeneficiary() != null ? entity.getBeneficiary().getId() : null,
                entity.getTechnique().getId(),
                entity.getPlan().getId(),
                entity.getWeeklyFrequency(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPaymentMethod(),
                entity.getGroupPlanPrice() != null ? entity.getGroupPlanPrice().getId() : null,
                entity.getGroupPlanFrequencyPrice() != null ? entity.getGroupPlanFrequencyPrice().getId() : null,
                entity.isActive()
        );
    }
}