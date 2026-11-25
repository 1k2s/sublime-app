// Linha de preço de um item do contrato, procurada na tabela de preços da técnica (a do
// GET /api/techniques/{id}/prices) pelo que o usuário escolheu. Não calcula nada: só acha a linha
// que bate com as escolhas, e o backend confere de novo ao salvar. Ver "Contract e ContractItem" e
// "Plano do item do Grupo" no domain-model. Função pura: o formulário chama a cada render.
//
// - item por duração: linha do plano do contrato com a duração escolhida; num plano de
//   mensalidade, linha do pacote equivalente (de-para, ex: Trimestral → Evolução);
// - Pilates em Grupo: linha do plano do Grupo com a frequência semanal escolhida. Com plano de
//   mensalidade, o plano do Grupo é o próprio plano do contrato; com pacote, o escolhido no item.
//   Se ele ainda está entre as opções da prévia, o formulário confere depois (as opções vêm da
//   prévia, que recebe os itens resolvidos aqui).
//
// Devolve { line, groupPlanId, issue }: issue é o aviso do item (sem linha não dá para salvar)

export function resolveItemLine(item, { plan, prices, technique }) {
  if (!item?.techniqueId || !plan || !prices) return { line: null, groupPlanId: null, issue: null };

  if (item.pricingModel === "DURATION_BASED") {
    // Plano de mensalidade: o item por duração usa o pacote equivalente (de-para)
    const durationPlanId = plan.type === "MONTHLY_FEE" ? plan.equivalentPlanId : plan.id;
    const durationPlanName = plan.type === "MONTHLY_FEE" ? plan.equivalentPlanName : plan.name;
    if (!durationPlanId) {
      return {
        line: null,
        groupPlanId: null,
        issue: `O plano ${plan.name} não tem plano equivalente definido (de-para): sem preço para ${technique?.name ?? "esta técnica"}.`,
      };
    }
    if (!item.durationMinutes) return { line: null, groupPlanId: null, issue: null };

    const line = prices.find(
      (price) => price.planId === durationPlanId && String(price.durationMinutes) === item.durationMinutes,
    );
    const issue = line ? null : `Sem preço de ${item.durationMinutes} min no plano ${durationPlanName}.`;
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

// No aditivo: o item que mantém as escolhas do item do mesmo grupo na versão atual (a mesma
// duração, ou o mesmo plano do Grupo e as mesmas vezes por semana) fica com a linha TRAVADA nele,
// mesmo que um reajuste tenha criado outra (o contrato mantém o valor combinado; ver "Contract e
// ContractItem" no domain-model). Mudou a escolha: a linha vigente, pelo resolveItemLine.
// lockedItem: o item da versão atual com o mesmo grupo (do ContractItemResponseDTO), ou null.
// Devolve o mesmo { line, groupPlanId, issue }, com isLocked
export function resolveAmendedItemLine(item, context, lockedItem) {
  const locked = lockedLineOf(item, context.plan, lockedItem);
  if (locked) return { ...locked, issue: null, isLocked: true };
  return { ...resolveItemLine(item, context), isLocked: false };
}

function lockedLineOf(item, plan, lockedItem) {
  if (!lockedItem || !plan) return null;
  if (item?.pricingModel === "DURATION_BASED") {
    const isSame = lockedItem.sessionDurationPriceId != null && String(lockedItem.durationMinutes) === item.durationMinutes;
    if (!isSame) return null;
    return {
      line: {
        id: lockedItem.sessionDurationPriceId,
        planId: lockedItem.planId,
        durationMinutes: lockedItem.durationMinutes,
        sessionValue: lockedItem.sessionValue,
      },
      groupPlanId: null,
    };
  }
  const groupPlanId = plan.type === "MONTHLY_FEE" ? String(plan.id) : item?.groupPlanId || null;
  const isSame =
    lockedItem.sessionFrequencyPriceId != null &&
    groupPlanId === String(lockedItem.planId) &&
    String(lockedItem.weeklyFrequency) === item?.weeklyFrequency;
  if (!isSame) return null;
  return {
    line: {
      id: lockedItem.sessionFrequencyPriceId,
      planId: lockedItem.planId,
      weeklyFrequency: lockedItem.weeklyFrequency,
      sessionValue: lockedItem.sessionValue,
    },
    groupPlanId,
  };
}
