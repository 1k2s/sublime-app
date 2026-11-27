package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.billing.domain.ContractInstallmentHistory;
import br.com.senai.sublime_app.billing.domain.InstallmentState;
import br.com.senai.sublime_app.contract.domain.ContractEntity;

// As parcelas de uma versão do contrato (a aba Parcelas), com a situação de cada uma
public record ContractVersionInstallmentsResponseDTO(

        Long contractId,
        int amendmentNumber,
        LocalDate createdAt,
        boolean current,
        BigDecimal totalValue,
        BigDecimal installmentBase,
        // o total menos o valor parcelado (zero na versão original)
        BigDecimal paidBeforeAmendment,
        int dueDay,
        List<InstallmentDTO> installments

) {
    // remaining: o que falta da parcela (valor − pago)
    public record InstallmentDTO(int number, LocalDate dueDate, BigDecimal value, BigDecimal paid,
            BigDecimal remaining, InstallmentState.Status status) {

        static InstallmentDTO fromState(InstallmentState installment) {
            return new InstallmentDTO(installment.number(), installment.dueDate(), installment.value().amount(),
                    installment.paid().amount(), installment.remaining().amount(), installment.status());
        }
    }

    public static ContractVersionInstallmentsResponseDTO fromVersion(ContractInstallmentHistory.Version version) {
        ContractEntity contract = version.contract();
        return new ContractVersionInstallmentsResponseDTO(
                contract.getId(),
                contract.getAmendmentNumber(),
                contract.getCreatedAt(),
                version.current(),
                contract.getTotalValue().amount(),
                contract.getInstallmentBase().amount(),
                version.paidBeforeAmendment().amount(),
                version.dueDay(),
                version.installments().stream().map(InstallmentDTO::fromState).toList());
    }
}
