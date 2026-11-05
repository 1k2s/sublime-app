// Cliente HTTP único da aplicação: envia o token e trata o 401
import axios from "axios";

export const AUTH_TOKEN_KEY = "sublime.authToken";

let unauthorizedHandler = null;

// Registrado pelo AuthContext para encerrar a sessão quando a API responder 401
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

httpClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  }
);
