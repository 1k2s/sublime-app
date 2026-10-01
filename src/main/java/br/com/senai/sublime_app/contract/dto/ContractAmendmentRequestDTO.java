package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Estado completo da nova versão do contrato (aditivo). O frontend abre o formulário
// preenchido com a versão atual, o usuário altera o que quiser e envia tudo — assim
// a nova versão passa pelas mesmas regras da criação e não há ambiguidade entre
// "campo não enviado" e "campo removido".
// Não tem patientId: o titular é copiado da versão atual (trocar titular = contrato novo).
public record ContractAmendmentRequestDTO(

        // nullable: Pilates em Dupla / plano Familiar
        Long beneficiaryId,

        @NotNull(message = "A técnica âncora é obrigatória.")
        Long techniqueId,

        // Obrigatória para preço por duração; proibida para preço por frequência
        // (vem da linha de preço). Ver isWeeklyFrequencyConsistent().
        @Min(value = 1, message = "A frequência semanal deve ser de no mínimo 1 sessão.")
        Integer weeklyFrequency,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate startDate,

        @NotNull(message = "A data de término é obrigatória.")
        LocalDate endDate,

        @NotNull(message = "O método de pagamento é obrigatório.")
        PaymentMethod paymentMethod,

        // exclusive arc: exatamente um dos dois deve ser preenchido. Manter a linha
        // de preço da versão atual é permitido mesmo após um reajuste; trocar de
        // linha exige uma linha vigente.
        Long sessionDurationPriceId,

        Long sessionFrequencyPriceId

) {
    // Mesmas validações de formato do ContractRequestDTO (records não têm herança).

    @AssertTrue(message = "Informe exatamente um preço: sessionDurationPriceId ou sessionFrequencyPriceId.")
    public boolean isValidExclusiveArc() {
        return (sessionDurationPriceId != null && sessionFrequencyPriceId == null)
                || (sessionDurationPriceId == null && sessionFrequencyPriceId != null);
    }

    @AssertTrue(message = "A frequência semanal é obrigatória para preço por duração e não deve ser enviada para preço por frequência.")
    public boolean isWeeklyFrequencyConsistent() {
        if (sessionDurationPriceId != null && sessionFrequencyPriceId == null) {
            return weeklyFrequency != null;
        }
        if (sessionFrequencyPriceId != null && sessionDurationPriceId == null) {
            return weeklyFrequency == null;
        }
        return true; // arc inválido: a mensagem do isValidExclusiveArc já cobre o erro
    }
}
