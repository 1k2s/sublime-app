package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.billing.domain.PaymentSuggestion;

// Uma opção do "o que está sendo pago" no registro do pagamento: o tipo e o valor que ela preenche.
// dueDate: o vencimento da parcela, só na OPEN_INSTALLMENT
public record PaymentSuggestionResponseDTO(PaymentSuggestion.Type type, BigDecimal amount, LocalDate dueDate) {

    public static PaymentSuggestionResponseDTO fromSuggestion(PaymentSuggestion suggestion) {
        return new PaymentSuggestionResponseDTO(suggestion.type(), suggestion.amount().amount(),
                suggestion.dueDate());
    }
}
