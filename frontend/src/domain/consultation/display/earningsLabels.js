// Textos das quantidades do rendimento, no singular ou no plural
export function formatConsultationCount(count) {
  return count === 1 ? "1 atendimento" : `${count} atendimentos`;
}

export function formatChargedAbsenceCount(count) {
  return count === 1 ? "1 falta cobrada" : `${count} faltas cobradas`;
}
