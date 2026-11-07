import { apiClient } from "@/shared/http/apiClient";

// GET /api/users/me: o usuário logado. Devolve { id, email, role, active, defaultAdmin }
export async function getMe() {
  const { data } = await apiClient.get("/users/me");
  return data;
}

// PUT /api/users/me: { email, newPassword (vazia = mantém), currentPassword }. Devolve o usuário atualizado
export async function updateMe({ email, newPassword, currentPassword }) {
  const { data } = await apiClient.put("/users/me", { email, newPassword, currentPassword });
  return data;
}
