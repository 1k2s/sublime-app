package br.com.senai.sublime_app.pricing.dto;

import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.enums.PricingModel;

public record PricingGroupResponseDTO(

        Long id,
        String name,
        PricingModel pricingModel

) {
    public static PricingGroupResponseDTO fromEntity(PricingGroupEntity entity) {
        return new PricingGroupResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getPricingModel());
    }
}
