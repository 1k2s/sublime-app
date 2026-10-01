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
        Long sessionDurationPriceId,
        Long sessionFrequencyPriceId,
        boolean active,
        // versão anterior deste contrato (aditivo); null na primeira versão
        Long previousContractId

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
                entity.getSessionDurationPrice() != null ? entity.getSessionDurationPrice().getId() : null,
                entity.getSessionFrequencyPrice() != null ? entity.getSessionFrequencyPrice().getId() : null,
                entity.isActive(),
                entity.getPreviousContract() != null ? entity.getPreviousContract().getId() : null);
    }
}