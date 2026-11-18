// Linhas da tabela de preços a partir do histórico do grupo (linhas soltas, uma por plano + eixo)
import { axisOf } from "./planAxes";

// Uma linha por versão do plano:
// - a vigente: todas as linhas do plano com validTo nulo (30 e 60 min juntas, mesmo reajustadas
//   em datas diferentes);
// - cada encerrada: as linhas do plano encerradas no mesmo dia (validTo igual), ex: 30 e 60 min
//   reajustadas juntas.
// Ordem: plano por ordem de cadastro (id); dentro dele, a vigente e depois as encerradas, da mais
// recente para a mais antiga.
export function buildPriceRows(prices) {
  const rows = new Map();
  for (const price of prices) {
    const isCurrent = price.validTo == null;
    const key = `${price.planId}-${isCurrent ? "current" : price.validTo}`;
    if (!rows.has(key)) {
      rows.set(key, {
        key,
        planId: price.planId,
        planName: price.planName,
        isCurrent,
        validTo: price.validTo,
        prices: [],
      });
    }
    rows.get(key).prices.push(price);
  }

  return [...rows.values()]
    .map((row) => ({ ...row, prices: row.prices.sort((a, b) => axisOf(a) - axisOf(b)) }))
    .sort((a, b) => {
      if (a.planId !== b.planId) return a.planId - b.planId;
      if (a.isCurrent !== b.isCurrent) return a.isCurrent ? -1 : 1;
      return b.validTo.localeCompare(a.validTo);
    });
}

// Filtro de status da tela: "ALL", "CURRENT" (vigentes) ou "CLOSED" (encerradas)
export function filterPriceRows(rows, status) {
  if (status === "CURRENT") return rows.filter((row) => row.isCurrent);
  if (status === "CLOSED") return rows.filter((row) => !row.isCurrent);
  return rows;
}
