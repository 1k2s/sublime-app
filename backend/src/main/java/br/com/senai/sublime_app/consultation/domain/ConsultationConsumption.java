package br.com.senai.sublime_app.consultation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.pricing.domain.Money;

// O que um atendimento ativo pesa na conta do paciente (o módulo billing lê daqui): só os campos
// que a conta usa, numa consulta, sem carregar as entidades. Ver "Saldo de sessões" em
// Consultation, no domain-model.
// - contractId nulo = avulso (caso 3); durationBased = a linha usada é por duração;
//   pricingGroupId = o grupo da linha usada (por duração ou por frequência): o item do contrato.
public record ConsultationConsumption(Long consultationId, Long contractId, Long patientId,
        PricingSource pricingSource, Money baseValue, boolean durationBased, Long pricingGroupId,
        LocalDate occurredAt) {

    // A dívida é o consumo: o caso 1 num item por duração, o caso 2 (de-para, a técnica fora do
    // contrato) e o caso 3 (avulso). O caso 1 no Pilates em Grupo não conta: ali a dívida é a
    // mensalidade, e as aulas são só controle de presença
    public boolean countsAsConsumed() {
        return switch (pricingSource) {
            case CONTRACT_ITEM -> durationBased;
            case EQUIVALENT_PLAN, SINGLE_SESSION -> true;
        };
    }

    // A soma do que conta como consumido. Zero na lista vazia
    public static Money sumConsumed(List<ConsultationConsumption> consumptions) {
        return Money.of(consumptions.stream()
                .filter(ConsultationConsumption::countsAsConsumed)
                .map(consumption -> consumption.baseValue().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
