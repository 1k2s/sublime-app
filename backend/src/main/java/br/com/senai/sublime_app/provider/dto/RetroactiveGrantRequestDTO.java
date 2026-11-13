package br.com.senai.sublime_app.provider.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Liberação de lançamentos retroativos: só a quantidade (o dia é hoje e o admin é o logado)
public record RetroactiveGrantRequestDTO(

        @NotNull(message = "A quantidade de lançamentos é obrigatória.")
        @Min(value = 1, message = "A quantidade de lançamentos deve ser de 1 a 50.")
        @Max(value = 50, message = "A quantidade de lançamentos deve ser de 1 a 50.")
        Integer allowedCount

) {
}
