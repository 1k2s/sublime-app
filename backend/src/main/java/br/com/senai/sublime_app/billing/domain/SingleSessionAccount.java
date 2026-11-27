package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.util.List;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.pricing.domain.Money;

// A conta do avulso do paciente (ver "Payment" no domain-model): os atendimentos sem contrato e
// os pagamentos sem contrato. É separada das contas dos contratos (o crédito de uma não passa para
// a outra). Sem parcelas: nunca fica atrasada.
public final class SingleSessionAccount {

    private final Money due;
    private final Money paid;
    private final BigDecimal balance;

    private SingleSessionAccount(Money due, Money paid) {
        this.due = due;
        this.paid = paid;
        this.balance = paid.amount().subtract(due.amount());
    }

    // consumption = os atendimentos ativos sem contrato do paciente; paid = o bruto dos pagamentos
    // ativos sem contrato
    public static SingleSessionAccount of(List<ConsultationConsumption> consumption, Money paid) {
        return new SingleSessionAccount(ConsultationConsumption.sumConsumed(consumption), paid);
    }

    // Tem avulso: algum atendimento ou pagamento sem contrato
    public boolean exists() {
        return due.isPositive() || paid.isPositive();
    }

    public List<AccountWarning> warnings() {
        return balance.signum() < 0
                ? List.of(new AccountWarning(AccountWarning.Type.SINGLE_SESSION_OPEN, null, Money.of(balance.negate()),
                        null, null, null))
                : List.of();
    }

    public List<PaymentSuggestion> suggestions() {
        List<PaymentSuggestion> list = PaymentSuggestion.newList();
        if (balance.signum() < 0) {
            PaymentSuggestion.addIfUseful(list, PaymentSuggestion.Type.SINGLE_SESSION_DUE, Money.of(balance.negate()));
        }
        return List.copyOf(list);
    }

    public AccountStatus status() {
        if (balance.signum() < 0) {
            return AccountStatus.OWING;
        }
        return balance.signum() > 0 ? AccountStatus.CREDIT : AccountStatus.UP_TO_DATE;
    }

    public Money due() {
        return due;
    }

    public Money paid() {
        return paid;
    }

    // pago − devido: positivo = saldo credor; negativo = saldo devedor
    public BigDecimal balance() {
        return balance;
    }
}
