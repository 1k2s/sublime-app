// Frequência de um item por duração, com os valores do enum SessionFrequency do backend. Ordem
// da tela: as semanais primeiro (as mais usadas), depois quinzenal e mensal. O prazo que cada uma
// dá é calculado pelo backend
export const SESSION_FREQUENCY_OPTIONS = [
  { value: "ONCE_A_WEEK", label: "1x por semana" },
  { value: "TWICE_A_WEEK", label: "2x por semana" },
  { value: "THREE_TIMES_A_WEEK", label: "3x por semana" },
  { value: "FOUR_TIMES_A_WEEK", label: "4x por semana" },
  { value: "FIVE_TIMES_A_WEEK", label: "5x por semana" },
  { value: "BIWEEKLY", label: "Quinzenal" },
  { value: "MONTHLY", label: "1x por mês" },
];

export function getSessionFrequencyLabel(value) {
  return SESSION_FREQUENCY_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "--";
}
