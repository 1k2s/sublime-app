package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.billing.domain.BalanceTransferEntity;
import br.com.senai.sublime_app.contract.domain.ContractEntity;

// Uma transferência de saldo: o crédito de um contrato aplicado no cadastro de outro. Os dois
// contratos vêm com o plano e o número do aditivo, para a tela montar os rótulos
public record BalanceTransferResponseDTO(

        Long id,
        Long fromContractId,
        String fromPlanName,
        int fromAmendmentNumber,
        Long toContractId,
        String toPlanName,
        int toAmendmentNumber,
        BigDecimal amount,
        LocalDate transferredAt

) {
    public static BalanceTransferResponseDTO fromEntity(BalanceTransferEntity transfer) {
        ContractEntity from = transfer.getFromContract();
        ContractEntity to = transfer.getToContract();
        return new BalanceTransferResponseDTO(transfer.getId(), from.getId(), from.getPlan().getName(),
                from.getAmendmentNumber(), to.getId(), to.getPlan().getName(), to.getAmendmentNumber(),
                transfer.getAmount().amount(), transfer.getTransferredAt());
    }
}
