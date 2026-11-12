package br.com.senai.sublime_app.contract.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Parcelamento do contrato, em qualquer forma de pagamento e sem juros: de 1 até os meses do
// contrato (no máximo 12). O total é dividido pelas parcelas e os centavos que sobram vão para a
// primeira (ex: R$ 1.711,06 em 6x = 1ª de R$ 285,21 + 5 × R$ 285,17). Ver "Valor e parcelas" em
// Contract, no domain-model
public record Installments(int count, Money firstValue, Money value) {

    public static final int MAX_COUNT = 12;
    private static final double WEEKS_PER_MONTH = 52.0 / 12;

    // Meses do contrato pelas semanas (52 semanas = 12 meses), arredondados para o mais próximo,
    // de 1 a 12. Pelas semanas, não pelo calendário: o mesmo prazo dá sempre o mesmo máximo,
    // qualquer que seja o início (ex: 26 semanas = 6 meses; pelo calendário, 6 meses e 1 dia)
    public static int maxFor(int durationWeeks) {
        long months = Math.round(durationWeeks / WEEKS_PER_MONTH);
        return Math.clamp(months, 1, MAX_COUNT);
    }

    // De 1 até o máximo do contrato
    public static void validateCount(int count, int maxCount) {
        if (count < 1 || count > maxCount) {
            throw new BusinessRuleException(
                    "O número de parcelas deve ser de 1 a " + maxCount + " (meses do contrato).");
        }
    }

    // Valor base truncado nos centavos; a primeira parcela leva o que sobra
    public static Installments split(Money total, int count) {
        BigDecimal value = total.amount().divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal first = total.amount().subtract(value.multiply(BigDecimal.valueOf(count - 1L)));
        return new Installments(count, Money.of(first), Money.of(value));
    }
}
