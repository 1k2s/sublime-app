# Spec — Autenticação

Login por e-mail e senha com token JWT, rotas da API protegidas e tela de login ligada à
API.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo](#2-objetivo)
- [3. Contrato da API (já implementado)](#3-contrato-da-api-já-implementado)
- [4. Regras do frontend](#4-regras-do-frontend)
- [5. Desenho do frontend](#5-desenho-do-frontend)
- [6. Tarefas](#6-tarefas)
- [7. Documentação ao concluir](#7-documentação-ao-concluir)
- [8. ADR a registrar](#8-adr-a-registrar)

## 1. Status

- **Backend: implementado e testado (2026-11-05).** As regras e o desenho estão nos
  documentos de referência: seção `User` do [`domain-model.md`](../domain-model.md),
  "Segurança" e "Formato dos erros" do [`architecture.md`](../architecture.md),
  [ADR 0023](../adr/0023-autenticacao-jwt-resource-server.md) e "Variáveis de ambiente" do
  [`development.md`](../development.md).
- **Frontend: falta.** O login da tela ainda é mockado (`domain/auth/api/authApi.js`).
- Branch: `feature/authentication`, criada a partir da `chore/monorepo` (o PR da
  autenticação tem base na `chore/monorepo` até ela entrar na `dev`; os dois PRs entram com
  "merge commit", nunca "squash").

## 2. Objetivo

Ligar a tela de login à API: login real, sessão que expira com o token, papéis com os
valores do backend, tela inicial por perfil e as mensagens de erro da API na tela.

- **Fora do escopo (feature seguinte, spec própria): autorização por perfil** (quais
  endpoints e telas cada perfil acessa, o prestador do lançamento vindo do login). Até lá,
  qualquer usuário logado acessa todos os endpoints.

## 3. Contrato da API (já implementado)

`POST /api/auth/login` (público)

```json
// request
{ "email": "admin@sublime.com", "password": "********" }

// 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-11-06T04:22:45.367Z",
  "user": { "id": 3, "email": "admin@sublime.com", "role": "ADMIN" }
}
```

- 400: campos inválidos (mapa `errors`).
- 401: "E-mail ou senha inválidos." (o mesmo para e-mail inexistente, senha errada e
  usuário inativo).

Todas as outras rotas exigem `Authorization: Bearer <token>` e respondem **401** "Sessão
inválida ou expirada. Faça login novamente." quando o token falta, é inválido, venceu ou o
usuário foi inativado / teve o perfil alterado. O `role` vem como `ADMIN` ou `PROVIDER`.

## 4. Regras do frontend

- **Token, usuário e expiração no `localStorage`**, como hoje (`sublime.authToken`,
  `sublime.authUser`).
- **A sessão termina** quando:
  - o usuário sai (logout);
  - o app é aberto com o token já vencido (pela `expiresAt` guardada);
  - a API responde 401 (garantia final: token vencido no meio do uso, usuário inativado ou
    com o perfil alterado).
- **Ao sair, o cache de dados (TanStack Query) é limpo**, para o próximo usuário do mesmo
  navegador não ver as listas do anterior.
- **Tela inicial por perfil:** `ADMIN` → `/patients`; `PROVIDER` → `/consultations`.
- **Papéis com os valores do backend:** `ADMIN` e `PROVIDER`.
- **Mensagens de erro da API** aparecem para o usuário (o `detail` do `ProblemDetail`),
  nunca a mensagem técnica do Axios ("Request failed with status code 401").
- A tela de login tem só e-mail e senha (sem "criar conta").

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `shared/http/apiClient.js` | o interceptor de resposta troca o `error.message` (texto técnico do Axios) pelo `detail` do `ProblemDetail`, ou por uma mensagem de reserva quando não houve resposta ou ela veio sem `detail`. As pages continuam com `showError(error.message)` |
| `domain/auth/api/authApi.js` | troca o mock por `apiClient.post("/auth/login", { email, password })`; devolve `{ token, expiresAt, user }` |
| `domain/auth/context/AuthContext.jsx` | `signIn` guarda também a `expiresAt`; `readStoredUser` descarta a sessão vencida; `signOut` limpa o cache (`queryClient.clear()`) |
| `domain/auth/roles.js` | valores `ADMIN` e `PROVIDER` |
| `app/router.jsx` | a rota `/` leva à tela inicial do perfil (`getHomePath`), no lugar do `/consultations` fixo |

O `apiClient` já envia o token e trata o 401; muda só a mensagem do erro. A `LoginPage` não
muda: já mostra o `error.message`.

## 6. Tarefas

**Backend:** concluído (dependências, `user`, módulo `auth`, `SecurityConfig`, primeiro
admin, Swagger com "Authorize", `backend/.env` local e teste manual de todos os casos).

**Frontend** (`npm run build` e `npm run lint` ao fim):
- [x] Corrigir o build (resolvido pelo PR #13 do repositório antigo, trazido para o
      monorepo em 2026-11-05).
- [x] Mensagem da API no `error.message` (interceptor do `apiClient`).
- [x] `authApi.js` real.
- [x] `AuthContext`: `expiresAt` e limpeza do cache.
- [x] `roles.js` com `ADMIN` / `PROVIDER` (e o e-mail no menu, no lugar do nome que a API não
      devolve).
- [x] Tela inicial por perfil (`HomeRedirect` na rota `/`).
- [ ] Teste pela tela: login de cada perfil, senha errada, sessão vencida, logout seguido
      de outro login, usuário inativado durante o uso.

## 7. Documentação ao concluir

- [`architecture.md`](../architecture.md), seção "Frontend" → "Rotas e sessão": expiração
  pela `expiresAt`, limpeza do cache ao sair, tela inicial por perfil, papéis `ADMIN` /
  `PROVIDER`.
- [`roadmap.md`](../roadmap.md): autenticação concluída; próximo passo, a autorização por
  perfil; tirar das pendências do front os papéis divergentes e as mensagens de erro da API.
- ADR da seção seguinte e remoção desta spec.

## 8. ADR a registrar

- **Sessão no frontend: token no `localStorage`, 8 horas, sem refresh token.**
  *Alternativas descartadas:* cookie `httpOnly` (protege o token de XSS, mas exige
  configuração de CSRF e `SameSite` e muda o `apiClient`); refresh token.
