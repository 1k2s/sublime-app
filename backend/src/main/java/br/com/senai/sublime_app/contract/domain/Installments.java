package br.com.senai.sublime_app.contract.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Parcelamento do contrato, em qualquer forma de pagamento e sem juros: de 1 até os meses do
// contrato (no máximo 12). O valor parcelado é dividido pelas parcelas e os centavos que sobram
// vão para a primeira (ex: R$ 1.711,06 em 6x = 1ª de R$ 285,21 + 5 × R$ 285,17). As parcelas
// vencem uma por mês a partir do 1º vencimento, sempre contando dele (o dia 31 vai ao último dia
// do mês curto e volta no seguinte). O pago é distribuído pela ordem de vencimento. Ver "Valor e
// parcelas" em Contract, no domain-model
public record Installments(int count, Money firstValue, Money value) {

    public static final int MAX_COUNT = 12;
    private static final double WEEKS_PER_MONTH = 52.0 / 12;

    // Uma parcela do cronograma (number começa em 1)
    public record Scheduled(int number, LocalDate dueDate, Money value) {
    }

    // Meses do contrato pelas semanas (52 semanas = 12 meses), arredondados para o mais próximo,
    // de 1 a 12. Pelas semanas, não pelo calendário: o mesmo prazo dá sempre o mesmo máximo,
    // qualquer que seja o início (ex: 26 semanas = 6 meses; pelo calendário, 6 meses e 1 dia)
    public static int maxFor(int durationWeeks) {
        long months = Math.round(durationWeeks / WEEKS_PER_MONTH);
        return Math.clamp(months, 1, MAX_COUNT);
    }

    // Máximo de parcelas num período (do 1º vencimento ao fim do contrato, no aditivo): pela
    // mesma conta de meses do maxFor; no mínimo 1, mesmo com o fim antes do 1º vencimento
    public static int maxForPeriod(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return maxFor((int) ((days + 6) / 7));
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

    // Vencimento da parcela de índice "index" (0 = a primeira): o 1º vencimento + index meses
    public static LocalDate dueDateOf(LocalDate firstDueDate, int index) {
        return firstDueDate.plusMonths(index);
    }

    // O valor de cada parcela, na ordem
    public List<Money> values() {
        List<Money> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            values.add(index == 0 ? firstValue : value);
        }
        return values;
    }

    // Vencimento e valor de cada parcela
    public List<Scheduled> scheduleFrom(LocalDate firstDueDate) {
        List<Money> values = values();
        List<Scheduled> schedule = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            schedule.add(new Scheduled(index + 1, dueDateOf(firstDueDate, index), values.get(index)));
        }
        return schedule;
    }

    // O pago distribuído pela ordem de vencimento: cada parcela recebe até o valor dela, e o que
    // sobra passa à seguinte. O pago fica entre 0 e o valor parcelado (o que passar é saldo da
    // conta, não das parcelas). Um valor por parcela, na ordem
    public List<Money> distribute(Money paid) {
        BigDecimal remaining = paid.amount();
        List<Money> paidPerInstallment = new ArrayList<>();
        for (Money installmentValue : values()) {
            BigDecimal share = remaining.min(installmentValue.amount()).max(BigDecimal.ZERO);
            paidPerInstallment.add(Money.of(share));
            remaining = remaining.subtract(share);
        }
        return paidPerInstallment;
    }
}
