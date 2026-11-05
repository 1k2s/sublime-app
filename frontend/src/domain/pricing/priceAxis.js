// Preço por duração mostra os minutos da sessão; por frequência, as vezes por semana
export function formatPriceAxis({ durationMinutes, weeklyFrequency } = {}) {
  if (durationMinutes != null) return `${durationMinutes} min`;
  if (weeklyFrequency != null) return `${weeklyFrequency}x por semana`;
  return "";
}

export function getPriceAxisLabel(pricingModel) {
  return pricingModel === "DURATION_BASED" ? "Duração" : "Frequência semanal";
}
