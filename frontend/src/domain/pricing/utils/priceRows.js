// Linhas da tabela de preços a partir do histórico do grupo (linhas soltas, uma por plano + eixo)
import { axisOf } from "./planAxes";

// Uma linha por versão do plano:
// - a versão aberta (validTo nulo): todas as linhas abertas do plano juntas (30 e 60 min, mesmo
//   reajustadas em datas diferentes); fica "EXPIRED" (Vencido) se qualquer eixo passou de 12 meses
//   sem reajuste (status resolvido pelo backend), ou "CURRENT" (Vigente) caso contrário;
// - cada encerrada ("CLOSED"): as linhas do plano encerradas no mesmo dia (validTo igual), ex: 30 e
//   60 min reajustadas juntas.
// Ordem: plano por ordem de cadastro (id); dentro dele, a versão aberta e depois as encerradas, da
// mais recente para a mais antiga.
export function buildPriceRows(prices) {
  const rows = new Map();
  for (const price of prices) {
    const status = price.status ?? (price.validTo == null ? "CURRENT" : "CLOSED");
    const isClosed = status === "CLOSED";
    const key = `${price.planId}-${isClosed ? price.validTo : "open"}`;
    if (!rows.has(key)) {
      rows.set(key, {
        key,
        planId: price.planId,
        planName: price.planName,
        status,
        validTo: price.validTo,
        prices: [],
      });
    }
    const row = rows.get(key);
    row.prices.push(price);
    // Basta um eixo vencido (> 12 meses sem reajuste) para sinalizar a versão aberta do plano
    if (!isClosed && status === "EXPIRED") {
      row.status = "EXPIRED";
    }
  }

  return [...rows.values()]
    .map((row) => ({ ...row, prices: row.prices.sort((a, b) => axisOf(a) - axisOf(b)) }))
    .sort((a, b) => {
      if (a.planId !== b.planId) return a.planId - b.planId;
      const aClosed = a.status === "CLOSED";
      const bClosed = b.status === "CLOSED";
      if (aClosed !== bClosed) return aClosed ? 1 : -1;
      return b.validTo.localeCompare(a.validTo);
    });
}

// Filtro de status da tela: "ALL", "CURRENT" (vigentes), "EXPIRED" (vencidos) ou "CLOSED" (encerrados)
export function filterPriceRows(rows, status) {
  if (status === "ALL") return rows;
  return rows.filter((row) => row.status === status);
}
