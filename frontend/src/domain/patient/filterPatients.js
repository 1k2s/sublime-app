// Busca e filtros de pacientes, em memória sobre a lista inteira (lista pequena). Função pura:
// quem lista (page, busca do contrato e do lançamento) chama com o estado dos filtros.
// status: "ALL", "ACTIVE" ou "INACTIVE". search procura no nome ou nos dígitos do CPF
import { onlyDigits } from "@/shared/utils/masks";

export function filterPatients(patients, { search, status }) {
  const term = search.trim().toLowerCase();
  const digits = onlyDigits(term);

  return patients.filter((patient) => {
    if (status === "ACTIVE" && !patient.active) return false;
    if (status === "INACTIVE" && patient.active) return false;
    if (!term) return true;

    const matchName = patient.name?.toLowerCase().includes(term);
    const matchCpf = digits ? onlyDigits(patient.cpf).includes(digits) : false;
    return matchName || matchCpf;
  });
}
