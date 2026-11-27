// Lista do dashboard: o filtro por status, em memória (lista pequena, ver "Camada api/" no
// architecture.md), e a ordem pela data do lançamento, a mesma do filtro do período e do
// rendimento, do mais recente para o mais antigo. A API ordena pela data do atendimento, e um
// retroativo lançado hoje ficaria no fim do mês. Só ordem de exibição: nenhuma regra muda.
export const ALL_STATUSES = "ALL";

function byLaunchedAtDesc(first, second) {
  // ISO 8601 ("2026-11-26T14:30:00"): a ordem do texto é a ordem da data
  return second.launchedAt.localeCompare(first.launchedAt) || second.id - first.id;
}

export function filterConsultations(consultations, { status = ALL_STATUSES } = {}) {
  const filtered =
    status === ALL_STATUSES ? consultations : consultations.filter((consultation) => consultation.status === status);
  return [...filtered].sort(byLaunchedAtDesc);
}
