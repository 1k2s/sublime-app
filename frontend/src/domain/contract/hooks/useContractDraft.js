import { useWatch } from "react-hook-form";
import { useQueries } from "@tanstack/react-query";
import { getTechniquePrices } from "@/domain/pricing/api/pricingApi";
import { useContractPreview } from "../api/useContracts";
import { resolveItemLine } from "../utils/resolveItemLine";

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
// prévia do backend. plans, techniques e pricingGroups: o catálogo, carregado pela page
export function useContractDraft({ control, plans, techniques, pricingGroups }) {
  const values = useWatch({ control });
  const items = values.items ?? [];

  // Planos contratáveis: ativos e sem o Avulso (que nunca entra em contrato)
  const contractPlans = plans.filter((plan) => plan.active && plan.type !== "SINGLE_SESSION");
  const plan = plans.find((item) => String(item.id) === values.planId) ?? null;

  // Técnicas ativas, com o modelo do grupo (diz os campos do item). Plano de mensalidade só tem
  // o Pilates em Grupo
  const modelByGroupId = new Map(pricingGroups.map((group) => [group.id, group.pricingModel]));
  const catalogTechniques = techniques
    .filter((technique) => technique.active)
    .map((technique) => ({ ...technique, pricingModel: modelByGroupId.get(technique.pricingGroupId) }));
  const techniqueById = new Map(catalogTechniques.map((technique) => [String(technique.id), technique]));
  const allowedTechniques =
    plan?.type === "MONTHLY_FEE"
      ? catalogTechniques.filter((technique) => technique.pricingModel === "FREQUENCY_BASED")
      : catalogTechniques;

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

  // Linhas de preço de cada técnica escolhida, numa leva só (mesmo cache do useTechniquePrices)
  const techniqueIds = [...new Set(items.map((item) => item?.techniqueId).filter(Boolean))];
  const pricesQueries = useQueries({
    queries: techniqueIds.map((techniqueId) => ({
      queryKey: ["prices", Number(techniqueId)],
      queryFn: () => getTechniquePrices(Number(techniqueId)),
    })),
  });
  const pricesByTechniqueId = new Map(techniqueIds.map((techniqueId, index) => [techniqueId, pricesQueries[index]]));

  // A linha de preço de cada item, pelo plano atual (ou o aviso de por que não há linha)
  const resolutions = items.map((item) =>
    resolveItemLine(item, {
      plan,
      prices: pricesByTechniqueId.get(item?.techniqueId)?.data?.prices,
      technique: techniqueById.get(item?.techniqueId),
    }),
  );

  // Prévia do contrato (fim, opções do Grupo, valores, total e parcelas), com as contas do
  // backend: os itens com linha, no formato do cadastro, e as parcelas escolhidas (vazio = o
  // máximo; acima do máximo, a prévia aplica o máximo)
  const resolvedItems = items.flatMap((item, index) =>
    resolutions[index].line ? [toItemRequest(item, resolutions[index].line)] : [],
  );
  const previewQuery = useContractPreview({
    planId: values.planId ? Number(values.planId) : null,
    startDate: values.startDate,
    items: resolvedItems,
    installments: Number(values.installments) || null,
  });
  const preview = {
    isEnabled: Boolean(values.planId && values.startDate),
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
  // Conferido depois da prévia: as opções vêm dela
  function groupPlanIssue(item) {
    const isStale =
      item?.pricingModel === "FREQUENCY_BASED" &&
      plan?.type === "PACKAGE" &&
      item.groupPlanId &&
      previewQuery.data &&
      !groupPlanOptions.some((option) => String(option.id) === item.groupPlanId);
    return isStale ? "As opções do plano do Grupo mudaram: escolha de novo." : null;
  }
  const issues = items.map((item, index) => resolutions[index].issue ?? groupPlanIssue(item));

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
  function itemStateFor(index) {
    const pricesQuery = pricesByTechniqueId.get(items[index]?.techniqueId);
    const line = resolutions[index]?.line ?? null;
    return {
      prices: pricesQuery?.data?.prices,
      isPricesLoading: pricesQuery?.isLoading,
      isPricesError: pricesQuery?.isError,
      resolution: { line, issue: issues[index] ?? null },
      itemValue: previewItemOf(items[index] ?? {}, line),
    };
  }

  // O pedido no formato do ContractRequestDTO (o fim e os valores não vão: são calculados), ou o
  // erro que impede salvar. Item sem linha no plano não vai: o aviso dele diz o motivo. As
  // parcelas são as que a prévia aplicou (as escolhidas, ou o máximo): por isso a prévia precisa
  // estar em dia
  function buildRequest(formValues) {
    if (issues.some(Boolean) || resolutions.some((resolution) => !resolution.line)) {
      return { error: { field: "items", message: "Há itens sem preço no plano escolhido: confira os avisos." } };
    }
    if (!previewQuery.data || previewQuery.isPlaceholderData || previewQuery.isError) {
      return {
        error: { field: "installments", message: "Aguarde o cálculo do contrato (fim e valor, no rodapé)." },
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
        items: formValues.items.map((item, index) => toItemRequest(item, resolutions[index].line)),
      },
    };
  }

  return {
    contractPlans,
    plan,
    techniqueOptionsFor,
    canAddItem,
    itemStateFor,
    groupPlanOptions,
    suggestedGroupPlanId: previewQuery.data?.suggestedGroupPlanId,
    preview,
    buildRequest,
  };
}
