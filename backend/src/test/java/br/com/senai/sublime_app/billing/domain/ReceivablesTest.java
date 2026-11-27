package br.com.senai.sublime_app.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.billing.domain.Receivables.Receivable;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.pricing.domain.Money;

// A previsão de recebimento e a taxa (ver "Payment" no domain-model). Teste de unidade puro.
class ReceivablesTest {

    private static final LocalDate PAID_AT = LocalDate.of(2026, 11, 26);

    @Test
    @DisplayName("Crédito 3x: taxa de 4,0% e três partes iguais, uma por mês")
    void creditInThreeTimes() {
        Money amount = money("1670.00");
        Money net = money("1603.20");

        Receivables receivables = Receivables.of(net, PaymentMethod.CREDIT_CARD, 3, PAID_AT);

        Money fee = PaymentEntity.feeOf(amount, net);
        assertThat(fee.amount()).isEqualByComparingTo("66.80");
        assertThat(Receivables.feePercentage(amount, fee)).isEqualByComparingTo("4.0");
        assertThat(receivables.parts()).extracting(Receivable::date)
                .containsExactly(PAID_AT.plusMonths(1), PAID_AT.plusMonths(2), PAID_AT.plusMonths(3));
        assertThat(receivables.parts()).extracting(part -> part.amount().amount())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("534.40"), new BigDecimal("534.40"), new BigDecimal("534.40"));
    }

    @Test
    @DisplayName("Os centavos que sobram vão para a primeira parte")
    void centsGoToTheFirstPart() {
        Receivables receivables = Receivables.of(money("1603.21"), PaymentMethod.CREDIT_CARD, 3, PAID_AT);

        assertThat(receivables.parts()).extracting(part -> part.amount().amount())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("534.41"), new BigDecimal("534.40"), new BigDecimal("534.40"));
    }

    @Test
    @DisplayName("Pix, débito e dinheiro: uma parte, na data do pagamento, sem taxa")
    void immediateMethods() {
        for (PaymentMethod method : new PaymentMethod[] { PaymentMethod.PIX, PaymentMethod.DEBIT_CARD,
                PaymentMethod.CASH }) {
            Receivables receivables = Receivables.of(money("668.00"), method, 1, PAID_AT);

            assertThat(receivables.parts()).containsExactly(new Receivable(PAID_AT, money("668.00")));
        }
        assertThat(Receivables.feePercentage(money("668.00"), money("0.00"))).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("Crédito em 31/01: a parte cai no último dia de fevereiro, e o dia volta em março")
    void creditKeepsTheDayAtTheEndOfTheMonth() {
        LocalDate paidAt = LocalDate.of(2027, 1, 31);

        Receivables receivables = Receivables.of(money("300.00"), PaymentMethod.CREDIT_CARD, 2, paidAt);

        assertThat(receivables.parts()).extracting(Receivable::date)
                .containsExactly(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
