// Chamadas HTTP do módulo provider (uma função por endpoint)
import { apiClient } from "@/shared/http/apiClient";

export async function listProviders() {
  const { data } = await apiClient.get("/providers");
  return data;
}

export async function getProviderById(id) {
  const { data } = await apiClient.get(`/providers/${id}`);
  return data;
}

export async function createProvider(payload) {
  const { data } = await apiClient.post("/providers", payload);
  return data;
}

export async function updateProvider(id, payload) {
  const { data } = await apiClient.put(`/providers/${id}`, payload);
  return data;
}

export async function deactivateProvider(id) {
  await apiClient.delete(`/providers/${id}`);
}

export async function activateProvider(id) {
  await apiClient.patch(`/providers/${id}/activate`);
}
