package br.com.senai.sublime_app.contract.dto;

import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

// Um item do contrato: um grupo de preço contratado. O grupo e o plano vêm da linha
// de preço escolhida (fonte da verdade), nunca do request. As validações abaixo
// tratam do FORMATO; as regras do item ficam na ContractItemEntity.
public record ContractItemRequestDTO(

        // Só informativa (aparece no contrato impresso)
        @NotNull(message = "A técnica do item é obrigatória.")
        Long techniqueId,

        // exclusive arc: exatamente um dos dois deve ser preenchido
        Long sessionDurationPriceId,

        Long sessionFrequencyPriceId,

        // Obrigatória com preço por duração; proibida com preço por frequência
        // (no Pilates em Grupo a frequência já está na linha de preço)
        SessionFrequency frequency

) {
    @AssertTrue(message = "Informe exatamente um preço no item: sessionDurationPriceId ou sessionFrequencyPriceId.")
    public boolean isValidExclusiveArc() {
        return (sessionDurationPriceId != null && sessionFrequencyPriceId == null)
                || (sessionDurationPriceId == null && sessionFrequencyPriceId != null);
    }

    @AssertTrue(message = "A frequência é obrigatória com preço por duração e não deve ser enviada com preço por frequência.")
    public boolean isFrequencyConsistent() {
        if (sessionDurationPriceId != null && sessionFrequencyPriceId == null) {
            return frequency != null;
        }
        if (sessionFrequencyPriceId != null && sessionDurationPriceId == null) {
            return frequency == null;
        }
        return true; // arc inválido: a mensagem do isValidExclusiveArc já cobre o erro
    }
}
