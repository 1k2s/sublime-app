package br.com.senai.sublime_app.pricing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlanRequestDTO(

        @NotBlank(message = "O nome do plano é obrigatório.")
        String name,

        // Opcional: os planos do Pilates em Grupo são mensalidade, sem quantidade de sessões
        @Min(value = 1, message = "A quantidade de sessões deve ser de no mínimo 1.")
        Integer sessionCount

) {
}
