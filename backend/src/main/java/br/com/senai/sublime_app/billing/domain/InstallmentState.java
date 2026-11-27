package br.com.senai.sublime_app.billing.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.pricing.domain.Money;

// Uma parcela de uma versão do contrato, com o que foi pago dela e a situação
public record InstallmentState(int number, LocalDate dueDate, Money value, Money paid, Status status) {

    public enum Status {
        PAID,
        // venceu e não está paga
        OVERDUE,
        // a primeira a vencer, ainda em aberto
        NEXT,
        UPCOMING,
        // a versão foi substituída por um aditivo, que parcelou de novo o que faltava
        REPLACED,
        // o contrato foi inativado antes do vencimento: nada vence depois da inativação
        CANCELED,
        // a vigência do contrato terminou com ela em aberto: não é mais cobrada, e a conta do
        // contrato terminado vale só pelo saldo
        ENDED
    }

    // O que falta pagar da parcela
    public Money remaining() {
        return Money.of(value.amount().subtract(paid.amount()));
    }

    // Em aberto e ainda cobrável (vencida, a próxima ou a vencer)
    public boolean isOpen() {
        return status == Status.OVERDUE || status == Status.NEXT || status == Status.UPCOMING;
    }
}
