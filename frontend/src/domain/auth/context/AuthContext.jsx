import { useCallback, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { AUTH_TOKEN_KEY, setUnauthorizedHandler } from "@/shared/http/apiClient";
import { AuthContext } from "./useAuth";

const AUTH_USER_KEY = "sublime.authUser";
const AUTH_EXPIRES_AT_KEY = "sublime.authExpiresAt";
const SESSION_KEYS = [AUTH_TOKEN_KEY, AUTH_USER_KEY, AUTH_EXPIRES_AT_KEY];

// O evento "storage" chega quando outra aba muda o localStorage; key null = localStorage.clear()
function isSessionChange(event) {
  return event.key === null || SESSION_KEYS.includes(event.key);
}

// Sessão guardada no navegador, só leitura. Incompleta ou vencida (pela expiresAt do login) conta
// como sem sessão
function findStoredUser() {
  try {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    const storedUser = localStorage.getItem(AUTH_USER_KEY);
    const expiresAt = localStorage.getItem(AUTH_EXPIRES_AT_KEY);
    if (!token || !storedUser || isExpired(expiresAt)) return null;
    return JSON.parse(storedUser);
  } catch {
    return null;
  }
}

// Ao abrir a aba: a sessão sem uso é apagada, para o token vencido não seguir nas requisições
function readStoredUser() {
  const storedUser = findStoredUser();
  if (!storedUser) clearStoredSession();
  return storedUser;
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

  // Uma sessão por navegador, com as abas sincronizadas (ADR 0035): quando outra aba entra ou sai,
  // esta relê a sessão inteira (o login grava três chaves e gera três eventos; relendo, o resultado é o
  // mesmo em qualquer ordem). Só lê, nunca apaga: no meio do login da outra aba, a sessão ainda está
  // incompleta, e apagar levaria o token que ela acabou de gravar. Outro usuário: limpa o cache, que
  // tem os dados do anterior. Sem sessão: user null, e o ProtectedRoute leva ao login
  useEffect(() => {
    function syncWithOtherTabs(event) {
      if (!isSessionChange(event)) return;
      const storedUser = findStoredUser();
      if (storedUser?.id !== user?.id) queryClient.clear();
      setUser(storedUser);
    }

    window.addEventListener("storage", syncWithOtherTabs);
    return () => window.removeEventListener("storage", syncWithOtherTabs);
  }, [user, queryClient]);

  const value = useMemo(() => ({ user, signIn, signOut, updateUser }), [user, signIn, signOut, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
