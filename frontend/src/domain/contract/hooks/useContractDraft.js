import { useWatch } from "react-hook-form";
import { useQueries } from "@tanstack/react-query";
import { getTechniquePrices } from "@/domain/pricing/api/pricingApi";
import { useAmendmentPreview, useContractPreview } from "../api/useContracts";
import { resolveAmendedItemLine, resolveItemLine } from "../utils/resolveItemLine";

// O item no formato do ContractItemRequestDTO, com a linha achada pelo resolveItemLine: o modelo
// do grupo diz qual das duas vai (exclusive arc)
function toItemRequest(item, line) {
  const techniqueId = Number(item.techniqueId);
  if (item.pricingModel === "DURATION_BASED") {
    return { techniqueId, sessionDurationPriceId: line.id, frequency: item.frequency };
  }
  return { techniqueId, sessionFrequencyPriceId: line.id };
}

// Chave da linha (as duas tabelas de preço têm ids próprios): liga o item ao valor da prévia
function lineKey(durationPriceId, frequencyPriceId) {
  return durationPriceId != null ? `d${durationPriceId}` : `f${frequencyPriceId}`;
}

// O contrato em montagem: lê o formulário (control) e devolve o que a tela mostra. Não guarda
// estado próprio: tudo sai dos valores do formulário e das respostas da API. O item guarda as
// escolhas (duração, frequência), e a linha de preço sai do plano: trocar o plano mantém os itens
// e atualiza os valores. O fim, os valores, as parcelas e as opções do plano do Grupo vêm da
// prévia do backend. plans, techniques e pricingGroups: o catálogo, carregado pela page.
// amendedContract: no aditivo, a versão atual (a prévia passa a ser a do aditivo, e o item que mantém
// as escolhas fica com a linha travada nela); null no cadastro
export function useContractDraft({ control, plans, techniques, pricingGroups, amendedContract = null }) {
  const values = useWatch({ control });
  const items = values.items ?? [];

  // Planos contratáveis: ativos e sem o Avulso (que nunca entra em contrato)
  const contractPlans = plans.filter((plan) => plan.active && plan.type !== "SINGLE_SESSION");
  const plan = plans.find((item) => String(item.id) === values.planId) ?? null;

  // No aditivo, os itens da versão atual, pelo grupo (contrato + grupo = item)
  const lockedItems = amendedContract?.items ?? [];
  const lockedItemByGroupId = new Map(lockedItems.map((item) => [item.pricingGroupId, item]));
  const keptTechniqueIds = new Set(lockedItems.map((item) => item.techniqueId));

  // Técnicas ativas, com o modelo do grupo (diz os campos do item). Qualquer plano aceita
  // qualquer técnica: no plano de mensalidade, o item por duração usa o pacote equivalente
  // (resolveItemLine). No aditivo, a técnica de um item da versão atual continua mesmo inativa
  // (o backend aceita a mesma do item anterior)
  const modelByGroupId = new Map(pricingGroups.map((group) => [group.id, group.pricingModel]));
  const catalogTechniques = techniques
    .filter((technique) => technique.active || keptTechniqueIds.has(technique.id))
    .map((technique) => ({ ...technique, pricingModel: modelByGroupId.get(technique.pricingGroupId) }));
  const techniqueById = new Map(catalogTechniques.map((technique) => [String(technique.id), technique]));
  const allowedTechniques = catalogTechniques;

  // Um item por grupo: cada item não oferece as técnicas dos grupos usados nos outros. A técnica
  // já escolhida continua na lista do próprio item (o aviso dele explica se não serve ao plano).
  // index -1 = um item novo (todos os grupos usados contam)
  function techniqueOptionsFor(index) {
    const usedGroupIds = new Set(
      items
        .filter((_, itemIndex) => itemIndex !== index)
        .map((item) => techniqueById.get(item?.techniqueId)?.pricingGroupId)
        .filter(Boolean),
    );
    const current = techniqueById.get(items[index]?.techniqueId);
    const options = allowedTechniques.filter((technique) => !usedGroupIds.has(technique.pricingGroupId));
    return current && !options.includes(current) ? [current, ...options] : options;
  }
  const canAddItem = techniqueOptionsFor(-1).length > 0;

  // Linhas de preço de cada técnica escolhida, numa leva só. A chave fica sob ["prices"]: as
  // mutations de preço do pricing a invalidam
  const techniqueIds = [...new Set(items.map((item) => item?.techniqueId).filter(Boolean))];
  const pricesQueries = useQueries({
    queries: techniqueIds.map((techniqueId) => ({
      queryKey: ["prices", Number(techniqueId)],
      queryFn: () => getTechniquePrices(Number(techniqueId)),
    })),
  });
  const pricesByTechniqueId = new Map(techniqueIds.map((techniqueId, index) => [techniqueId, pricesQueries[index]]));

  // O item da versão atual com o mesmo grupo do item (só no aditivo)
  function lockedItemOf(item) {
    const technique = techniqueById.get(item?.techniqueId);
    return technique ? (lockedItemByGroupId.get(technique.pricingGroupId) ?? null) : null;
  }

  // A linha de preço de cada item, pelo plano atual (ou o aviso de por que não há linha). No
  // aditivo, o item que manteve as escolhas fica com a linha travada
  const resolutions = items.map((item) => {
    const context = {
      plan,
      prices: pricesByTechniqueId.get(item?.techniqueId)?.data?.prices,
      technique: techniqueById.get(item?.techniqueId),
    };
    return amendedContract ? resolveAmendedItemLine(item, context, lockedItemOf(item)) : resolveItemLine(item, context);
  });

  // Prévia do contrato (fim, opções do Grupo, valores, total e parcelas), com as contas do
  // backend: os itens com linha, no formato do cadastro, e as parcelas escolhidas (vazio = o
  // máximo; acima do máximo, a prévia aplica o máximo). No aditivo, a prévia do aditivo (o fim sai
  // da versão atual e da prorrogação, não do início). As duas são chamadas (hooks não podem ser
  // condicionais); só a do modo em uso busca
  const resolvedItems = items.flatMap((item, index) =>
    resolutions[index].line ? [toItemRequest(item, resolutions[index].line)] : [],
  );
  const installments = Number(values.installments) || null;
  const creationPreviewQuery = useContractPreview(
    {
      planId: values.planId ? Number(values.planId) : null,
      startDate: values.startDate,
      items: resolvedItems,
      installments,
    },
    { enabled: !amendedContract },
  );
  const amendmentPreviewQuery = useAmendmentPreview(amendedContract?.id ?? null, {
    endDate: values.endDate || null,
    items: resolvedItems,
    installments,
  });
  const previewQuery = amendedContract ? amendmentPreviewQuery : creationPreviewQuery;
  const preview = {
    isEnabled: Boolean(amendedContract) || Boolean(values.planId && values.startDate),
    isLoading: previewQuery.isLoading,
    isError: previewQuery.isError,
    error: previewQuery.error,
    data: previewQuery.data,
  };
  const groupPlanOptions = previewQuery.data?.groupPlanOptions ?? [];
  const previewItemByLine = new Map(
    (previewQuery.data?.items ?? []).map((previewItem) => [
      lineKey(previewItem.sessionDurationPriceId, previewItem.sessionFrequencyPriceId),
      previewItem,
    ]),
  );

  // O plano do Grupo escolhido saiu das opções (a prévia mudou com o plano ou as frequências).
  // Conferido depois da prévia: as opções vêm dela. No aditivo, o item do Grupo sem mudança é
  // aceito como está (o backend não o reconfere), então a linha travada não tem aviso
  function groupPlanIssue(item, resolution) {
    if (resolution.isLocked) return null;
    const isStale =
      item?.pricingModel === "FREQUENCY_BASED" &&
      plan?.type === "PACKAGE" &&
      item.groupPlanId &&
      previewQuery.data &&
      !groupPlanOptions.some((option) => String(option.id) === item.groupPlanId);
    return isStale ? "As opções do plano do Grupo mudaram: escolha de novo." : null;
  }
  const issues = items.map((item, index) => resolutions[index].issue ?? groupPlanIssue(item, resolutions[index]));

  // Sessões e total de cada item, da prévia
  function previewItemOf(item, line) {
    if (!line) return null;
    const isDuration = item.pricingModel === "DURATION_BASED";
    return previewItemByLine.get(lineKey(isDuration ? line.id : null, isDuration ? null : line.id)) ?? null;
  }

  // O que o item da posição index mostra: as linhas de preço da técnica, a linha achada com o
  // aviso, e as sessões e o total da prévia. Logo após o "Adicionar item", o fields do formulário
  // já tem o item novo, mas os valores do useWatch (items e resolutions) só chegam no render
  // seguinte: daí os ?. no index
  // No aditivo, também o item da versão atual com o mesmo grupo (lockedItem: o saldo de sessões
  // é dele) e se o item é de um grupo novo (isNew)
  function itemStateFor(index) {
    const pricesQuery = pricesByTechniqueId.get(items[index]?.techniqueId);
    const line = resolutions[index]?.line ?? null;
    const lockedItem = amendedContract ? lockedItemOf(items[index]) : null;
    return {
      prices: pricesQuery?.data?.prices,
      isPricesLoading: pricesQuery?.isLoading,
      isPricesError: pricesQuery?.isError,
      resolution: { line, issue: issues[index] ?? null },
      itemValue: previewItemOf(items[index] ?? {}, line),
      lockedItem,
      isNew: Boolean(amendedContract && techniqueById.has(items[index]?.techniqueId) && !lockedItem),
    };
  }

  // Os itens do formulário já resolvidos, para comparar com a versão atual (as "Alterações" do
  // aditivo): o valor do formulário, a técnica, a linha (com o nome do plano dela: no Grupo, é o
  // plano do Grupo) e as sessões e o total da prévia
  const formItems = items.map((item, index) => {
    const line = resolutions[index]?.line ?? null;
    return {
      item: item ?? {},
      technique: techniqueById.get(item?.techniqueId) ?? null,
      line,
      planName: line ? (plans.find((option) => option.id === line.planId)?.name ?? null) : null,
      itemValue: previewItemOf(item ?? {}, line),
    };
  });

  // O pedido no formato do ContractRequestDTO (cadastro) ou do ContractAmendmentRequestDTO
  // (aditivo: o estado completo, sem titular, plano nem início), ou o erro que impede salvar. O fim
  // e os valores não vão: são calculados. Item sem linha no plano não vai: o aviso dele diz o
  // motivo. As parcelas são as que a prévia aplicou (as escolhidas, ou o máximo): por isso a prévia
  // precisa estar em dia
  function buildRequest(formValues) {
    if (issues.some(Boolean) || resolutions.some((resolution) => !resolution.line)) {
      return { error: { field: "items", message: "Há itens sem preço no plano escolhido: confira os avisos." } };
    }
    if (!previewQuery.data || previewQuery.isPlaceholderData || previewQuery.isError) {
      return {
        error: { field: "installments", message: "Aguarde o cálculo do contrato (fim e valor, no rodapé)." },
      };
    }
    const requestItems = formValues.items.map((item, index) => toItemRequest(item, resolutions[index].line));
    if (amendedContract) {
      return {
        request: {
          beneficiaryId: formValues.beneficiary?.id ?? null,
          paymentMethod: formValues.paymentMethod,
          installments: previewQuery.data.installments,
          endDate: formValues.endDate || null,
          items: requestItems,
        },
      };
    }
    return {
      request: {
        patientId: formValues.patient.id,
        beneficiaryId: formValues.beneficiary?.id ?? null,
        planId: Number(formValues.planId),
        startDate: formValues.startDate,
        paymentMethod: formValues.paymentMethod,
        installments: previewQuery.data.installments,
        items: requestItems,
      },
    };
  }

  return {
    contractPlans,
    plan,
    techniqueOptionsFor,
    canAddItem,
    itemStateFor,
    formItems,
    groupPlanOptions,
    suggestedGroupPlanId: previewQuery.data?.suggestedGroupPlanId,
    preview,
    buildRequest,
  };
}
