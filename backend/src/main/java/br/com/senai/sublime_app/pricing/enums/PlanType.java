package br.com.senai.sublime_app.pricing.enums;

// O que o plano é. As regras perguntam o tipo — nunca o nome do plano (ver
// domain-model.md, seção Plan). Regras fixas por tipo moram aqui (princípio 6).
public enum PlanType {

    // Sessão avulsa (Avulso): preço de quem é atendido sem contrato. Nunca entra em contrato.
    SINGLE_SESSION,

    // Pacote de sessões dos grupos por duração (Essencial, Evolução...).
    PACKAGE,

    // Mensalidade do Pilates em Grupo (Mensal, Trimestral...): prazo em semanas, sem sessões.
    MONTHLY_FEE;

    // Avulso e pacote têm quantidade de sessões; mensalidade não.
    // Vale nos dois sentidos: true = campo obrigatório, false = campo precisa vir vazio.
    public boolean hasSessionCount() {
        return this != MONTHLY_FEE;
    }

    // Só a mensalidade tem prazo próprio (os pacotes calculam o prazo pela frequência, no contrato).
    // Vale nos dois sentidos, como o hasSessionCount.
    public boolean hasTermWeeks() {
        return this == MONTHLY_FEE;
    }

    // Planos que podem ter linha na tabela de preço por duração
    public boolean isPricedByDuration() {
        return this == SINGLE_SESSION || this == PACKAGE;
    }

    // Planos que podem ter linha na tabela de preço por frequência (Pilates em Grupo)
    public boolean isPricedByFrequency() {
        return this == MONTHLY_FEE;
    }

    // Se o plano pode ter linha de preço num grupo com este modelo de preço
    public boolean isPricedBy(PricingModel pricingModel) {
        return pricingModel == PricingModel.DURATION_BASED ? isPricedByDuration() : isPricedByFrequency();
    }

    // Avulso é só o preço de quem é atendido sem contrato: nunca entra em contrato
    public boolean isContractable() {
        return this != SINGLE_SESSION;
    }

    // De-para: liga pacote e mensalidade (as duas famílias). Avulso não tem equivalente.
    public boolean isEquivalentFamilyOf(PlanType other) {
        return (this == PACKAGE && other == MONTHLY_FEE) || (this == MONTHLY_FEE && other == PACKAGE);
    }
}
