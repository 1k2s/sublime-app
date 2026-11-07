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
- [7. Casos de teste](#7-casos-de-teste)
  - [Parte 1 — API (Swagger)](#parte-1--api-swagger)
  - [Parte 2 — Interface](#parte-2--interface)
- [8. Pendências e pontos abertos](#8-pendências-e-pontos-abertos)
- [9. Documentação ao concluir](#9-documentação-ao-concluir)
- [10. ADRs a registrar](#10-adrs-a-registrar)

## 1. Status

- **Código implementado (2026-11-06), backend e front; falta o teste manual** (seção 7) e a
  documentação de conclusão (seção 9). Nada commitado ainda.
- Conferido: `./mvnw -q compile` OK; `npm run build` OK; `npm run lint` sem erros (só o aviso
  antigo do `PatientWizard`, já no roadmap). **Não conferido:** a aplicação subindo (o Docker
  não estava rodando e não havia `backend/.env` na máquina) — o `validate` do Hibernate contra
  a V1 editada e o padrão `{id:\d+}` do `SecurityConfig` só são conferidos ao subir.
- **Para rodar em outra máquina:** `backend/.env` (a partir do `.env.example`; o admin do
  `.env` nasce como admin padrão), Docker rodando, **banco zerado** (`docker compose down -v`
  e `up -d`: a V1 foi editada) e `npm ci` em `frontend/` se não houver `node_modules`.
- Branch: `feature/authorization`, criada a partir da `feature/frontend-authentication`
  (o PR desta tem base nela até ela entrar na `dev`; os dois entram com "merge commit").
  Destino do PR em aberto (seção 8).

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
  reativar acessos (usa o `defaultAdmin` da resposta para esconder "Inativar" e a troca de
  perfil do admin padrão);
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
  recurso." (`ProblemDetailAccessDeniedHandler`). Ele não acessa a listagem de pacientes,
  contratos, prestadores, usuários nem o cadastro de preços.
- **Tela é diferente de endpoint:** esconder a tela não basta (princípio 3 do front); cada
  endpoint confere o perfil pelo token.
- O `PROVIDER` vê nos lançamentos dele o valor base (`baseValue`), o percentual e o
  repasse (`payoutValue`).
- **Sem prévia de preço:** o valor aparece só depois de lançar (resposta do `POST`).

### Lançamento

- **`PROVIDER` só lança para ele mesmo:** o prestador vem do vínculo do usuário logado.
  `providerId` opcional no request; informado e diferente do dele → **403** "Você só pode
  lançar atendimentos em seu próprio nome.".
- **`ADMIN` lança para qualquer prestador:** `providerId` obrigatório (400 "O prestador é
  obrigatório.").
- **`PROVIDER` sem prestador vinculado** (usuário criado antes do prestador) → **403** "Seu
  usuário não está vinculado a um prestador. Procure o administrador.". Vale também na
  listagem, na busca por id e na inativação (decidido na implementação): ele não tem
  lançamentos. O vínculo é conferido **antes** da busca por id, para o 403 não variar entre
  id existente e inexistente (o que revelaria quais ids existem).
- **`launchedByUserId`:** todo lançamento grava quem lançou (pode ser diferente de quem
  atendeu, quando o admin lança por um prestador). **Obrigatório** (`NOT NULL`): com a V1
  editada e o banco zerado, não há lançamentos antigos sem autor.
- **Listagem do `PROVIDER`:** o filtro `providerId` é sempre o dele, qualquer que seja o da
  URL.

### Usuários

- **Admin padrão:** o admin criado pelo `AdminInitializer` (a partir de `ADMIN_EMAIL` /
  `ADMIN_PASSWORD`) **não pode ser inativado nem mudar de perfil** (409: "O administrador
  padrão não pode ser inativado." / "O perfil do administrador padrão não pode ser
  alterado."). Os outros usuários, sim. Com isso, sempre existe um admin ativo, e não é
  preciso a regra do "último ADMIN".
  - Identificado por uma **marca no próprio usuário** (`defaultAdmin`), gravada pelo
    inicializador; não pelo e-mail (ele pode trocar o e-mail, e o `.env` pode mudar).
  - A troca de perfil só é recusada se o perfil **mudar**: o `PUT /api/users/{id}` manda o
    estado completo, e corrigir o e-mail do admin padrão (mantendo `ADMIN`) é permitido.
  - As variáveis só valem na **primeira subida** (o inicializador cria o admin padrão só se
    ainda não existir um): depois, o banco é a fonte da verdade (trocar a senha no `.env`
    não muda nada).
  - `defaultAdmin` vem na resposta dos usuários (`UserResponseDTO`), para a futura tela de
    Usuários esconder as ações proibidas.
- **Reativar usuário:** `PATCH /api/users/{id}/activate` (só `ADMIN`), 204; já ativo → 409
  "O usuário já está ativo.". Mesmo padrão de paciente, prestador e técnica.
  - Consequência aceita: um token emitido antes da inativação e ainda dentro das 8 horas
    volta a valer com a reativação (mesmo usuário, mesmo perfil).
- **Configurações (o próprio usuário, os dois perfis, incluindo o admin padrão):** troca do
  **e-mail** e da **senha**, exigindo a **senha atual**.
  - Senha atual errada → **400** "Senha atual incorreta." (não 401: o front encerraria a
    sessão).
  - A senha atual é conferida **antes** do e-mail duplicado (409), para quem não tem a
    senha não usar o endpoint para descobrir quais e-mails existem.
  - Nova senha vazia ou ausente mantém a atual; preenchida, mínimo de 8.
  - O perfil nunca muda por aqui (o request nem tem o campo).
  - Trocar o e-mail não invalida o token (o validador confere o usuário pelo `userId`).

### Prestador

- **Reativar o prestador exige usuário ativo** (400 "O usuário está inativo.", a mesma
  regra e mensagem da criação). Já ativo continua dando 409, conferido antes. Substitui a
  regra de hoje ("não exige", em `Provider` no [`domain-model.md`](../domain-model.md)).

### Banco

- **Migrations editáveis até a versão estável:** enquanto não houver versão 1 no ar, as
  colunas novas entram **editando a V1** (não numa V4). Quem puxar a mudança zera o banco
  local (`docker compose down -v`). Depois da versão estável, volta a valer "migration
  aplicada nunca é editada" ([ADR 0021](../adr/0021-schema-versionado-flyway.md)). O
  cabeçalho da V1 já diz isso.

## 4. Desenho do backend

**Regras de acesso centralizadas no `SecurityConfig`** (`auth/config`), numa tabela única:
login e Swagger públicos, os endpoints da tabela acima com `hasAnyRole(ADMIN, PROVIDER)`
(rota a rota, sem curinga) e, no fim, `anyRequest().hasRole(ADMIN)`. **Endpoint novo nasce
restrito ao ADMIN**: esquecer de configurar nega acesso, nunca abre. O id dos lançamentos é
`{id:\d+}` (só números): um `{id}` simples casaria com qualquer rota nova de um segmento,
que nasceria aberta ao `PROVIDER`. *Descartado:* `@PreAuthorize` em cada método (regras
espalhadas; esquecer abre o acesso); `/api/consultations/**` (abriria endpoints futuros).

**Quem está logado:**

| Peça | Módulo | O que faz |
|---|---|---|
| `CurrentUser(userId, role)` | `user/domain` | record com `isAdmin()`; os controllers o recebem como parâmetro e o passam ao service |
| `CurrentUserArgumentResolver` | `auth/security` | `HandlerMethodArgumentResolver`: monta o `CurrentUser` a partir dos claims do token (`userId`, `roles`); sem token JWT → `IllegalStateException` (erro de programação) |
| `CurrentUserWebConfig` | `auth/config` | registra o resolver no Spring MVC |
| `OpenApiConfig` | `config/swagger` | `SpringDocUtils...addRequestWrapperToIgnore(CurrentUser.class)`: o Swagger não mostra `userId`/`role` como parâmetros |

Nenhum módulo importa o `auth`; o `consultation` já pode depender do `user`.

**403 de regra que depende dos dados:** `ForbiddenException` nova (`shared/exception`),
mapeada para 403 no `GlobalExceptionHandler`. O 403 por perfil (endpoint não permitido)
continua saindo pelo `ProblemDetailAccessDeniedHandler`. O service não usa a
`AccessDeniedException` do Spring para o `consultation` não depender do Spring Security.

**`consultation`:**

| Arquivo | Mudança |
|---|---|
| `ConsultationEntity` | `launchedBy` (`@ManyToOne` LAZY → `UserEntity`, coluna `launched_by_user_id`, obrigatório); construtor recebe o `launchedBy`, com `validateLaunchedBy` |
| `ConsultationRequestDTO` | `providerId` sem `@NotNull` (obrigatório só para o `ADMIN`, no service) |
| `ConsultationService` | `create`, `search`, `findById`, `deactivate` recebem o `CurrentUser`. `resolveProvider` (+ `requireProviderId`, `getOwnProviderOrThrow`, `requireOwnProvider`); `launchedBy` por `userRepository.getReferenceById` (sem consulta: o token já garantiu o usuário); filtro forçado na listagem; `getConsultationVisibleToOrThrow` (404 para o lançamento de outro prestador, com a mesma mensagem do inexistente); `findPatients` |
| `ProviderRepository` | `findByUserId` (prestador do usuário logado) |
| `PatientRepository` | `findByActiveTrueOrderByNameAsc` |
| `ConsultationController` | `CurrentUser` nos métodos; `GET /api/consultations/patients` |
| `ConsultationPatientResponseDTO` (novo) | `id`, `name`, `birthDate` |
| `ConsultationResponseDTO` | `launchedByUserId` |

**`user`:**

| Arquivo | Mudança |
|---|---|
| `UserEntity` | `defaultAdmin`; fábrica `defaultAdmin(email, encodedPassword)`; `requireNotDefaultAdmin()` na inativação e `requireRoleUnchangedIfDefaultAdmin()` no `update`; `activate()` com `requireInactive()`; `changeEmail()` (Configurações) |
| `UserRepository` | `existsByDefaultAdminTrue` (substitui o `existsByRoleAndActiveTrue`, removido) |
| `UserService` | `createDefaultAdmin`, `existsDefaultAdmin`, `activate`, `findMe`, `updateMe` (`requireCurrentPassword` com o `PasswordEncoder`; 400 se errada); checagem de e-mail duplicado num método só (`requireEmailAvailable` / `requireEmailAvailableForUser`) |
| `UserController` | `PATCH /{id}/activate`; `GET` e `PUT /me` |
| `UserSelfUpdateRequestDTO` (novo) | `email`, `newPassword` (opcional, mínimo 8), `currentPassword` (obrigatória) |
| `UserResponseDTO` | `defaultAdmin` |
| `AdminInitializer` (`auth`) | cria o admin padrão (`createDefaultAdmin`) só se ainda não existir um (`existsDefaultAdmin`) |

**`provider`:** `ProviderEntity.activate()` chama o `validateActiveUser(user)` da criação.

**Banco (editando a V1):** `` `user`.default_admin BIT(1) NOT NULL DEFAULT b'0' `` (o
padrão dos booleanos do schema, que o `validate` do Hibernate espera);
`consultation.launched_by_user_id BIGINT NOT NULL` com a FK
`fk_consultation_launched_by_user` para `` `user` ``.

**Swagger:** seção "Perfis" na descrição geral (o 403 explicado uma vez, como o 401 já é);
"**Perfis:** ADMIN e PROVIDER" nos endpoints compartilhados, com a restrição de dados; 403
nos endpoints do lançamento; 409 do admin padrão no `PUT`/`DELETE` de usuário; 400 na
reativação do prestador. Removidos dois textos desatualizados (técnicas `/all` "quando a
autenticação for implementada"; reativação do prestador "não exige usuário ativo").

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `app/layout/navigationItems.js` | lista única filtrada por papel. `PROVIDER`: Dashboard (desabilitado), Lançamentos, Configurações. `ADMIN`: Dashboard (desabilitado), Pacientes, Contratos, Preços, Lançamentos, Prestadores e Usuários (desabilitados até as telas existirem), Configurações |
| `domain/auth/components/ProtectedRoute.jsx` | prop opcional `roles`: sem o papel → tela inicial do papel dele (`getHomePath`). Só experiência de uso: a proteção é o 403 do backend |
| `app/router.jsx` | rotas agrupadas por papel: `ProtectedRoute roles={[ADMIN]}` (patients, pricing, providers, contracts) e `roles={[ADMIN, PROVIDER]}` (consultations, settings). *Descartado:* `handle: { roles }` + `useMatches()` |
| `domain/user/api/userApi.js` + `useMe.js` (novos) | `getMe`, `updateMe`; `useMe` (`["users", "me"]`) e `useUpdateMe` (invalida `["users"]`) |
| `domain/user/schemas/settingsSchema.js` (novo) | e-mail, nova senha (vazia ou 8+), confirmação igual, senha atual obrigatória — só formato |
| `domain/user/components/SettingsForm.jsx` (novo) | o formulário; entrega à page os valores e `resetPasswords`, para limpar as senhas só depois do sucesso |
| `domain/user/pages/SettingsPage.jsx` (nova) | busca pelo `useMe`, salva pelo `useUpdateMe`; sucesso: atualiza a sessão, limpa as senhas e mostra "Dados atualizados."; erro: `showError` com a mensagem da API |
| `domain/auth/context/AuthContext.jsx` | `updateUser({ email })`: atualiza a sessão (estado e `sublime.authUser`) mantendo a forma `{ id, email, role }`; o menu mostra o e-mail novo |
| `shared/utils/alert.js` | `showSuccess(message)` (novo, sem precedente: telas que não fecham nem navegam depois de salvar) |

O 403 não muda nada no `apiClient`: a mensagem da API já chega no `error.message`, e só o
401 encerra a sessão.

## 6. Tarefas

**Backend** (`./mvnw -q compile` OK):
- [x] V1: `default_admin` e `launched_by_user_id`.
- [x] `CurrentUser` (`user`) e `CurrentUserArgumentResolver` (`auth`).
- [x] `SecurityConfig`: tabela de acesso por perfil, `ADMIN` como padrão.
- [x] `consultation`: prestador do login, `launchedBy`, restrição da listagem, busca e
      inativação, endpoint de pacientes.
- [x] `user`: admin padrão protegido, `activate`, `/me`.
- [x] `AdminInitializer`: marca de admin padrão.
- [x] `provider`: reativação exige usuário ativo.
- [x] Swagger.
- [ ] Teste manual pela API (seção 7, parte 1).

**Frontend** (`npm run build` e `npm run lint` OK):
- [x] Menu por papel.
- [x] Rotas por papel.
- [x] Tela de Configurações.
- [ ] Teste pela tela (seção 7, parte 2).

**Conclusão:**
- [ ] Documentação (seção 9) e ADRs (seção 10), depois dos testes.
- [ ] Commit e PR (destino em aberto, seção 8).

## 7. Casos de teste

**Pré-requisitos:** banco zerado e `.env` preenchido. Usuários: o admin padrão (do `.env`),
um segundo `ADMIN`, dois `PROVIDER` com prestador (**A** e **B**) e um `PROVIDER` **sem**
prestador. Catálogo mínimo para lançar (grupo, técnica, plano Avulso e preço). Pacientes: um
ativo e um inativo.

### Parte 1 — API (Swagger)

**Subida e Swagger**

| # | Caso | Esperado |
|---|---|---|
| 1 | Subir a aplicação com o banco zerado | Sobe sem erro (Flyway + `validate`); log "Admin padrão criado" |
| 2 | Swagger: descrição geral | Seção "Perfis" presente |
| 3 | Swagger: `GET /api/users/me` e endpoints de lançamento | `CurrentUser` **não** aparece como parâmetro (`userId`, `role`) |

**Acesso por perfil**

| # | Caso | Esperado |
|---|---|---|
| 4 | Qualquer rota sem token | 401 |
| 5 | PROVIDER: `GET /api/patients`, `POST /api/users`, `GET /api/techniques/all`, `GET /api/contracts` | 403 "Você não tem permissão para acessar este recurso." |
| 6 | PROVIDER: `GET /api/techniques` | 200 |
| 7 | PROVIDER: `GET /api/consultations/abc` (id não numérico) | 403 (para o ADMIN, 400) |
| 8 | ADMIN: endpoints acima | 200 |

**Lançamento (`POST /api/consultations`)**

| # | Caso | Esperado |
|---|---|---|
| 9 | ADMIN sem `providerId` | 400 "O prestador é obrigatório." |
| 10 | ADMIN com `providerId` = B | 201; `providerId` = B; `launchedByUserId` = id do admin |
| 11 | PROVIDER A sem `providerId` | 201; `providerId` = A; `launchedByUserId` = usuário de A |
| 12 | PROVIDER A com `providerId` = A | 201 |
| 13 | PROVIDER A com `providerId` = B | 403 "Você só pode lançar atendimentos em seu próprio nome." |
| 14 | PROVIDER sem prestador | 403 "Seu usuário não está vinculado a um prestador. Procure o administrador." |

**Leituras e inativação**

| # | Caso | Esperado |
|---|---|---|
| 15 | PROVIDER A: `GET /api/consultations`, depois `?providerId=B` | Nos dois, só os de A |
| 16 | ADMIN: `GET /api/consultations`, depois `?providerId=B` | Todos; depois, só os de B |
| 17 | PROVIDER A: `GET /{id de B}` e `GET /{id inexistente}` | 404 com a **mesma** mensagem |
| 18 | PROVIDER A: `DELETE /{id de B}` | 404 |
| 19 | PROVIDER A: `DELETE /{id próprio}` duas vezes | 204, depois 409 |
| 20 | PROVIDER sem prestador: listagem, `GET` de id existente e de inexistente | 403 nos três |
| 21 | Os dois perfis: `GET /api/consultations/patients` | 200; só ativos (o inativo não aparece), por nome, só `id`, `name` e `birthDate` |
| 22 | PROVIDER: `eligible-contracts` e `session-balance` | 200 |

**Usuários**

| # | Caso | Esperado |
|---|---|---|
| 23 | `GET /api/users` | Admin padrão com `defaultAdmin: true`; os demais `false` |
| 24 | `DELETE` do admin padrão | 409 "O administrador padrão não pode ser inativado." |
| 25 | `PUT` do admin padrão com `role: PROVIDER` | 409 "O perfil do administrador padrão não pode ser alterado." |
| 26 | `PUT` do admin padrão com e-mail novo e `role: ADMIN` | 200 |
| 27 | Trocar `ADMIN_EMAIL`/`ADMIN_PASSWORD` no `.env` e reiniciar | Nenhum usuário novo; o admin padrão não muda |
| 28 | `DELETE` do segundo ADMIN | 204 |
| 29 | `PATCH /api/users/{id}/activate`: inativo, ativo, inexistente | 204 (login volta a funcionar); 409; 404 |
| 30 | PROVIDER: `PATCH /api/users/{id}/activate` | 403 |

**Configurações (`/api/users/me`)**

| # | Caso | Esperado |
|---|---|---|
| 31 | Os dois perfis: `GET /me` | 200 com os próprios dados |
| 32 | `PUT /me` com e-mail novo e senha atual certa | 200; o mesmo token continua funcionando; login pelo e-mail novo |
| 33 | `PUT /me` com `newPassword` preenchida | 200; login só com a senha nova |
| 34 | `PUT /me` com `newPassword` vazia | 200; a senha não muda |
| 35 | `PUT /me` com senha atual errada | 400 "Senha atual incorreta." (não 401) |
| 36 | `PUT /me` sem `currentPassword` | 400 com `errors.currentPassword` |
| 37 | `PUT /me` com `newPassword` de 5 caracteres | 400 com `errors.newPasswordValid` |
| 38 | `PUT /me` com e-mail de outro usuário, senha certa | 409 |
| 39 | `PUT /me` com e-mail de outro usuário, senha **errada** | 400 "Senha atual incorreta." |
| 40 | `PUT /me` com o admin padrão | 200 |

**Prestador**

| # | Caso | Esperado |
|---|---|---|
| 41 | Inativar o usuário do prestador A e reativar o prestador A | 400 "O usuário está inativo." |
| 42 | Reativar o usuário de A e depois o prestador A | 204, 204 |
| 43 | Reativar um prestador já ativo | 409 |

### Parte 2 — Interface

**Menu e rotas**

| # | Caso | Esperado |
|---|---|---|
| 44 | Login como ADMIN | Abre em `/patients`; menu: Dashboard (desab.), Pacientes, Contratos, Preços, Lançamentos, Prestadores (desab.), Usuários (desab.), Configurações |
| 45 | Login como PROVIDER | Abre em `/consultations`; menu: Dashboard (desab.), Lançamentos, Configurações |
| 46 | PROVIDER digita na URL `/patients`, `/contracts`, `/pricing`, `/providers` | Volta para `/consultations` |
| 47 | ADMIN digita na URL `/consultations` e `/settings` | Abrem normalmente |
| 48 | Sem login, abrir `/settings` | Vai para `/login`; depois do login, volta para `/settings` |
| 49 | Rota inexistente (`/xyz`) | Página de não encontrado |

**Configurações**

| # | Caso | Esperado |
|---|---|---|
| 50 | Abrir Configurações (os dois perfis) | E-mail atual preenchido; campos de senha vazios |
| 51 | Salvar com e-mail inválido ou senha atual vazia | Erro no campo, sem chamar a API |
| 52 | Nova senha com menos de 8 caracteres | Erro no campo |
| 53 | Confirmação diferente da nova senha | Erro "A confirmação não confere com a nova senha" |
| 54 | Senha atual errada | Alerta "Senha atual incorreta."; **continua logado** |
| 55 | E-mail de outro usuário | Alerta com a mensagem do 409 |
| 56 | Troca do e-mail com sucesso | Alerta "Dados atualizados."; o menu mostra o e-mail novo; campos de senha limpos; após F5, o e-mail novo continua no menu |
| 57 | Troca da senha com sucesso, logout e login | Login só com a senha nova |
| 58 | ADMIN padrão troca o próprio e-mail | Funciona como nos demais |

## 8. Pendências e pontos abertos

- **Testar** (seção 7) e, se algo falhar, corrigir antes da documentação.
- **Destino do PR:** o `development.md` diz "PR para a `dev`", mas os merges recentes foram na
  `main`. Decidir antes de abrir o PR.
- **Fora desta feature, para decidir se entram no roadmap:**
  - `npm ci` acusou **7 vulnerabilidades** nas dependências do front (4 moderadas, 3 altas).
  - **Erros por campo da API não chegam ao formulário:** o `architecture.md` (seção
    "Camada `api/`") diz que o `errors` do `ProblemDetail` aparece no `FormField`, mas o
    `apiClient` só lê o `detail`, e nenhum componente lê o `errors`. Um 400 de validação do
    DTO aparece como "Campos da requisição inválidos." no alerta.

## 9. Documentação ao concluir

- [`domain-model.md`](../domain-model.md): `User` (admin padrão, reativação, `/me`), a frase
  "`User`, `Contract` e `Consultation` não têm reativação" (o `User` passa a ter), o "Primeiro
  admin" (agora admin padrão, só na primeira subida), `Provider` (reativação exige usuário
  ativo), `Consultation` (atributo `launchedByUserId`; "Quem lança": prestador do login,
  restrição de dados; corrigir "o `SecurityConfig` ainda libera tudo"), diagrama ER
  (`USER.defaultAdmin`, `CONSULTATION.launchedByUserId`).
- [`architecture.md`](../architecture.md): "Segurança" (tabela de acesso por perfil,
  `CurrentUser`, "Perfis" sem o "ainda não aplicada"); "Formato dos erros" (403 do
  `ForbiddenException` e o do perfil, sem o "ainda não usada"); seção "Frontend" → "Rotas e
  sessão" (rotas por papel, `ProtectedRoute` com `roles`, `updateUser`).
- [`development.md`](../development.md): o `.env` do admin só vale na primeira subida (admin
  padrão); exceção da regra das migrations até a versão estável.
- [`roadmap.md`](../roadmap.md): autorização concluída; antes da versão 1, consolidar as
  migrations e voltar a regra de nunca editá-las; próximos passos (telas de Prestadores e
  Usuários, redesenho do lançamento, dashboard); no front, `auth` com rotas por papel e a
  tela de Configurações; os pontos da seção 8 que você decidir registrar.
- ADRs da seção seguinte e remoção desta spec.

## 10. ADRs a registrar

- **Regras de acesso centralizadas no `SecurityConfig`, com o `ADMIN` como padrão** (rota a
  rota, id numérico). *Descartados:* `@PreAuthorize` em cada método; curinga por módulo.
- **Admin padrão protegido por uma marca no usuário** (não pode ser inativado nem mudar de
  perfil). *Descartados:* a regra do "último ADMIN ativo"; reconhecer o admin padrão pelo
  `ADMIN_EMAIL` (quebra se o e-mail ou a variável mudar).
- **Migrations editáveis até a versão estável** (substitui, até lá, o ponto "migration
  aplicada nunca é editada" do [ADR 0021](../adr/0021-schema-versionado-flyway.md)).
  *Descartado:* migration nova a cada mudança antes do MVP. *Consequência:* quem puxar uma
  migration editada zera o banco local.
