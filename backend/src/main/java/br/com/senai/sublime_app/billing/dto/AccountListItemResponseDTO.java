package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.billing.domain.AccountStatus;
import br.com.senai.sublime_app.billing.domain.PatientAccountSummary;
import br.com.senai.sublime_app.billing.domain.ContractAccount;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.ContractStatus;

// Uma linha da lista do Financeiro: um paciente, com as contas somadas
public record AccountListItemResponseDTO(

        Long patientId,
        String patientName,
        boolean patientActive,
        // os contratos na conta, o vigente primeiro: a tela monta o rótulo ("Evolução · Aditivo 1")
        List<ContractLabelDTO> contracts,
        BigDecimal due,
        BigDecimal paid,
        // pago − devido: positivo = saldo credor; negativo = saldo devedor
        BigDecimal balance,
        AccountStatus status,
        boolean hasOverdue,
        int overdueCount,
        BigDecimal overdueAmount,
        LocalDate oldestOverdueDate,
        // nulo: nada a vencer
        NextDueResponseDTO nextDue,
        List<AccountWarningResponseDTO> warnings

) {
    // status: o do contrato (vigente, vencido, encerrado ou cancelado), com o saldo da conta
    public record ContractLabelDTO(Long contractId, String planName, int amendmentNumber, boolean active,
            ContractStatus status) {

        static ContractLabelDTO fromAccount(ContractAccount account) {
            ContractEntity contract = account.contract();
            return new ContractLabelDTO(contract.getId(), contract.getPlan().getName(), contract.getAmendmentNumber(),
                    contract.isActive(), account.contractStatus());
        }
    }

    public static AccountListItemResponseDTO fromSummary(PatientAccountSummary summary) {
        return new AccountListItemResponseDTO(
                summary.patient().getId(),
                summary.patient().getName(),
                summary.patient().isActive(),
                summary.contracts().stream().map(ContractLabelDTO::fromAccount).toList(),
                summary.due().amount(),
                summary.paid().amount(),
                summary.balance(),
                summary.status(),
                summary.hasOverdue(),
                summary.overdueCount(),
                summary.overdueAmount().amount(),
                summary.oldestOverdueDate().orElse(null),
                summary.nextDue().map(NextDueResponseDTO::fromNextDue).orElse(null),
                summary.warnings().stream().map(AccountWarningResponseDTO::fromWarning).toList());
    }
}
