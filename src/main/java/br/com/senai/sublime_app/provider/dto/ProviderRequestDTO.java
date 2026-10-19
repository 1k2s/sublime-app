package br.com.senai.sublime_app.provider.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cadastro do prestador. A alteração usa o ProviderUpdateRequestDTO, sem userId:
// o vínculo com o usuário é fixo após a criação.
public record ProviderRequestDTO(

        @NotNull(message = "O usuário do prestador é obrigatório.")
        Long userId,

        @NotBlank(message = "O nome do prestador é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome do prestador deve ter entre 3 e 100 caracteres.")
        String name,

        // Opcional: vazio = padrão da clínica (40%). Formato 0–100 (35.00 = 35%);
        // mais de 2 casas é barrado pelo Percentage.
        @DecimalMin(value = "0.00", message = "O percentual de comissão deve ser no mínimo 0.")
        @DecimalMax(value = "100.00", message = "O percentual de comissão deve ser no máximo 100.")
        BigDecimal commissionPercentage,

        // Opcional: vazio = padrão da clínica (45%). Pacientes que o prestador trouxe;
        // nunca menor que o percentual padrão (regra da ProviderEntity).
        @DecimalMin(value = "0.00", message = "O percentual de pacientes trazidos deve ser no mínimo 0.")
        @DecimalMax(value = "100.00", message = "O percentual de pacientes trazidos deve ser no máximo 100.")
        BigDecimal referralCommissionPercentage

) {
}
