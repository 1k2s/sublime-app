package br.com.senai.sublime_app.billing.domain;

// A situação de uma conta (um contrato ou o avulso) e do paciente na lista, nesta prioridade:
// atrasado (alguma parcela vencida em aberto) → devendo (saldo devedor) → com crédito (saldo
// credor) → em dia (zerada). Ver "Payment" no domain-model.
public enum AccountStatus {

    LATE,
    OWING,
    CREDIT,
    UP_TO_DATE
}
