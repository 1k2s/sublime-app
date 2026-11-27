import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";

// A previsão de recebimento da prévia do pagamento (receivables da API), em texto:
// "R$ 668,00 em 26/11/2026" (uma parte) ou "3 × R$ 534,40 (dez, jan e fev)"; com os centavos na
// primeira, "R$ 534,41 + 2 × R$ 534,40 (dez, jan e fev)"
export function formatReceivables(receivables = []) {
  if (receivables.length === 0) return "";
  if (receivables.length === 1) {
    return `${formatCurrency(receivables[0].amount)} em ${formatDate(receivables[0].date)}`;
  }
  const [first, ...rest] = receivables;
  const values =
    Number(first.amount) === Number(rest[0].amount)
      ? `${receivables.length} × ${formatCurrency(first.amount)}`
      : `${formatCurrency(first.amount)} + ${rest.length} × ${formatCurrency(rest[0].amount)}`;
  const months = receivables.map((part) => formatDate(part.date, "MMM"));
  return `${values} (${months.slice(0, -1).join(", ")} e ${months.at(-1)})`;
}
