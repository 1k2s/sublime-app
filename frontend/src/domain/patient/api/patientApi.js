import { apiClient } from "@/shared/http/apiClient";

// GET
export async function listPatients() {
  const { data } = await apiClient.get("/patients");
  return data;
}

// GET ID
export async function getPatientById(id) {
  const { data } = await apiClient.get(`/patients/${id}`);
  return data;
}

// POST
export async function createPatient(payload) {
  const { data } = await apiClient.post("/patients", payload);
  return data;
}


// PUT
export async function updatePatient(id, payload) {
  const { data } = await apiClient.put(`/patients/${id}`, payload);
  return data;
}

// DELETE
export async function deactivatePatient(id) {
  await apiClient.delete(`/patients/${id}`);
}

// PATCH
export async function activatePatient(id) {
  await apiClient.patch(`/patients/${id}/activate`);
}
