package br.com.senai.sublime_app.contract.dto;

import java.math.BigDecimal;

import br.com.senai.sublime_app.contract.domain.ContractItemEntity;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

// Um item do contrato. Grupo, plano, eixo (duração ou frequência semanal) e valor vêm
// da linha de preço travada — só um dos eixos vem preenchido (exclusive arc).
public record ContractItemResponseDTO(

        Long id,
        Long pricingGroupId,
        String pricingGroupName,
        Long techniqueId,
        String techniqueName,
        Long sessionDurationPriceId,
        Long sessionFrequencyPriceId,
        Long planId,
        String planName,
        Integer durationMinutes,
        Integer weeklyFrequency,
        // ritmo negociado; só em item de grupo por duração
        SessionFrequency frequency,
        BigDecimal sessionValue

) {
    public static ContractItemResponseDTO fromEntity(ContractItemEntity item) {
        SessionDurationPriceEntity durationPrice = item.getSessionDurationPrice();
        SessionFrequencyPriceEntity frequencyPrice = item.getSessionFrequencyPrice();
        PlanEntity plan = durationPrice != null ? durationPrice.getPlan() : frequencyPrice.getPlan();
        return new ContractItemResponseDTO(
                item.getId(),
                item.getPricingGroup().getId(),
                item.getPricingGroup().getName(),
                item.getTechnique().getId(),
                item.getTechnique().getName(),
                durationPrice != null ? durationPrice.getId() : null,
                frequencyPrice != null ? frequencyPrice.getId() : null,
                plan.getId(),
                plan.getName(),
                durationPrice != null ? durationPrice.getDurationMinutes() : null,
                frequencyPrice != null ? frequencyPrice.getWeeklyFrequency() : null,
                item.getFrequency(),
                durationPrice != null ? durationPrice.getSessionValue().amount() : frequencyPrice.getSessionValue().amount());
    }
}
