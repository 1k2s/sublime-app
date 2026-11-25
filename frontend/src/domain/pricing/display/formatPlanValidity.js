// Vigência de um plano na tabela, pelas linhas vigentes dele (validFrom de cada uma)
import { formatDate } from "@/shared/utils/date";

// Todas desde a mesma data: "Desde 17/11/2026". Reajustadas em datas diferentes: a mais recente,
// "Atualizado em 20/11/2026" (a data de cada valor aparece no detalhe). Sem preço: ""
export function formatPlanValidity(prices) {
  const dates = [...new Set(prices.map((price) => price.validFrom))].sort();
  if (dates.length === 0) return "";
  const latest = formatDate(dates[dates.length - 1]);
  return dates.length === 1 ? `Desde ${latest}` : `Atualizado em ${latest}`;
}

// Se as linhas do plano têm datas de início diferentes (o detalhe mostra a data em cada valor)
export function hasMixedValidity(prices) {
  return new Set(prices.map((price) => price.validFrom)).size > 1;
}
