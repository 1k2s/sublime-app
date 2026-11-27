package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.Installments;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Prévia do aditivo (nada é gravado): só o que muda as contas. Separado do pedido do aditivo
// (ContractAmendmentRequestDTO), que exige forma de pagamento e itens: com o formulário em
// preenchimento, a prévia daria 400. Beneficiário e forma de pagamento não mudam as contas.
public record ContractAmendmentPreviewRequestDTO(

        // Opcional: prorrogação (precisa passar do fim atual, como no aditivo)
        LocalDate endDate,

        // O estado dos itens no formulário, no formato do aditivo; pode vir incompleto ou vazio
        @Valid
        List<ContractItemRequestDTO> items,

        // Opcional: vazio ou acima do máximo da nova vigência = o máximo (a resposta diz as
        // aplicadas). O aditivo de verdade recusa acima do máximo
        @Min(value = 1, message = "O número de parcelas deve ser no mínimo 1.")
        @Max(value = Installments.MAX_COUNT, message = "O número de parcelas deve ser no máximo 12.")
        Integer installments

) {
    public List<ContractItemRequestDTO> itemsOrEmpty() {
        return items != null ? items : List.of();
    }
}
