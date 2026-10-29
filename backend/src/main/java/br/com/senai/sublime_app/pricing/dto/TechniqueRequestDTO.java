package br.com.senai.sublime_app.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TechniqueRequestDTO(

        @NotBlank(message = "O nome da técnica é obrigatório.")
        String name,

        // Define o preço da técnica; não muda depois da criação
        @NotNull(message = "O grupo de preço é obrigatório.")
        Long pricingGroupId

) {
}
