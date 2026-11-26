import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatInstallments } from "./formatInstallments";
import { getPaymentMethodLabel } from "./paymentMethods";

// O que mudou de uma versão do contrato para a anterior a ela (ex: do Original para o Aditivo 1),
// para MARCAR no histórico. Função pura, sobre duas respostas da API (com beneficiaryName). Os
// itens seguem o mesmo critério do backend para "aditivo sem alteração" (ver "Versionamento
// (aditivos)" em Contract, no domain-model): grupo novo, grupo que saiu, ou outra técnica, linha
// de preço ou frequência no mesmo grupo. O início e o "Criado em" não entram (o início nunca muda).
//
// Devolve { fields, itemStatus, removedItems }:
// - fields: { campo: "valor na anterior" } só dos que mudaram (paymentMethod, endDate,
//   beneficiary, totalValue, installments);
// - itemStatus: Map(id do item → "added" | "changed");
// - removedItems: os itens da anterior que saíram.
// previous null (a versão original): nada marcado
export function getVersionChanges(version, previous) {
  if (!previous) return { fields: {}, itemStatus: new Map(), removedItems: [] };

  const fields = {};
  if (version.paymentMethod !== previous.paymentMethod) {
    fields.paymentMethod = getPaymentMethodLabel(previous.paymentMethod);
  }
  if (version.endDate !== previous.endDate) fields.endDate = formatDate(previous.endDate);
  if (version.beneficiaryId !== previous.beneficiaryId) fields.beneficiary = previous.beneficiaryName ?? "Nenhum";
  if (Number(version.totalValue) !== Number(previous.totalValue)) fields.totalValue = formatCurrency(previous.totalValue);
  if (formatInstallments(version) !== formatInstallments(previous)) fields.installments = formatInstallments(previous);

  const previousByGroupId = new Map(previous.items.map((item) => [item.pricingGroupId, item]));
  const groupIds = new Set(version.items.map((item) => item.pricingGroupId));
  const itemStatus = new Map();
  version.items.forEach((item) => {
    const previousItem = previousByGroupId.get(item.pricingGroupId);
    if (!previousItem) itemStatus.set(item.id, "added");
    else if (hasItemChanged(item, previousItem)) itemStatus.set(item.id, "changed");
  });
  const removedItems = previous.items.filter((item) => !groupIds.has(item.pricingGroupId));

  return { fields, itemStatus, removedItems };
}

function hasItemChanged(item, previousItem) {
  return (
    item.techniqueId !== previousItem.techniqueId ||
    item.sessionDurationPriceId !== previousItem.sessionDurationPriceId ||
    item.sessionFrequencyPriceId !== previousItem.sessionFrequencyPriceId ||
    (item.frequency ?? null) !== (previousItem.frequency ?? null)
  );
}
