package br.com.senai.sublime_app.contract.enums;

// Status do contrato na resposta, calculado pelo backend (ContractEntity.statusOn): depende da
// vigência e, depois do fim, do saldo da conta, que é do billing. Ver "Status do contrato" em
// Contract, no domain-model
public enum ContractStatus {

    // ativo, dentro da vigência (o endDate ainda não passou)
    CURRENT,

    // passou do fim com pendência: o saldo da conta é diferente de zero (dívida ou crédito)
    EXPIRED,

    // passou do fim com o saldo zerado
    CLOSED,

    // inativado à mão (o DELETE)
    CANCELED,

    // a versão que um aditivo substituiu (só no histórico)
    SUPERSEDED
}
