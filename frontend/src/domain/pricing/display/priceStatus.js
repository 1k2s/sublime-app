// Status de uma versão de preço na tela, a partir do status resolvido pela API (CURRENT, EXPIRED
// após 12 meses sem reajuste ou CLOSED por reajuste), e o período em que valeu (no detalhe)
import { parseISO, subDays } from "date-fns";
import { formatDate } from "@/shared/utils/date";
import { formatPlanValidity } from "./formatPlanValidity";

export const PRICE_STATUS = {
  CURRENT: { value: "CURRENT", label: "Vigente", badge: "active" },
  // aberto, sem reajuste há mais de 12 meses: sinaliza revisão na tabela
  EXPIRED: { value: "EXPIRED", label: "Vencido", badge: "warning" },
  // encerrado por um reajuste (validTo preenchido)
  CLOSED: { value: "CLOSED", label: "Encerrado", badge: "inactive" },
};

export const PRICE_STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Vigentes", value: "CURRENT" },
  { label: "Vencidos", value: "EXPIRED" },
  { label: "Encerrados", value: "CLOSED" },
];

export function getPriceStatus(status) {
  return PRICE_STATUS[status] ?? PRICE_STATUS.CLOSED;
}

// Aberto (Vigente ou Vencido): "Desde 17/11/2026". Encerrado: "01/10/2026 a 16/11/2026". O validTo
// é exclusivo (a linha valeu até o dia anterior; ver a convenção de datas em SessionDurationPrice,
// no domain-model); trocada no mesmo dia em que entrou, não valeu em nenhum dia: "Substituído em ..."
export function formatRowPeriod(row) {
  if (row.status !== "CLOSED") return formatPlanValidity(row.prices);
  const firstDay = row.prices.map((price) => price.validFrom).sort()[0];
  const lastDay = subDays(parseISO(row.validTo), 1);
  if (lastDay < parseISO(firstDay)) return `Substituído em ${formatDate(row.validTo)}`;
  return `${formatDate(firstDay)} a ${formatDate(lastDay)}`;
}
