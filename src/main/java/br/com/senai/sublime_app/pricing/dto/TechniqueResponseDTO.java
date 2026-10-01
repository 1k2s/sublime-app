package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PricingModel;

// O pricingModel vai junto para o frontend saber, antes de buscar os preços,
// se a tabela da técnica é por duração ou por frequência.
public record TechniqueResponseDTO(

        Long id,
        String name,
        Long pricingGroupId,
        String pricingGroupName,
        PricingModel pricingModel

) {
    public static TechniqueResponseDTO fromEntity(TechniqueEntity entity) {
        return new TechniqueResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getPricingGroup().getId(),
                entity.getPricingGroup().getName(),
                entity.getPricingGroup().getPricingModel());
    }
}
