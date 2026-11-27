package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.billing.domain.PaymentEntity;
import br.com.senai.sublime_app.billing.domain.Receivables;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;

// Um pagamento. O contrato vem com o plano e o número do aditivo, para a tela montar o rótulo
// ("Evolução · Aditivo 1") como a tela de Contratos; contrato nulo = avulso
public record PaymentResponseDTO(

        Long id,
        Long patientId,
        Long contractId,
        String contractPlanName,
        Integer contractAmendmentNumber,
        // bruto, líquido e a taxa (bruto − líquido), em valor e em % do bruto
        BigDecimal amount,
        BigDecimal netAmount,
        BigDecimal fee,
        BigDecimal feePercentage,
        PaymentMethod paymentMethod,
        int cardInstallments,
        LocalDate paidAt,
        boolean active

) {
    public static PaymentResponseDTO fromEntity(PaymentEntity payment) {
        ContractEntity contract = payment.getContract();
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getPatient().getId(),
                contract != null ? contract.getId() : null,
                contract != null ? contract.getPlan().getName() : null,
                contract != null ? contract.getAmendmentNumber() : null,
                payment.getAmount().amount(),
                payment.getNetAmount().amount(),
                payment.getFee().amount(),
                Receivables.feePercentage(payment.getAmount(), payment.getFee()),
                payment.getPaymentMethod(),
                payment.getCardInstallments(),
                payment.getPaidAt(),
                payment.isActive());
    }
}
