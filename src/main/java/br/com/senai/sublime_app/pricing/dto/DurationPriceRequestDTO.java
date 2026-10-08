package br.com.senai.sublime_app.pricing.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Cadastro de valor (preço novo ou reajuste) para grupos DURATION_BASED.
// validFrom não vem do request: é sempre hoje (ver domain-model.md).
public record DurationPriceRequestDTO(

        @NotNull(message = "O grupo de preço é obrigatório.")
        Long pricingGroupId,

        @NotNull(message = "O plano é obrigatório.")
        Long planId,

        @NotNull(message = "A duração da sessão é obrigatória.")
        @Min(value = 1, message = "A duração da sessão deve ser de no mínimo 1 minuto.")
        Integer durationMinutes,

        @NotNull(message = "O valor da sessão é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O valor da sessão deve ser maior que zero.")
        BigDecimal sessionValue

) {
}
