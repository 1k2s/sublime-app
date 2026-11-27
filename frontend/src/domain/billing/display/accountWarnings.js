import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";

// O texto de cada aviso da conta, montado com os dados que a API manda (type e os campos do
// tipo). Ver "Payment" no domain-model
const WARNING_TEXTS = {
  OVERDUE_INSTALLMENTS: ({ count, date }) =>
    count > 1 ? `${count} parcelas vencidas (desde ${formatDate(date, "dd/MM")})` : `Parcela de ${formatDate(date, "dd/MM")} vencida`,
  BEYOND_CONTRACT: ({ techniqueName, count, contractedCount, amount }) =>
    `${techniqueName}: ${count} ${count > 1 ? "sessões" : "sessão"} além das ${contractedCount} contratadas (+ ${formatCurrency(amount)})`,
  SINGLE_SESSION_OPEN: ({ amount }) => `Atendimento avulso em aberto (${formatCurrency(amount)})`,
  INACTIVE_WITH_DEBT: ({ amount }) => `Contrato cancelado com saldo devedor (${formatCurrency(amount)})`,
};

export function formatWarning(warning) {
  return WARNING_TEXTS[warning.type]?.(warning) ?? warning.type;
}

// Os avisos numa linha só (a coluna da lista): "Parcela de 10/11 vencida · Atendimento avulso..."
export function formatWarnings(warnings = []) {
  return warnings.length > 0 ? warnings.map(formatWarning).join(" · ") : "—";
}
