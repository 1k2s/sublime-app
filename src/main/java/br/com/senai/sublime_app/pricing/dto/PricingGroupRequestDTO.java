package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.enums.PricingModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PricingGroupRequestDTO(

        @NotBlank(message = "O nome do grupo de preço é obrigatório.")
        String name,

        // Define em qual tabela de preço o grupo é precificado; não muda depois da criação
        @NotNull(message = "O modelo de precificação é obrigatório.")
        PricingModel pricingModel

) {
}
