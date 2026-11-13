import { apiClient } from "@/shared/http/apiClient";

// GET /api/providers — lista todos os prestadores (ativos e inativos)
export async function listProviders() {
  const { data } = await apiClient.get("/providers");
  return data;
}

// POST /api/providers — { userId, name, commissionPercentage, referralCommissionPercentage }.
// Percentual nulo = padrão da clínica, aplicado pelo backend
export async function createProvider(payload) {
  const { data } = await apiClient.post("/providers", payload);
  return data;
}

// PUT /api/providers/{id} — { name, commissionPercentage, referralCommissionPercentage },
// os três obrigatórios. O usuário vinculado não muda
export async function updateProvider(id, payload) {
  const { data } = await apiClient.put(`/providers/${id}`, payload);
  return data;
}

// DELETE /api/providers/{id} — inativa o prestador (soft delete)
export async function deactivateProvider(id) {
  await apiClient.delete(`/providers/${id}`);
}

// PATCH /api/providers/{id}/activate — reativa o prestador (exige usuário ativo)
export async function activateProvider(id) {
  await apiClient.patch(`/providers/${id}/activate`);
}

// GET /api/providers/{id}/retroactive-grant — a liberação de hoje com saldo; null quando não há
// (a API responde 204 sem corpo)
export async function getRetroactiveGrant(id) {
  const { status, data } = await apiClient.get(`/providers/${id}/retroactive-grant`);
  return status === 204 ? null : data;
}

// POST /api/providers/{id}/retroactive-grant — { allowedCount } (1 a 50). Libera para hoje e
// substitui a liberação anterior do dia
export async function grantRetroactiveLaunches(id, allowedCount) {
  const { data } = await apiClient.post(`/providers/${id}/retroactive-grant`, { allowedCount });
  return data;
}
