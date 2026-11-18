// Status de uma versão de preço na tela: "Vigente" (validTo nulo) ou "Encerrado" (encerrada por
// um reajuste), e o período em que valeu (no detalhe)
import { parseISO, subDays } from "date-fns";
import { formatDate } from "@/shared/utils/date";
import { formatPlanValidity } from "./formatPlanValidity";

export const PRICE_STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Vigentes", value: "CURRENT" },
  { label: "Encerrados", value: "CLOSED" },
];

export function getPriceStatusLabel(isCurrent) {
  return isCurrent ? "Vigente" : "Encerrado";
}

// Vigente: "Desde 17/11/2026". Encerrado: "01/10/2026 a 16/11/2026". O validTo é exclusivo (a
// linha valeu até o dia anterior; ver a convenção de datas em SessionDurationPrice, no
// domain-model); trocada no mesmo dia em que entrou, não valeu em nenhum dia: "Substituído em ..."
export function formatRowPeriod(row) {
  if (row.isCurrent) return formatPlanValidity(row.prices);
  const firstDay = row.prices.map((price) => price.validFrom).sort()[0];
  const lastDay = subDays(parseISO(row.validTo), 1);
  if (lastDay < parseISO(firstDay)) return `Substituído em ${formatDate(row.validTo)}`;
  return `${formatDate(firstDay)} a ${formatDate(lastDay)}`;
}
