import { formatPriceAxis } from "@/domain/pricing/display/priceAxis";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatAmendedInstallments, formatInstallments } from "./formatInstallments";
import { getPaymentMethodLabel } from "./paymentMethods";
import { getSessionFrequencyLabel } from "./sessionFrequencies";

// As "Alterações" do aditivo: o formulário (e a prévia do aditivo) comparado com a versão atual,
// só para MOSTRAR. Função pura. Os valores (fim, total, parcelas, sessões) vêm da API; a regra que
// vale é a do backend, que recusa o aditivo sem alteração (ver "Versionamento (aditivos)" em
// Contract, no domain-model). hasChanges segue o que o backend conta como alteração: item incluído
// ou removido; outra técnica, linha de preço ou frequência num item mantido; outro beneficiário;
// outra forma de pagamento; outras parcelas; prorrogação pedida; 1º vencimento digitado. Num caso
// raro o backend ainda recusa (o 1º vencimento digitado igual ao sugerido, no mesmo dia): a
// mensagem dele aparece ao confirmar.
//
// contract: a versão atual (com beneficiaryName). form: { beneficiary, paymentMethod,
// installments, endDate, firstDueDate }, os valores do formulário. formItems: os itens resolvidos do
// useContractDraft. preview: a resposta da prévia do aditivo (ou null). balances: o saldo de
// sessões dos itens (do session-balance).
// Devolve { hasChanges, lines }: cada linha { key, label, text } ou { key, label, before, after }
export function listAmendmentChanges({ contract, form, formItems, preview, balances = [] }) {
  const lockedByGroupId = new Map(contract.items.map((item) => [item.pricingGroupId, item]));
  const formGroupIds = new Set(formItems.map((formItem) => formItem.technique?.pricingGroupId).filter(Boolean));
  const lines = [];

  formItems.forEach((formItem, index) => {
    const groupId = formItem.technique?.pricingGroupId;
    const locked = groupId ? lockedByGroupId.get(groupId) : null;
    if (formItem.technique && !locked) {
      lines.push({ key: `added-${index}`, label: "Adicionado:", text: describeAddedItem(formItem) });
    } else if (locked && hasItemChanged(formItem, locked)) {
      const techniqueChanged = Number(formItem.item.techniqueId) !== locked.techniqueId;
      lines.push({
        key: `changed-${index}`,
        label: "Alterado:",
        text: `${formItem.technique.name}, de ${describeLockedItem(locked, techniqueChanged)} para ${describeFormItem(formItem, techniqueChanged)}`,
      });
    }
  });

  contract.items
    .filter((item) => !formGroupIds.has(item.pricingGroupId))
    .forEach((item) => {
      const balance = balances.find((entry) => entry.pricingGroupId === item.pricingGroupId);
      const remaining = balance ? ` (restavam ${balance.remainingSessions} sessões)` : "";
      lines.push({ key: `removed-${item.id}`, label: "Removido:", text: `${item.techniqueName}${remaining}` });
    });

  const beneficiaryChanged = String(form.beneficiary?.id ?? "") !== String(contract.beneficiaryId ?? "");
  if (beneficiaryChanged) {
    lines.push({
      key: "beneficiary",
      label: "Novo beneficiário:",
      before: contract.beneficiaryName ?? "Nenhum",
      after: form.beneficiary?.name ?? "Nenhum",
    });
  }

  const paymentChanged = form.paymentMethod !== contract.paymentMethod;
  if (paymentChanged) {
    lines.push({
      key: "payment",
      label: "Nova forma de pagamento:",
      before: getPaymentMethodLabel(contract.paymentMethod),
      after: getPaymentMethodLabel(form.paymentMethod),
    });
  }

  if (preview && preview.endDate !== contract.endDate) {
    lines.push({ key: "end", label: "Novo fim:", before: formatDate(contract.endDate), after: formatDate(preview.endDate) });
  }
  if (preview && Number(preview.totalValue) !== Number(contract.totalValue)) {
    lines.push({
      key: "total",
      label: "Novo total:",
      before: formatCurrency(contract.totalValue),
      after: formatCurrency(preview.totalValue),
    });
  }
  // As novas parcelas: o valor parcelado (o total − o já pago) e os vencimentos
  if (preview && (haveInstallmentsChanged(preview, contract) || form.firstDueDate)) {
    lines.push({
      key: "installments",
      label: "Novas parcelas:",
      before: formatInstallments(contract),
      after: formatAmendedInstallments(preview),
    });
  }

  const installmentsChanged = Number(form.installments) !== contract.installments;
  const hasChanges =
    Boolean(form.endDate) ||
    Boolean(form.firstDueDate) ||
    beneficiaryChanged ||
    paymentChanged ||
    installmentsChanged ||
    haveItemsChanged(formItems, lockedByGroupId, contract.items.length);

  return { hasChanges, lines };
}

// Itens: outra quantidade, um grupo novo, ou um item mantido com outra técnica, linha ou
// frequência. Item ainda sem técnica ou sem linha conta como alteração (o formulário acusa o erro)
function haveItemsChanged(formItems, lockedByGroupId, lockedCount) {
  if (formItems.length !== lockedCount) return true;
  return formItems.some((formItem) => {
    const locked = lockedByGroupId.get(formItem.technique?.pricingGroupId);
    return !locked || hasItemChanged(formItem, locked);
  });
}

function hasItemChanged(formItem, locked) {
  if (!formItem.line) return true;
  const lockedLineId = locked.sessionDurationPriceId ?? locked.sessionFrequencyPriceId;
  const isSameLineType = (locked.sessionDurationPriceId != null) === (formItem.item.pricingModel === "DURATION_BASED");
  return (
    Number(formItem.item.techniqueId) !== locked.techniqueId ||
    !isSameLineType ||
    formItem.line.id !== lockedLineId ||
    (formItem.item.frequency || null) !== (locked.frequency ?? null)
  );
}

function haveInstallmentsChanged(preview, contract) {
  return (
    preview.installments !== contract.installments ||
    Number(preview.firstInstallmentValue) !== Number(contract.firstInstallmentValue) ||
    Number(preview.installmentValue) !== Number(contract.installmentValue)
  );
}

// "Liberação miofascial (30 min, 1x por semana, 8 sessões, R$ 640,00)"
function describeAddedItem({ technique, line, item, itemValue }) {
  const isGroup = item.pricingModel === "FREQUENCY_BASED";
  const parts = [
    line && formatPriceAxis(line),
    item.frequency && getSessionFrequencyLabel(item.frequency),
    itemValue && `${itemValue.sessionCount} ${isGroup ? "aulas" : "sessões"}`,
    itemValue && formatCurrency(itemValue.totalValue),
  ].filter(Boolean);
  return parts.length > 0 ? `${technique.name} (${parts.join(", ")})` : technique.name;
}

// "50 min · 2x por semana" ou, no Grupo, "Trimestral · 2x por semana" (com a técnica na frente
// quando ela mudou)
function describeLockedItem(locked, withTechnique) {
  const isGroup = locked.sessionFrequencyPriceId != null;
  return [
    withTechnique && locked.techniqueName,
    isGroup && locked.planName,
    formatPriceAxis(locked),
    locked.frequency && getSessionFrequencyLabel(locked.frequency),
  ]
    .filter(Boolean)
    .join(" · ");
}

function describeFormItem({ technique, line, planName, item }, withTechnique) {
  const isGroup = item.pricingModel === "FREQUENCY_BASED";
  return [
    withTechnique && technique.name,
    isGroup && planName,
    line && formatPriceAxis(line),
    item.frequency && getSessionFrequencyLabel(item.frequency),
  ]
    .filter(Boolean)
    .join(" · ");
}
