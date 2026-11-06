# 0024 — Sessão do frontend no `localStorage`, encerrada pela `expiresAt` e pelo 401

- **Status:** aceito
- **Data:** 2026-11-06

## Contexto

O login da API ([ADR 0023](0023-autenticacao-jwt-resource-server.md)) devolve o token no
corpo da resposta, com a `expiresAt` (8 horas, sem refresh token), e cada requisição
precisa dele no header `Authorization`. O front precisa manter a sessão entre recarregamentos
da página e encerrá-la quando o token deixa de valer.

## Decisão

- **Token, usuário e expiração no `localStorage`** (`sublime.authToken`,
  `sublime.authUser`, `sublime.authExpiresAt`). O `apiClient` envia o token em cada
  requisição.
- **A sessão termina** no logout, ao abrir o app com a `expiresAt` vencida ou a sessão
  incompleta, e num 401 da API (garantia final: token vencido durante o uso, usuário
  inativado ou com o perfil alterado).
- **Sem refresh token:** ao fim das 8 horas, o usuário faz login de novo.
- **Ao sair, o cache do TanStack Query é limpo**, para o próximo usuário do mesmo navegador
  não ver os dados do anterior.

## Alternativas descartadas

- **Cookie `httpOnly`:** protege o token de XSS, mas exige configuração de CSRF e
  `SameSite` e muda o `apiClient`.
- **Refresh token** (o mesmo motivo do [ADR 0023](0023-autenticacao-jwt-resource-server.md)).

## Consequências

- O token fica acessível ao JavaScript da página: a proteção contra XSS depende do próprio
  front (o React já escapa o conteúdo renderizado).
- A sessão vale para todas as abas do mesmo navegador.
