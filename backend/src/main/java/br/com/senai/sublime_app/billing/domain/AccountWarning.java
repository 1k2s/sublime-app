package br.com.senai.sublime_app.billing.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.pricing.domain.Money;

// Um aviso da conta, com os dados para a tela montar o texto (ex: "Parcela de 10/11 vencida",
// "RPG: 2 sessões além das 12 contratadas (+ R$ 334,00)"). Os campos que não se aplicam ao tipo
// ficam nulos.
public record AccountWarning(Type type, Integer count, Money amount, LocalDate date, String techniqueName,
        Integer contractedCount) {

    public enum Type {
        // parcelas vencidas em aberto: count, amount (o que falta delas), date (a mais antiga)
        OVERDUE_INSTALLMENTS,
        // sessões além das contratadas num item por duração: techniqueName, count (as a mais),
        // contractedCount, amount (o valor a mais)
        BEYOND_CONTRACT,
        // atendimento avulso em aberto: amount (o saldo devedor do avulso)
        SINGLE_SESSION_OPEN,
        // contrato inativado com saldo devedor: amount
        INACTIVE_WITH_DEBT
    }
}
