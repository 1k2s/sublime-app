import { formatCurrency } from "@/shared/utils/money";

// Situação da conta e saldo, como a API resolve (status e balance), sem conta no front. Ver
// "Payment" no domain-model. badge: a variante do Badge
export const ACCOUNT_STATUS = {
  LATE: { value: "LATE", label: "Atrasado", badge: "inactive" },
  OWING: { value: "OWING", label: "Devendo", badge: "warning" },
  CREDIT: { value: "CREDIT", label: "Com crédito", badge: "suggestion" },
  UP_TO_DATE: { value: "UP_TO_DATE", label: "Em dia", badge: "active" },
};

export function getAccountStatus(status) {
  return ACCOUNT_STATUS[status] ?? ACCOUNT_STATUS.UP_TO_DATE;
}

// O saldo é pago − devido: negativo = devedor; positivo = credor
// "− R$ 334,00", "+ R$ 167,00" ou "R$ 0,00"
export function formatBalance(balance) {
  const value = Number(balance);
  if (value < 0) return `− ${formatCurrency(-value)}`;
  if (value > 0) return `+ ${formatCurrency(value)}`;
  return formatCurrency(0);
}

// A cor do saldo: "negative" (vermelho), "positive" (azul) ou null (zero)
export function getBalanceTone(balance) {
  const value = Number(balance);
  if (value < 0) return "negative";
  return value > 0 ? "positive" : null;
}

// A nota embaixo do saldo: "devendo", "saldo credor" ou "em dia"
export function getBalanceNote(balance) {
  const value = Number(balance);
  if (value < 0) return "devendo";
  return value > 0 ? "saldo credor" : "em dia";
}
