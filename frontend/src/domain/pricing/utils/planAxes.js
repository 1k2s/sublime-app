// Eixos (durações ou frequências) da tabela de preços de um grupo. Cada linha da tabela (uma versão
// do plano, ver priceRows.js) tem { prices }, as linhas de preço da API

// Eixo da linha: a duração ou a frequência (só um vem preenchido)
export function axisOf(price) {
  return price.durationMinutes ?? price.weeklyFrequency;
}

// Eixos com pelo menos um preço nas linhas dadas, em ordem crescente
export function listGroupAxes(rows) {
  const axes = new Set(rows.flatMap((row) => row.prices.map(axisOf)));
  return [...axes].sort((a, b) => a - b);
}

// Preço da linha num eixo, ou undefined quando ela não tem preço nele
export function findPriceByAxis(row, axis) {
  return row.prices.find((price) => axisOf(price) === axis);
}
