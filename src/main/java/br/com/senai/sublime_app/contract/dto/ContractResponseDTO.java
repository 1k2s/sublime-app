package br.com.senai.sublime_app.contract.dto;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;

import java.time.LocalDate;
import java.util.List;

public record ContractResponseDTO(

        Long id,
        Long patientId,
        Long beneficiaryId,
        Long planId,
        String planName,
        LocalDate startDate,
        // calculado pelo sistema, inclusivo
        LocalDate endDate,
        PaymentMethod paymentMethod,
        boolean active,
        // versão anterior deste contrato (aditivo); null na primeira versão
        Long previousContractId,
        List<ContractItemResponseDTO> items

) {
    public static ContractResponseDTO fromEntity(ContractEntity entity) {
        return new ContractResponseDTO(
                entity.getId(),
                entity.getPatient().getId(),
                entity.getBeneficiary() != null ? entity.getBeneficiary().getId() : null,
                entity.getPlan().getId(),
                entity.getPlan().getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPaymentMethod(),
                entity.isActive(),
                entity.getPreviousContract() != null ? entity.getPreviousContract().getId() : null,
                entity.getItems().stream().map(ContractItemResponseDTO::fromEntity).toList());
    }
}
