// Chamadas HTTP do módulo pricing (uma função por endpoint)
import { apiClient } from "@/shared/http/apiClient";

export async function listPricingGroups() {
  const { data } = await apiClient.get("/pricing-groups");
  return data;
}

export async function listPlans() {
  const { data } = await apiClient.get("/plans");
  return data;
}

export async function listTechniques() {
  const { data } = await apiClient.get("/techniques");
  return data;
}

// Preços vigentes do grupo da técnica (técnicas do mesmo grupo devolvem a mesma lista)
export async function getTechniquePrices(techniqueId) {
  const { data } = await apiClient.get(`/techniques/${techniqueId}/prices`);
  return data;
}

// Preço novo ou reajuste: o backend fecha a vigência anterior e abre a nova com validFrom = hoje
export async function createDurationPrice(payload) {
  const { data } = await apiClient.post("/prices/duration", payload);
  return data;
}

export async function createFrequencyPrice(payload) {
  const { data } = await apiClient.post("/prices/frequency", payload);
  return data;
}
