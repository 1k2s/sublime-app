package br.com.senai.sublime_app.pricing.dto;

import jakarta.validation.constraints.NotNull;

// De-para: o plano equivalente na outra família (pacote ⇄ mensalidade)
public record PlanEquivalentRequestDTO(

        @NotNull(message = "O plano equivalente é obrigatório.")
        Long equivalentPlanId

) {
}
