package br.com.senai.sublime_app.pricing.dto;

import java.util.List;

import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PricingModel;

// Tabela de preços vigentes de uma técnica. O pricingModel indica ao frontend qual
// campo preencher no contrato: sessionDurationPriceId (DURATION_BASED) ou
// sessionFrequencyPriceId (FREQUENCY_BASED).
public record TechniquePriceTableResponseDTO(

        Long techniqueId,
        String techniqueName,
        Long pricingGroupId,
        String pricingGroupName,
        PricingModel pricingModel,
        List<SessionPriceResponseDTO> prices

) {
    public static TechniquePriceTableResponseDTO fromEntity(TechniqueEntity technique, List<SessionPriceResponseDTO> prices) {
        return new TechniquePriceTableResponseDTO(
                technique.getId(),
                technique.getName(),
                technique.getPricingGroup().getId(),
                technique.getPricingGroup().getName(),
                technique.getPricingGroup().getPricingModel(),
                prices);
    }
}
