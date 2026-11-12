package br.com.senai.sublime_app.contract.dto;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.Installments;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;

import java.math.BigDecimal;
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
        // soma dos itens (sessões × valor da linha travada)
        BigDecimal totalValue,
        int installments,
        // a primeira leva os centavos que sobram da divisão
        BigDecimal firstInstallmentValue,
        BigDecimal installmentValue,
        boolean active,
        // vigente hoje (isCurrentOn): a tela mostra Vigente, Vencido (ativo e não vigente) ou
        // Inativo sem repetir a regra da vigência
        boolean current,
        // versão anterior deste contrato (aditivo); null na primeira versão
        Long previousContractId,
        List<ContractItemResponseDTO> items

) {
    public static ContractResponseDTO fromEntity(ContractEntity entity) {
        Installments installments = entity.getInstallmentValues();
        return new ContractResponseDTO(
                entity.getId(),
                entity.getPatient().getId(),
                entity.getBeneficiary() != null ? entity.getBeneficiary().getId() : null,
                entity.getPlan().getId(),
                entity.getPlan().getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPaymentMethod(),
                entity.getTotalValue().amount(),
                installments.count(),
                installments.firstValue().amount(),
                installments.value().amount(),
                entity.isActive(),
                entity.isCurrentOn(LocalDate.now()),
                entity.getPreviousContract() != null ? entity.getPreviousContract().getId() : null,
                entity.getItems().stream().map(ContractItemResponseDTO::fromEntity).toList());
    }
}
