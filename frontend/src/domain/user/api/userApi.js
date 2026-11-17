import { apiClient } from "@/shared/http/apiClient";

// GET /api/users — lista todos os usuários (ativos e inativos)
export async function listUsers() {
  const { data } = await apiClient.get("/users");
  return data;
}

// POST /api/users — cria usuário com senha provisória (troca obrigatória no primeiro acesso)
export async function createUser({ email, password, role }) {
  const { data } = await apiClient.post("/users", { email, password, role });
  return data;
}

// PUT /api/users/{id} — atualiza e-mail, perfil e, opcionalmente, redefine senha provisória
export async function updateUser(id, { email, password, role }) {
  const payload = {
    email,
    role,
    password: password && password.trim().length > 0 ? password : null,
  };
  const { data } = await apiClient.put(`/users/${id}`, payload);
  return data;
}

// DELETE /api/users/{id} — inativa o usuário (soft delete)
export async function deactivateUser(id) {
  await apiClient.delete(`/users/${id}`);
}

// PATCH /api/users/{id}/activate — reativa o usuário (devolve o acesso)
export async function activateUser(id) {
  await apiClient.patch(`/users/${id}/activate`);
}

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
