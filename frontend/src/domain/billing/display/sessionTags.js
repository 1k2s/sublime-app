// Numeração e etiqueta das sessões, como a API resolve (number, contractedCount e tag). Ver
// "Numeração das sessões" em Payment, no domain-model. badge: a variante do Badge
const SESSION_TAGS = {
  BEYOND_CONTRACT: { label: "além do contratado", badge: "warning", highlight: true },
  BEYOND_CLASSES: { label: "além das aulas previstas", badge: "warning", highlight: true },
  OUT_OF_CONTRACT: { label: "fora do contrato", badge: "neutral", highlight: false },
  REMOVED_ITEM: { label: "item removido no aditivo", badge: "neutral", highlight: false },
};

export function getSessionTag(tag) {
  return tag ? (SESSION_TAGS[tag] ?? null) : null;
}

// "5ª de 6", "7ª" (item removido: sem o contratado) ou "—" (de-para: sem número)
export function formatSessionNumber({ number, contractedCount }) {
  if (number == null) return "—";
  return contractedCount != null ? `${number}ª de ${contractedCount}` : `${number}ª`;
}
