package br.com.senai.sublime_app.provider.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Alteração do prestador. Não recebe userId: o vínculo com o usuário é fixo após a
// criação (mesma lógica do ContractAmendmentRequestDTO, que não recebe o titular).
// Os dois percentuais são obrigatórios: o padrão da clínica só vale no cadastro.
public record ProviderUpdateRequestDTO(

        @NotBlank(message = "O nome do prestador é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome do prestador deve ter entre 3 e 100 caracteres.")
        String name,

        // Formato 0–100 (35.00 = 35%). Mais de 2 casas é barrado pelo Percentage.
        @NotNull(message = "O percentual de comissão é obrigatório.")
        @DecimalMin(value = "0.00", message = "O percentual de comissão deve ser no mínimo 0.")
        @DecimalMax(value = "100.00", message = "O percentual de comissão deve ser no máximo 100.")
        BigDecimal commissionPercentage,

        // Pacientes que o prestador trouxe; nunca menor que o percentual padrão
        @NotNull(message = "O percentual de pacientes trazidos é obrigatório.")
        @DecimalMin(value = "0.00", message = "O percentual de pacientes trazidos deve ser no mínimo 0.")
        @DecimalMax(value = "100.00", message = "O percentual de pacientes trazidos deve ser no máximo 100.")
        BigDecimal referralCommissionPercentage

) {
}
