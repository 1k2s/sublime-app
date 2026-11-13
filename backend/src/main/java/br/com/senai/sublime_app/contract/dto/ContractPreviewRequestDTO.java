package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.Installments;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Prévia da tela de criação (nada é gravado): o plano, o início e os itens já escolhidos, no
// mesmo formato do cadastro. A lista pode vir incompleta ou vazia: a tela manda só os itens com a
// linha escolhida, e as opções do plano do Grupo saem dos itens por duração
public record ContractPreviewRequestDTO(

        @NotNull(message = "O plano do contrato é obrigatório.")
        Long planId,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate startDate,

        @Valid
        List<ContractItemRequestDTO> items,

        // Opcional: vazio ou acima do máximo do contrato = o máximo (a resposta diz as aplicadas)
        @Min(value = 1, message = "O número de parcelas deve ser no mínimo 1.")
        @Max(value = Installments.MAX_COUNT, message = "O número de parcelas deve ser no máximo 12.")
        Integer installments

) {
    // Sem itens = lista vazia (o plano de mensalidade calcula a vigência sem eles)
    public List<ContractItemRequestDTO> itemsOrEmpty() {
        return items != null ? items : List.of();
    }
}
