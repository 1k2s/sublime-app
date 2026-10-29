package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;

public record PlanResponseDTO(

        Long id,
        String name,
        PlanType type,
        Integer sessionCount,
        Integer termWeeks,
        // de-para; null enquanto não definido (e sempre null no avulso)
        Long equivalentPlanId,
        String equivalentPlanName,
        boolean active

) {
    public static PlanResponseDTO fromEntity(PlanEntity entity) {
        PlanEntity equivalent = entity.getEquivalentPlan();
        return new PlanResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getSessionCount(),
                entity.getTermWeeks(),
                equivalent != null ? equivalent.getId() : null,
                equivalent != null ? equivalent.getName() : null,
                entity.isActive());
    }
}
