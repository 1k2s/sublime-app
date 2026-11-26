package br.com.senai.sublime_app.contract.dto;

import java.math.BigDecimal;

import br.com.senai.sublime_app.contract.domain.ContractItemDraft;
import br.com.senai.sublime_app.contract.domain.ContractItemValue;

// Valor de um item na prévia, na mesma ordem dos itens enviados. A linha (uma das duas) diz de
// qual item é
public record ContractItemPreviewDTO(

        Long sessionDurationPriceId,
        Long sessionFrequencyPriceId,
        // sessões que o item dá no contrato (Pilates em Grupo: semanas do contrato × vezes por semana)
        int sessionCount,
        BigDecimal sessionValue,
        BigDecimal totalValue

) {
    public static ContractItemPreviewDTO fromDraft(ContractItemDraft draft, int contractWeeks) {
        ContractItemValue value = draft.value(contractWeeks);
        return new ContractItemPreviewDTO(
                draft.durationPrice() != null ? draft.durationPrice().getId() : null,
                draft.frequencyPrice() != null ? draft.frequencyPrice().getId() : null,
                value.sessionCount(),
                value.sessionValue().amount(),
                value.totalValue().amount());
    }
}
