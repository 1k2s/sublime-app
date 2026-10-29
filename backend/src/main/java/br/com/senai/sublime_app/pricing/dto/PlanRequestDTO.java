package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.enums.PlanType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// A coerência entre o tipo e os campos de quantidade (qual é obrigatório, qual
// precisa vir vazio) é regra do plano: fica na PlanEntity.
public record PlanRequestDTO(

        @NotBlank(message = "O nome do plano é obrigatório.")
        String name,

        // Não muda depois da criação
        @NotNull(message = "O tipo do plano é obrigatório.")
        PlanType type,

        // Avulso (1) e pacote; vazio na mensalidade do Pilates em Grupo
        @Min(value = 1, message = "A quantidade de sessões deve ser de no mínimo 1.")
        Integer sessionCount,

        // Só na mensalidade do Pilates em Grupo
        @Min(value = 1, message = "O prazo em semanas deve ser de no mínimo 1.")
        Integer termWeeks

) {
}
