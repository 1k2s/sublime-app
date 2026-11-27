package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.Installments;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.pricing.domain.Money;

// Previsão de recebimento de um pagamento: quando o líquido cai na conta da clínica. Calculada,
// nunca gravada (é uma estimativa: com um gateway, a data viria dele).
// - Pix, débito e dinheiro: uma parte, na data do pagamento (o débito cai em D+1: um dia não muda
//   o mês do faturamento);
// - crédito em N vezes: N partes do líquido, na data do pagamento + 1, + 2… meses, com os centavos
//   que sobram na primeira (a mesma divisão das parcelas do contrato). O plusMonths leva o dia 31
//   ao último dia do mês curto, contando sempre da data do pagamento.
// Ver "Payment" no domain-model.
public record Receivables(List<Receivable> parts) {

    // Uma parte da previsão
    public record Receivable(LocalDate date, Money amount) {
    }

    public static Receivables of(Money netAmount, PaymentMethod paymentMethod, int cardInstallments,
            LocalDate paidAt) {
        if (paymentMethod != PaymentMethod.CREDIT_CARD) {
            return new Receivables(List.of(new Receivable(paidAt, netAmount)));
        }
        Installments split = Installments.split(netAmount, cardInstallments);
        List<Receivable> parts = new ArrayList<>();
        for (int month = 1; month <= cardInstallments; month++) {
            Money amount = month == 1 ? split.firstValue() : split.value();
            parts.add(new Receivable(paidAt.plusMonths(month), amount));
        }
        return new Receivables(List.copyOf(parts));
    }

    // A taxa em porcentagem do bruto, com uma casa (ex: 4,0%). Zero quando não há taxa
    public static BigDecimal feePercentage(Money amount, Money fee) {
        return fee.amount()
                .multiply(BigDecimal.valueOf(100))
                .divide(amount.amount(), 1, RoundingMode.HALF_UP);
    }
}
