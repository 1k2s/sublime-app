// Status de lançamento: "value" idêntico ao enum ConsultationStatus do backend.
// Só existem os que são cobrados e geram repasse (ver domain-model.md, Consultation, "Status")
export const CONSULTATION_STATUSES = [
  { value: "ATTENDED", label: "Atendido" },
  { value: "NO_SHOW", label: "Faltou sem aviso" },
  { value: "CANCELED_LATE", label: "Desmarcou com menos de 6h" },
];

export function getStatusLabel(value) {
  return CONSULTATION_STATUSES.find((status) => status.value === value)?.label ?? value;
}
