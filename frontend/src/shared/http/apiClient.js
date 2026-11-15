// Cliente HTTP único da aplicação: envia o token e trata o 401
import axios from "axios";

export const AUTH_TOKEN_KEY = "sublime.authToken";

let unauthorizedHandler = null;

// Registrado pelo AuthContext para encerrar a sessão quando a API responder 401
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

// Sem a VITE_API_URL, o Axios chamaria o próprio Vite, que responde 200 com o index.html:
// a tela quebraria sem dizer o motivo. Falha logo, com a causa
function requireApiUrl() {
  const url = import.meta.env.VITE_API_URL;
  if (!url) {
    throw new Error(
      "VITE_API_URL não definida. Crie o frontend/.env a partir do frontend/.env.example e reinicie o npm run dev."
    );
  }
  return url;
}

//Criando uma instância do Axios: um objeto que já vem com todos os verbos http
export const apiClient = axios.create({
  baseURL: requireApiUrl(),
});

//Incluindo o TOKEN a cada requisição
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

//Tratando o erro 401 e trocando a mensagem técnica do Axios pela mensagem da API
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      unauthorizedHandler?.();
    }
    error.message = describeApiError(error);
    return Promise.reject(error);
  }
);

//A mensagem é da API (o "detail" do ProblemDetail); as de reserva cobrem quando ela não mandou nenhuma
function describeApiError(error) {
  if (!error.response) {
    return "Não foi possível conectar ao servidor. Tente novamente.";
  }
  return error.response.data?.detail ?? "Ocorreu um erro inesperado. Tente novamente.";
}
