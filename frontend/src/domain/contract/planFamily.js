// Plano Familiar x plano base, só para a exibição do cadastro (o alternador Individual |
// Familiar). A API não tem um campo para isso: o Familiar se distingue pelo nome ("Evolução
// Familiar"), como na tabela de planos do domain-model. Um plano renomeado sem o sufixo só
// aparece no modo Individual, sem quebrar nada. Funções puras

const FAMILY_SUFFIX = " Familiar";

export function isFamilyPlan(plan) {
  return plan.name.endsWith(FAMILY_SUFFIX);
}

// "Evolução Familiar" -> "Evolução"
function getPlanBaseName(plan) {
  return isFamilyPlan(plan) ? plan.name.slice(0, -FAMILY_SUFFIX.length) : plan.name;
}

// A versão do plano no outro modo (mesmo tipo e mesmo nome base), ou null se não existe
// (ex: o Essencial não tem Familiar)
export function findPlanCounterpart(plan, plans, family) {
  return (
    plans.find(
      (candidate) =>
        candidate.type === plan.type &&
        isFamilyPlan(candidate) === family &&
        getPlanBaseName(candidate) === getPlanBaseName(plan),
    ) ?? null
  );
}
