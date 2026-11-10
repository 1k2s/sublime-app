// Chamadas HTTP do módulo consultation (uma função por endpoint)
import { apiClient } from "@/shared/http/apiClient";

// Lançamentos, ativos e inativos, do mais recente para o mais antigo.
// Filtros opcionais: { providerId, patientId, from, to } (datas "AAAA-MM-DD", inclusivas)
export async function listConsultations(filters) {
  const { data } = await apiClient.get("/consultations", { params: filters });
  return data;
}

// Contratos vinculados ao paciente na data, sem gravar nada: vazio = Avulso, um = esse, dois = o usuário escolhe
export async function listEligibleContracts({ patientId, techniqueId, occurredAt }) {
  const { data } = await apiClient.get("/consultations/eligible-contracts", {
    params: { patientId, techniqueId, occurredAt },
  });
  return data;
}

// Lança um atendimento. Preço, comissão e repasse são resolvidos pela API e voltam na resposta
export async function createConsultation(payload) {
  const { data } = await apiClient.post("/consultations", payload);
  return data;
}
