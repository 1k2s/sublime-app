package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Dados de um item antes de virar ContractItemEntity — não é persistido. O service
// monta o rascunho com as entidades já carregadas; quem cria o item é o
// ContractEntity (raiz do agregado), porque só ele conhece a versão anterior.
public record ContractItemDraft(
        TechniqueEntity technique,
        // exclusive arc: exatamente uma das duas linhas
        SessionDurationPriceEntity durationPrice,
        SessionFrequencyPriceEntity frequencyPrice,
        // só com linha por duração (no Grupo a frequência já está na linha)
        SessionFrequency frequency) {

    // Construtor compacto: garante o exclusive arc em qualquer caminho de criação
    public ContractItemDraft {
        validateExclusiveArc(durationPrice, frequencyPrice);
    }

    public static ContractItemDraft withDurationPrice(TechniqueEntity technique,
            SessionDurationPriceEntity price, SessionFrequency frequency) {
        return new ContractItemDraft(technique, price, null, frequency);
    }

    public static ContractItemDraft withFrequencyPrice(TechniqueEntity technique,
            SessionFrequencyPriceEntity price) {
        return new ContractItemDraft(technique, null, price, null);
    }

    // O grupo vem da linha de preço (nunca do request)
    public PricingGroupEntity pricingGroup() {
        return durationPrice != null ? durationPrice.getPricingGroup() : frequencyPrice.getPricingGroup();
    }

    public boolean isDurationBased() {
        return durationPrice != null;
    }

    private static void validateExclusiveArc(SessionDurationPriceEntity durationPrice,
            SessionFrequencyPriceEntity frequencyPrice) {
        if ((durationPrice == null) == (frequencyPrice == null)) {
            throw new BusinessRuleException("Cada item precisa de exatamente um preço: por duração ou por frequência.");
        }
    }
}
