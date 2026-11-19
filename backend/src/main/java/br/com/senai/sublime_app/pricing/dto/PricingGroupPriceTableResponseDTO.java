package br.com.senai.sublime_app.pricing.dto;

import java.util.List;

import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.enums.PricingModel;

// Tabela de preços de um grupo (tela de preços): as linhas vigentes ou o histórico, conforme o
// endpoint. O preço pertence ao grupo (ADR 0004); a tabela por técnica
// (TechniquePriceTableResponseDTO) atende o contrato.
public record PricingGroupPriceTableResponseDTO(

        Long pricingGroupId,
        String pricingGroupName,
        PricingModel pricingModel,
        List<SessionPriceResponseDTO> prices

) {
    public static PricingGroupPriceTableResponseDTO fromEntity(PricingGroupEntity pricingGroup, List<SessionPriceResponseDTO> prices) {
        return new PricingGroupPriceTableResponseDTO(
                pricingGroup.getId(),
                pricingGroup.getName(),
                pricingGroup.getPricingModel(),
                prices);
    }
}
