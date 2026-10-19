package br.com.senai.sublime_app.provider.dto;

import java.math.BigDecimal;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;

public record ProviderResponseDTO(

        Long id,
        Long userId,
        String name,
        // pacientes da clínica ou trazidos por outro prestador
        BigDecimal commissionPercentage,
        // pacientes que este prestador trouxe
        BigDecimal referralCommissionPercentage,
        boolean active

) {
    public static ProviderResponseDTO fromEntity(ProviderEntity entity) {
        return new ProviderResponseDTO(
                entity.getId(),
                entity.getUser().getId(),
                entity.getName(),
                entity.getCommissionPercentage().value(),
                entity.getReferralCommissionPercentage().value(),
                entity.isActive());
    }
}
