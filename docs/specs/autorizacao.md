# Spec — Autorização por perfil

O que cada perfil (`ADMIN`, `PROVIDER`) acessa na API e no front, o prestador do
lançamento vindo do login, o admin padrão protegido e as configurações do próprio usuário.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo e escopo](#2-objetivo-e-escopo)
- [3. Regras](#3-regras)
  - [Acesso por perfil](#acesso-por-perfil)
  - [Lançamento](#lançamento)
  - [Usuários](#usuários)
  - [Prestador](#prestador)
  - [Banco](#banco)
- [4. Desenho do backend](#4-desenho-do-backend)
- [5. Desenho do frontend](#5-desenho-do-frontend)
- [6. Tarefas](#6-tarefas)
- [7. Documentação ao concluir](#7-documentação-ao-concluir)
- [8. ADRs a registrar](#8-adrs-a-registrar)

## 1. Status

- **Regras e desenho decididos (2026-11-06); nada implementado.** Hoje qualquer usuário
  logado acessa todos os endpoints ([ADR 0023](../adr/0023-autenticacao-jwt-resource-server.md)).
- Branch: `feature/authorization`, criada a partir da `feature/frontend-authentication`
  (o PR desta tem base nela até ela entrar na `dev`; os dois entram com "merge commit").

## 2. Objetivo e escopo

O `PROVIDER` passa a acessar só as telas e os dados dele; o `ADMIN` continua com acesso a
tudo.

**Entra:**
- backend: regras de acesso, restrição de dados do lançamento, `launchedByUserId`,
  endpoint de pacientes do lançamento, `/api/users/me`, reativação do usuário, admin
  padrão protegido, reativação do prestador exigindo usuário ativo;
- front: menu e rotas por papel e a tela de Configurações.

**Fica para as specs de cada tela** (as telas já nascem sobre esta autorização):
- **redesenho do lançamento:** usa o endpoint de pacientes do lançamento e esconde o
  prestador para o `PROVIDER`;
- **telas de Prestadores e Usuários:** decidido um **formulário só, em Prestadores**, que
  cria o usuário e o prestador juntos (na mesma transação) ou vincula um usuário existente
  (admin que também atende). A tela de Usuários fica para os admins e para inativar e
  reativar acessos;
- **dashboard do prestador** (ver o [`roadmap.md`](../roadmap.md)).

## 3. Regras

### Acesso por perfil

- **`ADMIN`:** acesso a tudo (todas as criações, inserções e alterações).
- **`PROVIDER`:** só as telas **Dashboard**, **Lançamentos** e **Configurações**. Na API,
  só o que essas telas usam:

| Endpoint | Restrição de dados para o `PROVIDER` |
|---|---|
| `POST /api/consultations` | só lança para o próprio prestador |
| `GET /api/consultations`, `GET /api/consultations/{id}` | só os lançamentos dele; o de outro prestador responde **404** (não revela que existe) |
| `DELETE /api/consultations/{id}` | só inativa os dele (404 para o de outro) |
| `GET /api/consultations/eligible-contracts`, `GET /api/consultations/session-balance` | nenhuma: apoio do lançamento |
| `GET /api/consultations/patients` (novo) | só pacientes **ativos**, com `id`, `name` e `birthDate` (sem CPF, contato e endereço) |
| `GET /api/techniques` (ativas) | nenhuma |
| `GET`/`PUT /api/users/me` (novos) | o próprio usuário |

- Fora dessa lista, o `PROVIDER` recebe **403** "Você não tem permissão para acessar este
  recurso." (já implementado no `ProblemDetailAccessDeniedHandler`). Ele não acessa a
  listagem de pacientes, contratos, prestadores, usuários nem o cadastro de preços.
- **Tela é diferente de endpoint:** esconder a tela não basta (princípio 3 do front); cada
  endpoint confere o perfil pelo token.
- O `PROVIDER` vê nos lançamentos dele o valor base (`baseValue`), o percentual e o
  repasse (`payoutValue`).
- **Sem prévia de preço:** o valor aparece só depois de lançar (resposta do `POST`).

### Lançamento

- **`PROVIDER` só lança para ele mesmo:** o prestador vem do vínculo do usuário logado.
  `providerId` opcional no request; informado e diferente do dele → **403**.
- **`ADMIN` lança para qualquer prestador:** `providerId` obrigatório (400 sem ele).
- **`PROVIDER` sem prestador vinculado** (usuário criado antes do prestador) → **403** "Seu
  usuário não está vinculado a um prestador. Procure o administrador.".
- **`launchedByUserId`:** todo lançamento grava quem lançou (pode ser diferente de quem
  atendeu, quando o admin lança por um prestador).
- **Listagem do `PROVIDER`:** o filtro `providerId` é sempre o dele, qualquer que seja o da
  URL.

### Usuários

- **Admin padrão:** o admin criado pelo `AdminInitializer` (a partir de `ADMIN_EMAIL` /
  `ADMIN_PASSWORD`) **não pode ser inativado nem mudar de perfil** (409). Os outros
  usuários, sim. Com isso, sempre existe um admin ativo, e não é preciso a regra do
  "último ADMIN".
  - Identificado por uma **marca no próprio usuário** (`defaultAdmin`), gravada pelo
    inicializador; não pelo e-mail (ele pode trocar o e-mail, e o `.env` pode mudar).
  - As variáveis só valem na **primeira subida**: depois, o banco é a fonte da verdade
    (trocar a senha no `.env` não muda nada).
- **Reativar usuário:** `PATCH /api/users/{id}/activate` (só `ADMIN`), 204; já ativo → 409.
  Mesmo padrão de paciente, prestador e técnica.
- **Configurações (o próprio usuário, os dois perfis, incluindo o admin padrão):** troca do
  **e-mail** e da **senha**, exigindo a **senha atual**.
  - Senha atual errada → **400** "Senha atual incorreta." (não 401: o front encerraria a
    sessão).
  - O perfil nunca muda por aqui.
  - Trocar o e-mail não invalida o token (o validador confere o usuário pelo `userId`).

### Prestador

- **Reativar o prestador exige usuário ativo** (400, a mesma regra e mensagem da criação).
  Substitui a regra de hoje ("não exige", em `Provider` no
  [`domain-model.md`](../domain-model.md)).

### Banco

- **Migrations editáveis até a versão estável:** enquanto não houver versão 1 no ar, as
  colunas novas entram **editando a V1** (não numa V4). Quem puxar a mudança zera o banco
  local (`docker compose down -v`). Depois da versão estável, volta a valer "migration
  aplicada nunca é editada" ([ADR 0021](../adr/0021-schema-versionado-flyway.md)).

## 4. Desenho do backend

**Regras de acesso centralizadas no `SecurityConfig`** (`auth/config`), numa tabela única:
login e Swagger públicos, os endpoints da tabela acima com `hasAnyRole("ADMIN",
"PROVIDER")` e, no fim, `anyRequest().hasRole("ADMIN")`. **Endpoint novo nasce restrito
ao ADMIN**: esquecer de configurar nega acesso, nunca abre. *Descartado:* `@PreAuthorize`
em cada método (regras espalhadas; esquecer abre o acesso).

**Quem está logado:**

| Peça | Módulo | O que faz |
|---|---|---|
| `CurrentUser(userId, role)` | `user` | record com `isAdmin()`; os controllers o recebem como parâmetro e o passam ao service |
| `CurrentUserArgumentResolver` | `auth` | `HandlerMethodArgumentResolver`: monta o `CurrentUser` a partir dos claims do token (`userId`, `roles`) |

Nenhum módulo importa o `auth`; o `consultation` já pode depender do `user`.

**`consultation`:**

| Arquivo | Mudança |
|---|---|
| `ConsultationEntity` | `launchedBy` (`@ManyToOne` → `UserEntity`, coluna `launched_by_user_id`, nullable para os lançamentos antigos) |
| `ConsultationRequestDTO` | `providerId` deixa de ser `@NotNull` (obrigatório só para o `ADMIN`, no service) |
| `ConsultationService` | recebe o `CurrentUser`: resolve o prestador (do vínculo, para o `PROVIDER`; do request, para o `ADMIN`), valida o `providerId` informado, grava o `launchedBy`, força o filtro da listagem e devolve 404 para o lançamento de outro prestador (busca e inativação) |
| `ProviderRepository` | `findByUserId` (prestador do usuário logado) |
| `ConsultationController` | `CurrentUser` nos métodos; `GET /api/consultations/patients` |
| `ConsultationPatientResponseDTO` (novo) | `id`, `name`, `birthDate` |
| `ConsultationResponseDTO` | `launchedByUserId` |

**`user`:**

| Arquivo | Mudança |
|---|---|
| `UserEntity` | `defaultAdmin`; fábrica/método que marca o admin padrão; `requireNotDefaultAdmin()` na inativação e na troca de perfil (409: "O administrador padrão não pode ser inativado." / "O perfil do administrador padrão não pode ser alterado."); `activate()` (409 se já ativo) |
| `UserService` | `activate(id)`; `findMe` / `updateMe` (confere a senha atual com o `PasswordEncoder`; 400 se errada); criação do admin padrão |
| `UserController` | `PATCH /{id}/activate`; `GET` e `PUT /me` |
| `UserSelfUpdateRequestDTO` (novo) | `email`, `newPassword` (opcional, mínimo 8), `currentPassword` (obrigatória) |
| `AdminInitializer` (`auth`) | cria o admin com a marca de admin padrão |

**`provider`:** `ProviderEntity`/`ProviderService` — reativar exige usuário ativo (400,
mesma mensagem da criação).

**Banco (editando a V1):** `users.default_admin BOOLEAN NOT NULL DEFAULT FALSE`;
`consultations.launched_by_user_id BIGINT NULL` com FK para `users`.

**Swagger:** descrições dos endpoints com o perfil exigido e o 403.

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `app/layout/navigationItems.js` | `PROVIDER`: Dashboard (desabilitado), Lançamentos, Configurações. `ADMIN`: Dashboard (desabilitado), Pacientes, Contratos, Preços, Lançamentos, Prestadores e Usuários (desabilitados até as telas existirem), Configurações |
| `app/router.jsx` + `ProtectedRoute` | cada rota declara os papéis; quem não tem o papel vai para a tela inicial dele. Só experiência de uso: a proteção é o 403 do backend |
| `domain/user/api/userApi.js` + hooks (novos) | `getMe`, `updateMe` |
| `domain/user/pages/SettingsPage.jsx` (nova) | e-mail, nova senha, confirmação da nova senha (Zod, só formato) e senha atual; mensagem da API no erro |
| `domain/auth/context/AuthContext.jsx` | atualizar o usuário guardado depois do `updateMe` (o menu mostra o e-mail novo) |

O 403 não muda nada no `apiClient`: a mensagem da API já chega no `error.message`, e só o
401 encerra a sessão.

## 6. Tarefas

**Backend** (`./mvnw -q compile` ao fim):
- [ ] V1: `default_admin` e `launched_by_user_id`.
- [ ] `CurrentUser` (`user`) e `CurrentUserArgumentResolver` (`auth`).
- [ ] `SecurityConfig`: tabela de acesso por perfil, `ADMIN` como padrão.
- [ ] `consultation`: prestador do login, `launchedBy`, restrição da listagem, busca e
      inativação, endpoint de pacientes.
- [ ] `user`: admin padrão protegido, `activate`, `/me`.
- [ ] `AdminInitializer`: marca de admin padrão.
- [ ] `provider`: reativação exige usuário ativo.
- [ ] Swagger.
- [ ] Teste manual pela API: cada endpoint com `ADMIN` e `PROVIDER`, lançamento com e sem
      `providerId`, `PROVIDER` sem prestador, admin padrão (inativar, mudar perfil), `/me`
      com senha atual certa e errada, reativação do usuário e do prestador.

**Frontend** (`npm run build` e `npm run lint` ao fim):
- [ ] Menu por papel.
- [ ] Rotas por papel.
- [ ] Tela de Configurações.
- [ ] Teste pela tela: menu e rotas de cada perfil, Configurações (e-mail, senha, senha
      atual errada).

## 7. Documentação ao concluir

- [`domain-model.md`](../domain-model.md): `User` (admin padrão, reativação, `/me`),
  `Provider` (reativação exige usuário ativo), `Consultation` ("Quem lança":
  prestador do login, `launchedByUserId`, restrição de dados; corrigir "o `SecurityConfig`
  ainda libera tudo").
- [`architecture.md`](../architecture.md): "Segurança" (tabela de acesso por perfil,
  `CurrentUser`); seção "Frontend" → "Rotas e sessão" (rotas por papel).
- [`development.md`](../development.md): o `.env` do admin só vale na primeira subida;
  exceção da regra das migrations até a versão estável.
- [`roadmap.md`](../roadmap.md): autorização concluída; antes da versão 1, consolidar as
  migrations e voltar a regra de nunca editá-las; próximos passos (telas de Prestadores e
  Usuários, redesenho do lançamento, dashboard).
- ADRs da seção seguinte e remoção desta spec.

## 8. ADRs a registrar

- **Regras de acesso centralizadas no `SecurityConfig`, com o `ADMIN` como padrão.**
  *Descartado:* `@PreAuthorize` em cada método.
- **Admin padrão protegido por uma marca no usuário** (não pode ser inativado nem mudar de
  perfil). *Descartados:* a regra do "último ADMIN ativo"; reconhecer o admin padrão pelo
  `ADMIN_EMAIL` (quebra se o e-mail ou a variável mudar).
- **Migrations editáveis até a versão estável** (substitui, até lá, o ponto "migration
  aplicada nunca é editada" do [ADR 0021](../adr/0021-schema-versionado-flyway.md)).
  *Descartado:* migration nova a cada mudança antes do MVP. *Consequência:* quem puxar uma
  migration editada zera o banco local.
