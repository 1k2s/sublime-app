import { apiClient } from "@/shared/http/apiClient";

// POST /api/auth/login (público). Devolve { token, expiresAt, user: { id, email, role } }
export async function login({ email, password }) {
  const { data } = await apiClient.post("/auth/login", { email, password });
  return data;
}

// POST /api/auth/first-access (público). Troca a senha provisória pela senha pessoal
// definitiva e devolve { token, expiresAt, user: { id, email, role } }
export async function completeFirstAccess({ email, provisionalPassword, newPassword }) {
  const { data } = await apiClient.post("/auth/first-access", {
    email,
    provisionalPassword,
    newPassword,
  });
  return data;
}
