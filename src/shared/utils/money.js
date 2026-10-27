// Formatação de valores monetários (chegam da API como string)
import Decimal from "decimal.js";

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const percentageFormatter = new Intl.NumberFormat("pt-BR", {
  style: "percent",
  maximumFractionDigits: 2,
});

// "1234.5" -> "R$ 1.234,50". Devolve "" para valor vazio ou inválido
export function formatCurrency(value) {
  if (value === null || value === undefined || value === "") return "";
  try {
    return currencyFormatter.format(new Decimal(value).toFixed(2));
  } catch {
    return "";
  }
}

// Soma valores monetários sem erro de ponto flutuante: ["60.00", "42.98"] -> "102.98"
export function sumMoney(values) {
  return values.reduce((total, value) => total.plus(value || 0), new Decimal(0)).toFixed(2);
}

// Percentual em escala 0-100, como a comissão: "40.5" -> "40,5%". Devolve "" para valor vazio ou inválido
export function formatPercentage(value) {
  if (value === null || value === undefined || value === "") return "";
  try {
    return percentageFormatter.format(new Decimal(value).dividedBy(100).toNumber());
  } catch {
    return "";
  }
}
