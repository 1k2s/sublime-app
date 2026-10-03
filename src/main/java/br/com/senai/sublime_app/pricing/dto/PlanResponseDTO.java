package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;

public record PlanResponseDTO(

        Long id,
        String name,
        Integer sessionCount,
        boolean active

) {
    public static PlanResponseDTO fromEntity(PlanEntity entity) {
        return new PlanResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getSessionCount(),
                entity.isActive());
    }
}
