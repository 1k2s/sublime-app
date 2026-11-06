const formatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 });

// 35.5 -> "35,5%". Só exibição: o valor que vale é o da API
export function formatPercentage(value) {
  if (value == null || value === "") return "--";
  return `${formatter.format(Number(value))}%`;
}

// "35,5" -> "35.5"; vazio -> null. Vai como texto para não perder casas decimais
export function toPercentageRequest(value) {
  const trimmed = value?.trim() ?? "";
  return trimmed === "" ? null : trimmed.replace(",", ".");
}

// 35.5 -> "35,5", para preencher o formulário
export function toPercentageInput(value) {
  return value == null ? "" : String(value).replace(".", ",");
}
