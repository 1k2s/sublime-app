import { useCallback, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { AUTH_TOKEN_KEY, setUnauthorizedHandler } from "@/shared/http/apiClient";
import { AuthContext } from "./useAuth";

const AUTH_USER_KEY = "sublime.authUser";
const AUTH_EXPIRES_AT_KEY = "sublime.authExpiresAt";

// Sessão guardada no navegador. Incompleta ou vencida (pela expiresAt do login) conta como
// sem sessão e é apagada, para o token vencido não seguir nas requisições
function readStoredUser() {
  try {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    const storedUser = localStorage.getItem(AUTH_USER_KEY);
    const expiresAt = localStorage.getItem(AUTH_EXPIRES_AT_KEY);
    if (!token || !storedUser || isExpired(expiresAt)) {
      clearStoredSession();
      return null;
    }
    return JSON.parse(storedUser);
  } catch {
    return null;
  }
}

// Sem data ou com data ilegível também conta como vencida (ex: sessão gravada antes da expiresAt existir)
function isExpired(expiresAt) {
  const expiresAtTime = Date.parse(expiresAt);
  return Number.isNaN(expiresAtTime) || expiresAtTime <= Date.now();
}

function clearStoredSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  localStorage.removeItem(AUTH_EXPIRES_AT_KEY);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const queryClient = useQueryClient();

  // session = resposta do login: { token, expiresAt, user }
  const signIn = useCallback((session) => {
    localStorage.setItem(AUTH_TOKEN_KEY, session.token);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(session.user));
    localStorage.setItem(AUTH_EXPIRES_AT_KEY, session.expiresAt);
    setUser(session.user);
  }, []);

  // Limpa também o cache do TanStack Query: o próximo usuário do navegador não vê os dados do anterior
  const signOut = useCallback(() => {
    clearStoredSession();
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  // Atualiza o usuário da sessão depois de uma alteração dos próprios dados (Configurações),
  // para o menu mostrar o e-mail novo. Mantém a forma da sessão do login ({ id, email, role })
  const updateUser = useCallback(({ email }) => {
    setUser((current) => {
      const updated = { ...current, email };
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(updated));
      return updated;
    });
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(signOut);
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  const value = useMemo(() => ({ user, signIn, signOut, updateUser }), [user, signIn, signOut, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
