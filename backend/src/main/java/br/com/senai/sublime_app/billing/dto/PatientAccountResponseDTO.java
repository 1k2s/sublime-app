package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.billing.domain.AccountStatus;
import br.com.senai.sublime_app.billing.domain.ContractAccount;
import br.com.senai.sublime_app.billing.domain.InstallmentState;
import br.com.senai.sublime_app.billing.domain.PatientAccountSummary;
import br.com.senai.sublime_app.billing.domain.SingleSessionAccount;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.ContractStatus;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;

// A conta do paciente (o Resumo): cada contrato na conta, o vigente primeiro, e o avulso
public record PatientAccountResponseDTO(

        Long patientId,
        String patientName,
        boolean patientActive,
        List<ContractAccountDTO> contracts,
        // nulo: sem avulso (nenhum atendimento nem pagamento sem contrato)
        SingleSessionDTO singleSession

) {
    // A conta de um contrato, pela versão atual (contractId), com as contas da cadeia de versões
    public record ContractAccountDTO(
            Long contractId,
            String planName,
            int amendmentNumber,
            LocalDate endDate,
            boolean active,
            // vigente, vencido, encerrado ou cancelado (com o saldo desta conta)
            ContractStatus contractStatus,
            BigDecimal totalValue,
            // os pagamentos + o crédito recebido − o enviado
            BigDecimal paid,
            // o crédito recebido de contratos anteriores e o enviado a um contrato novo
            BigDecimal creditReceived,
            BigDecimal creditTransferred,
            // o pago em % do total, sem casas
            int paidPercentage,
            BigDecimal consumed,
            BigDecimal groupFeesDue,
            BigDecimal due,
            // pago − devido: positivo = saldo credor; negativo = saldo devedor
            BigDecimal balance,
            AccountStatus status,
            // os atendimentos por duração da cadeia
            int sessionsUsed,
            InstallmentsSummaryDTO installments,
            List<ItemDTO> items,
            List<AccountWarningResponseDTO> warnings,
            List<PaymentSuggestionResponseDTO> paymentSuggestions) {

        static ContractAccountDTO fromAccount(ContractAccount account) {
            ContractEntity contract = account.contract();
            return new ContractAccountDTO(
                    contract.getId(),
                    contract.getPlan().getName(),
                    contract.getAmendmentNumber(),
                    contract.getEndDate(),
                    contract.isActive(),
                    account.contractStatus(),
                    contract.getTotalValue().amount(),
                    account.paid().amount(),
                    account.creditReceived().amount(),
                    account.creditTransferred().amount(),
                    account.paidPercentageOfTotal(),
                    account.consumed().amount(),
                    account.groupFeesDue().amount(),
                    account.due().amount(),
                    account.balance(),
                    account.status(),
                    account.sessionsUsed(),
                    InstallmentsSummaryDTO.fromAccount(account),
                    account.items().stream().map(ItemDTO::fromItem).toList(),
                    account.warnings().stream().map(AccountWarningResponseDTO::fromWarning).toList(),
                    account.suggestions().stream().map(PaymentSuggestionResponseDTO::fromSuggestion).toList());
        }
    }

    // A linha de atalho das parcelas da versão atual: pagas de total, atrasadas e a próxima
    public record InstallmentsSummaryDTO(int paidCount, int totalCount, int overdueCount, NextDueResponseDTO next) {

        static InstallmentsSummaryDTO fromAccount(ContractAccount account) {
            int paidCount = (int) account.installments().stream()
                    .filter(installment -> installment.status() == InstallmentState.Status.PAID)
                    .count();
            return new InstallmentsSummaryDTO(paidCount, account.installments().size(), account.overdueCount(),
                    account.nextDue().map(NextDueResponseDTO::fromInstallment).orElse(null));
        }
    }

    // Um item da versão atual. durationMinutes e frequency: os do item por duração (nulos no Grupo);
    // weeklyFrequency: a do Grupo. usedValue nulo e extra zero no Grupo (não cobra por aula)
    public record ItemDTO(
            Long pricingGroupId,
            String techniqueName,
            Integer durationMinutes,
            SessionFrequency frequency,
            Integer weeklyFrequency,
            boolean groupItem,
            boolean addedInCurrentVersion,
            int used,
            int contracted,
            BigDecimal usedValue,
            BigDecimal contractedValue,
            int extraCount,
            BigDecimal extraValue) {

        static ItemDTO fromItem(ContractAccount.ItemState item) {
            return new ItemDTO(item.pricingGroupId(), item.techniqueName(), item.durationMinutes(), item.frequency(),
                    item.weeklyFrequency(), item.groupItem(),
                    item.addedInCurrentVersion(), item.used(), item.contracted(),
                    item.usedValue() != null ? item.usedValue().amount() : null, item.contractedValue().amount(),
                    item.extraCount(), item.extraValue().amount());
        }
    }

    public record SingleSessionDTO(BigDecimal due, BigDecimal paid, BigDecimal balance,
            List<PaymentSuggestionResponseDTO> paymentSuggestions) {

        static SingleSessionDTO fromAccount(SingleSessionAccount account) {
            return new SingleSessionDTO(account.due().amount(), account.paid().amount(), account.balance(),
                    account.suggestions().stream().map(PaymentSuggestionResponseDTO::fromSuggestion).toList());
        }
    }

    public static PatientAccountResponseDTO fromSummary(PatientAccountSummary summary) {
        SingleSessionAccount singleSession = summary.singleSession();
        return new PatientAccountResponseDTO(
                summary.patient().getId(),
                summary.patient().getName(),
                summary.patient().isActive(),
                summary.contracts().stream().map(ContractAccountDTO::fromAccount).toList(),
                singleSession.exists() ? SingleSessionDTO.fromAccount(singleSession) : null);
    }
}
