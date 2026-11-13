// Linha de preço de um item do contrato, procurada na tabela de preços da técnica (a do
// GET /api/techniques/{id}/prices) pelo que o usuário escolheu. Não calcula nada: só acha a linha
// que bate com as escolhas, e o backend confere de novo ao salvar. Ver "Contract e ContractItem" e
// "Plano do item do Grupo" no domain-model. Função pura: o formulário chama a cada render.
//
// - item por duração: linha do plano do contrato com a duração escolhida;
// - Pilates em Grupo: linha do plano do Grupo com a frequência semanal escolhida. Com plano de
//   mensalidade, o plano do Grupo é o próprio plano do contrato; com pacote, o escolhido no item.
//   Se ele ainda está entre as opções da prévia, o formulário confere depois (as opções vêm da
//   prévia, que recebe os itens resolvidos aqui).
//
// Devolve { line, groupPlanId, issue }: issue é o aviso do item (sem linha não dá para salvar)

export function resolveItemLine(item, { plan, prices, technique }) {
  if (!item?.techniqueId || !plan || !prices) return { line: null, groupPlanId: null, issue: null };

  if (item.pricingModel === "DURATION_BASED") {
    if (plan.type === "MONTHLY_FEE") {
      const group = technique?.pricingGroupName ?? "outro grupo";
      return {
        line: null,
        groupPlanId: null,
        issue: `${technique?.name ?? "Esta técnica"} é do grupo ${group}: as mensalidades são só para o Pilates em Grupo.`,
      };
    }
    if (!item.durationMinutes) return { line: null, groupPlanId: null, issue: null };

    const line = prices.find(
      (price) => price.planId === plan.id && String(price.durationMinutes) === item.durationMinutes,
    );
    const issue = line ? null : `Sem preço de ${item.durationMinutes} min no plano ${plan.name}.`;
    return { line: line ?? null, groupPlanId: null, issue };
  }

  // Pilates em Grupo
  const groupPlanId = plan.type === "MONTHLY_FEE" ? String(plan.id) : item.groupPlanId || null;
  if (!groupPlanId || !item.weeklyFrequency) return { line: null, groupPlanId, issue: null };

  const line = prices.find(
    (price) => String(price.planId) === groupPlanId && String(price.weeklyFrequency) === item.weeklyFrequency,
  );
  const issue = line ? null : `Sem preço de ${item.weeklyFrequency}x por semana neste plano do Grupo.`;
  return { line: line ?? null, groupPlanId, issue };
}
