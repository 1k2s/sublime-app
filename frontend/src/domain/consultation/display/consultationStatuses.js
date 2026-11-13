// Status do atendimento: os valores são os do enum ConsultationStatus do backend (os três que
// são cobrados e geram repasse; ver "Status" em Consultation, no domain-model)
export const CONSULTATION_STATUS_OPTIONS = [
  { value: "ATTENDED", label: "Atendido" },
  { value: "NO_SHOW", label: "Faltou" },
  { value: "CANCELED_LATE", label: "Desmarcou < 6h" },
];

export const DEFAULT_CONSULTATION_STATUS = "ATTENDED";

export function getConsultationStatusLabel(value) {
  return CONSULTATION_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}
