import { apiClient } from "@/shared/http/apiClient";

// POST /api/auth/login (público). Devolve { token, expiresAt, user: { id, email, role } }
export async function login({ email, password }) {
  const { data } = await apiClient.post("/auth/login", { email, password });
  return data;
}
