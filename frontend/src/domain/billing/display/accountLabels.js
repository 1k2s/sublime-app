import { getContractStatusByValue } from "@/domain/contract/display/contractStatus";
import { getSessionFrequencyLabel } from "@/domain/contract/display/sessionFrequencies";

// Rótulos do contrato e dos itens na conta, com os campos que a API manda

// "Evolução · Aditivo 1" ou "Evolução" (a versão original). contract: { planName, amendmentNumber }
export function formatContractLabel({ planName, amendmentNumber }) {
  return amendmentNumber > 0 ? `${planName} · Aditivo ${amendmentNumber}` : planName;
}

// "Evolução · Aditivo 1 (vencido)": o rótulo com o status, quando não é o vigente (status: o da
// API, ContractStatus)
export function formatContractLabelWithStatus(contract, status) {
  const label = formatContractLabel(contract);
  return status && status !== "CURRENT" ? `${label} (${getContractStatusByValue(status).label.toLowerCase()})` : label;
}

// "Evolução · Aditivo 1, Trimestral": os contratos de uma linha da lista; sem contrato, "Avulso"
export function formatContractLabels(contracts = []) {
  return contracts.length > 0
    ? contracts.map((contract) => formatContractLabelWithStatus(contract, contract.status)).join(", ")
    : "Avulso";
}

// "RPG · 60 min, 1x por semana" ou "Pilates em Grupo · 2x por semana"
export function formatItemLabel(item) {
  if (item.groupItem) return `${item.techniqueName} · ${item.weeklyFrequency}x por semana`;
  return `${item.techniqueName} · ${item.durationMinutes} min, ${getSessionFrequencyLabel(item.frequency)}`;
}
