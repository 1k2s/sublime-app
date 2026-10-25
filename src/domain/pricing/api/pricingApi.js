// Chamadas HTTP do módulo pricing (uma função por endpoint)
import { httpClient } from "@/shared/http/httpClient";

export async function listPricingGroups() {
  const { data } = await httpClient.get("/pricing-groups");
  return data;
}

export async function listPlans() {
  const { data } = await httpClient.get("/plans");
  return data;
}

export async function listTechniques() {
  const { data } = await httpClient.get("/techniques");
  return data;
}

// Preços vigentes do grupo da técnica (técnicas do mesmo grupo devolvem a mesma lista)
export async function getTechniquePrices(techniqueId) {
  const { data } = await httpClient.get(`/techniques/${techniqueId}/prices`);
  return data;
}

// Preço novo ou reajuste: o backend fecha a vigência anterior e abre a nova com validFrom = hoje
export async function createDurationPrice(payload) {
  const { data } = await httpClient.post("/prices/duration", payload);
  return data;
}

export async function createFrequencyPrice(payload) {
  const { data } = await httpClient.post("/prices/frequency", payload);
  return data;
}
