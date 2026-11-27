package br.com.senai.sublime_app.billing.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.senai.sublime_app.pricing.domain.Money;

// Uma sugestão do "o que está sendo pago" no registro do pagamento: o tipo e o valor que ela
// preenche (o admin pode mudar: o pagamento é de valor livre). Só as que valem para a conta.
// dueDate: o vencimento da parcela, só na OPEN_INSTALLMENT (nulo nas outras)
public record PaymentSuggestion(Type type, Money amount, LocalDate dueDate) {

    public enum Type {
        // o que falta da parcela vencida mais antiga (ou da próxima, se nenhuma venceu)
        OPEN_INSTALLMENT,
        // o que falta de todas as parcelas em aberto da versão
        ALL_OPEN_INSTALLMENTS,
        // devido − pago (ex: sessões além das contratadas, que as parcelas não cobrem)
        BALANCE_DUE,
        // o saldo devedor do avulso
        SINGLE_SESSION_DUE
    }

    // Acrescenta a sugestão se o valor for maior que zero e diferente das que já estão na lista
    static void addIfUseful(List<PaymentSuggestion> suggestions, Type type, Money amount) {
        addIfUseful(suggestions, type, amount, null);
    }

    static void addIfUseful(List<PaymentSuggestion> suggestions, Type type, Money amount, LocalDate dueDate) {
        boolean repeated = suggestions.stream()
                .anyMatch(suggestion -> suggestion.amount().amount().compareTo(amount.amount()) == 0);
        if (amount.isPositive() && !repeated) {
            suggestions.add(new PaymentSuggestion(type, amount, dueDate));
        }
    }

    static List<PaymentSuggestion> newList() {
        return new ArrayList<>();
    }
}
