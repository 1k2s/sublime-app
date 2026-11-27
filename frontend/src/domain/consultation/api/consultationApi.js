import { apiClient } from "@/shared/http/apiClient";

// Pacientes ativos para o lançamento (id, nome e nascimento; os dois perfis)
export async function listLaunchPatients() {
  const { data } = await apiClient.get("/consultations/patients");
  return data;
}

// Contratos vinculados (ou o Avulso), com as durações com preço e o saldo do item
export async function getLaunchOptions({ patientId, techniqueId, occurredAt }) {
  const { data } = await apiClient.get("/consultations/launch-options", {
    params: { patientId, techniqueId, occurredAt },
  });
  return data;
}

// providerId: só o ADMIN informa (o PROVIDER é sempre ele mesmo)
export async function getRecentPatients({ providerId }) {
  const { data } = await apiClient.get("/consultations/recent-patients", { params: { providerId } });
  return data;
}

export async function getDaySummary({ providerId }) {
  const { data } = await apiClient.get("/consultations/day-summary", { params: { providerId } });
  return data;
}

// Ativos e inativos, do mais recente para o mais antigo (pela data do atendimento).
// from/to: período pela data do atendimento; launchedFrom/launchedTo: pela do lançamento
export async function listConsultations({ providerId, from, to, launchedFrom, launchedTo, status }) {
  const { data } = await apiClient.get("/consultations", {
    params: { providerId, from, to, launchedFrom, launchedTo, status },
  });
  return data;
}

// Rendimento de hoje, da semana e do mês, com os limites de cada período.
// providerId: só o ADMIN informa (vazio = todos)
export async function getEarningsSummary({ providerId }) {
  const { data } = await apiClient.get("/consultations/earnings-summary", { params: { providerId } });
  return data;
}

// Rendimento de um período (from/to pela data do lançamento); byProvider só para o ADMIN sem prestador
export async function getEarnings({ providerId, from, to }) {
  const { data } = await apiClient.get("/consultations/earnings", { params: { providerId, from, to } });
  return data;
}

export async function createConsultation(body) {
  const { data } = await apiClient.post("/consultations", body);
  return data;
}

export async function deactivateConsultation(id) {
  await apiClient.delete(`/consultations/${id}`);
}
