package br.com.senai.sublime_app.contract.dto;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContractRequestDTO(

        @NotNull(message = "O paciente titular é obrigatório.")
        Long patientId,

        // nullable: Pilates em Dupla / plano Familiar
        Long beneficiaryId,

        @NotNull(message = "A técnica âncora é obrigatória.")
        Long techniqueId,

        @NotNull(message = "O plano é obrigatório.")
        Long planId,

        @NotNull(message = "A frequência semanal é obrigatória.")
        @Min(value = 1, message = "A frequência semanal deve ser de no mínimo 1 sessão.")
        Integer weeklyFrequency,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate startDate,

        @NotNull(message = "A data de término é obrigatória.")
        LocalDate endDate,

        @NotNull(message = "O método de pagamento é obrigatório.")
        PaymentMethod paymentMethod,

        // exclusive arc: exatamente um dos dois deve ser preenchido
        Long groupPlanPriceId,

        Long groupPlanFrequencyPriceId

) {
    /**
     * Valida a regra de exclusive arc:
     * exatamente um dos dois campos de FK de preço deve estar preenchido.
     */
    public boolean isValidExclusiveArc() {
        return (groupPlanPriceId != null && groupPlanFrequencyPriceId == null)
                || (groupPlanPriceId == null && groupPlanFrequencyPriceId != null);
    }
}