// Eixo da linha de preço: por duração, os minutos da sessão; por frequência, as vezes por semana

// Curto, para listas: "50 min" ou "2x por semana"
export function formatPriceAxis({ durationMinutes, weeklyFrequency } = {}) {
  if (durationMinutes != null) return `${durationMinutes} min`;
  if (weeklyFrequency != null) return `${weeklyFrequency}x por semana`;
  return "";
}

// Título de coluna ou campo: "Sessão de 30 min" ou "2x por semana"
export function formatAxisTitle(axis, pricingModel) {
  return pricingModel === "DURATION_BASED" ? `Sessão de ${axis} min` : `${axis}x por semana`;
}

// Só o valor do eixo, para frases: "30 min" ou "2x por semana"
export function formatAxisShort(axis, pricingModel) {
  return pricingModel === "DURATION_BASED" ? `${axis} min` : `${axis}x por semana`;
}
