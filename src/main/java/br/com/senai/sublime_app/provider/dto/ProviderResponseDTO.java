package br.com.senai.sublime_app.provider.dto;

import java.math.BigDecimal;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;

public record ProviderResponseDTO(

        Long id,
        Long userId,
        String name,
        BigDecimal commissionPercentage,
        boolean active

) {
    public static ProviderResponseDTO fromEntity(ProviderEntity entity) {
        return new ProviderResponseDTO(
                entity.getId(),
                entity.getUser().getId(),
                entity.getName(),
                entity.getCommissionPercentage().value(),
                entity.isActive());
    }
}
