// Chamadas HTTP do módulo patient (uma função por endpoint)
import { apiClient } from "@/shared/http/apiClient";

export async function listPatients() {
  const { data } = await apiClient.get("/patients");
  return data;
}

export async function createPatient(payload) {
  const { data } = await apiClient.post("/patients", payload);
  return data;
}

export async function updatePatient(id, payload) {
  const { data } = await apiClient.put(`/patients/${id}`, payload);
  return data;
}

export async function deactivatePatient(id) {
  await apiClient.delete(`/patients/${id}`);
}

export async function activatePatient(id) {
  await apiClient.patch(`/patients/${id}/activate`);
}
