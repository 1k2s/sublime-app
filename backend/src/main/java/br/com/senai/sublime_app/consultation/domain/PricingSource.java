package br.com.senai.sublime_app.consultation.domain;

// Como o preço do atendimento foi resolvido (casos 1, 2 e 3). Calculado pela
// ConsultationEntity no lançamento, nunca vem do request. Ver domain-model.md,
// Consultation, "Resolução de preço" (e ADR 0017).
public enum PricingSource {

    // Caso 1: o contrato escolhido cobre o grupo da técnica — preço do item
    CONTRACT_ITEM,

    // Caso 2: o contrato escolhido não cobre o grupo — preço pelo de-para do plano dele
    EQUIVALENT_PLAN,

    // Caso 3: paciente sem contrato vinculado — preço do plano Avulso
    SINGLE_SESSION;

    // Só o preço do item do contrato consome uma sessão do saldo
    public boolean debitsContractItem() {
        return this == CONTRACT_ITEM;
    }
}
