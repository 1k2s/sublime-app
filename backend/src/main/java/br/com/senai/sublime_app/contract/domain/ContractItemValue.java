package br.com.senai.sublime_app.contract.domain;

import java.math.BigDecimal;
import java.util.List;

import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

// Valor de um item do contrato: as sessões que ele dá × o valor da sessão da linha travada.
// - item por duração: as sessões do plano da linha (o plano do contrato);
// - Pilates em Grupo: as aulas que cabem na vigência do contrato (semanas do contrato × vezes
//   por semana), ao preço da aula do plano escolhido. O valor da aula é o que manda; o plano dá
//   a condição (ver o ADR 0041).
// Calculado a partir da linha e da vigência, nunca gravado. O item salvo e o rascunho da prévia
// usam esta mesma conta. Ver "Valor e parcelas" em Contract, no domain-model
public record ContractItemValue(int sessionCount, Money sessionValue) {

    // Exatamente uma das linhas vem preenchida (exclusive arc, garantido por quem chama).
    // contractWeeks: a vigência do contrato em semanas (só o item do Grupo usa)
    public static ContractItemValue of(SessionDurationPriceEntity durationPrice,
            SessionFrequencyPriceEntity frequencyPrice, int contractWeeks) {
        if (durationPrice != null) {
            return new ContractItemValue(durationPrice.getPlan().getSessionCount(), durationPrice.getSessionValue());
        }
        return new ContractItemValue(contractWeeks * frequencyPrice.getWeeklyFrequency(),
                frequencyPrice.getSessionValue());
    }

    public Money totalValue() {
        return Money.of(sessionValue.amount().multiply(BigDecimal.valueOf(sessionCount)));
    }

    // Total do contrato: a soma dos itens, sem desconto
    public static Money sumOf(List<ContractItemValue> values) {
        BigDecimal total = values.stream()
                .map(value -> value.totalValue().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(total);
    }
}
