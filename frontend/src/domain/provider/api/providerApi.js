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
