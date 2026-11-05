import { ROLES } from "../roles";

// Mock até o backend expor o login. Depois, trocar o corpo por httpClient.post("/auth/login", credentials)
const MOCK_USERS = [
  { id: 1, name: "Administrador", email: "admin@sublime.com", role: ROLES.ADMIN.value },
  { id: 2, name: "Prestador", email: "prestador@sublime.com", role: ROLES.PROVIDER.value },
];

export async function login({ email, password }) {
  await new Promise((resolve) => setTimeout(resolve, 500));

  const user = MOCK_USERS.find((mockUser) => mockUser.email === email.toLowerCase());
  if (!user || password.length < 6) {
    throw new Error("E-mail ou senha inválidos");
  }

  return { token: `mock-token-${user.id}`, user };
}
