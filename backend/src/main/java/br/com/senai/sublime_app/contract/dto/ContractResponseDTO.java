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
        // 0 na versão original, 1 no primeiro aditivo...: a tela mostra "Aditivo N" sem conta
        int amendmentNumber,
        // dia em que a versão foi criada (cadastro ou aditivo)
        LocalDate createdAt,
        // substituída por um aditivo (inativa por isso, e não inativada): calculado, não gravado
        boolean superseded,
        List<ContractItemResponseDTO> items

) {
    // superseded vem de quem monta a resposta: na listagem é sempre falso (só vêm as
    // versões atuais); nas versões, sai da posição na cadeia; no detalhe, de uma consulta
    public static ContractResponseDTO fromEntity(ContractEntity entity, boolean superseded) {
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
                entity.getAmendmentNumber(),
                entity.getCreatedAt(),
                superseded,
                entity.getItems().stream().map(ContractItemResponseDTO::fromEntity).toList());
    }
}
