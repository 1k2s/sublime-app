# Spec — Cadastro de Prestadores e Primeiro Acesso

Tela de gestão de prestadores (`/providers`, exclusiva para `ADMIN`) em formato wizard de
3 etapas (seguindo o padrão de `PatientWizard`) e fluxo obrigatório de definição de senha
no primeiro acesso sem novos atributos em `UserEntity`.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo](#2-objetivo)
- [3. Regras](#3-regras)
  - [Primeiro acesso com senha provisória](#primeiro-acesso-com-senha-provisória)
  - [Cadastro e gestão de prestadores (`/providers`)](#cadastro-e-gestão-de-prestadores-providers)
- [4. Desenho](#4-desenho)
  - [Backend](#backend)
  - [Contrato da API](#contrato-da-api)
  - [Frontend](#frontend)
- [5. Tarefas](#5-tarefas)
- [6. Documentação ao concluir](#6-documentação-ao-concluir)
- [7. ADRs a registrar](#7-adrs-a-registrar)

## 1. Status

- **Em andamento.** Branch: `feature/frontend-provider`, a partir da `dev`.
- Hoje:
  - No backend, `user`, `provider` e `auth` já estão implementados, mas toda senha criada
    em `POST /api/users` ou alterada em `PUT /api/users/{id}` já permite login imediato
    sem exigir troca no primeiro acesso.
  - No frontend, `domain/provider/pages/ProviderPage.jsx` é apenas um placeholder,
    `domain/user/` está vazio e o item "Prestadores" ainda não consta no menu lateral
    (`navigationItems.js`).

## 2. Objetivo

Permitir que o administrador (`ADMIN`) cadastre, consulte, edite, inative e reative
prestadores (`Provider`) juntamente com sua conta de acesso (`User`) na rota `/providers`,
usando o mesmo padrão visual em etapas (`Stepper` + `PreviewCard`) do módulo de pacientes,
e garantir que o prestador defina sua própria senha pessoal no primeiro login (em
conformidade com a LGPD e sem adicionar colunas/atributos em `UserEntity`).

- **Depende de:** `user`, `provider`, `auth`.

## 3. Regras

### Primeiro acesso com senha provisória

- **Senha provisória no cadastro e no reset pelo admin ([ADR 0025](../adr/0025-primeiro-acesso-com-senha-provisoria.md)):**
  - Todo usuário criado pelo `POST /api/users` ou que tenha a senha preenchida no
    `PUT /api/users/{id}` recebe o hash BCrypt prefixado com `FIRST_ACCESS:` no campo
    `password` (`FIRST_ACCESS:<hash_bcrypt>`), sem novos atributos em `UserEntity` nem
    migration no banco.
  - Exceção: o primeiro admin criado pelo `AdminInitializer` (variáveis `ADMIN_EMAIL` e
    `ADMIN_PASSWORD`) nasce com hash definitivo (sem prefixo), podendo logar direto.
- **Transição automática no login (`POST /api/auth/login`):**
  - Não há botão manual de "Primeiro Acesso" na tela de login.
  - Quando o usuário tenta logar com `email` e `password` e seu registro possui o prefixo
    `FIRST_ACCESS:`:
    - se estiver inativo ou se a senha digitada não corresponder ao hash provisório, a API
      responde `401 "E-mail ou senha inválidos."` (mesma mensagem padrão);
    - se estiver ativo e a senha provisória estiver correta, a API **não emite token** e
      responde `403` (`ProblemDetail` com `detail: "Primeiro acesso pendente. Defina sua
      nova senha para continuar."` e `firstAccessRequired: true`).
  - Ao receber `firstAccessRequired: true`, a `LoginPage` transiciona automaticamente para
    a tela de definição de nova senha (`FirstAccessForm`), mantendo em memória o `email` e
    a `provisionalPassword` informados.
- **Definição da senha pessoal (`POST /api/auth/first-access`):**
  - Endpoint público que recebe `{ email, provisionalPassword, newPassword }`.
  - Valida formato (`email` válido, `provisionalPassword` não vazia, `newPassword` com
    mínimo de 8 caracteres).
  - Confere se o usuário existe, está ativo, está com primeiro acesso pendente e se
    `provisionalPassword` bate com o hash provisório (senão `401 "E-mail ou senha
    inválidos."`).
  - Exige que `newPassword` seja diferente da `provisionalPassword` (`400
    BusinessRuleException`: `"A nova senha deve ser diferente da senha provisória."`).
  - Grava o hash BCrypt definitivo (sem prefixo) e devolve `200 OK` com
    `LoginResponseDTO` (`token`, `expiresAt`, `user`), iniciando a sessão do usuário.

### Cadastro e gestão de prestadores (`/providers`)

- **Acesso exclusivo do `ADMIN`:**
  - O item "Prestadores" (`/providers`) aparece no menu lateral (`navigationItems.js`)
    apenas para o papel `ADMIN`.
  - Na `ProviderPage`, caso um usuário com papel diferente de `ADMIN` acesse `/providers`
    pela URL, é redirecionado para a sua tela inicial (`getHomePath(user?.role)`).
- **Listagem, filtros e detalhes:**
  - Lista todos os prestadores (`GET /api/providers`, ativos e inativos) enriquecidos com
    os dados do usuário vinculado (`GET /api/users`: `email`, `role`, `active`).
  - Filtros em memória por busca textual (nome ou e-mail) e por status (`ACTIVE`,
    `INACTIVE`, `ALL`), seguindo o padrão de `PatientPage`.
  - Modal de detalhes (`ProviderDetailsModal`) com dados do prestador, percentuais de
    comissão, dados da conta de acesso, botão para alternar status (ativar/inativar) e
    botão para abrir a edição.
- **Formulário em 3 etapas (`ProviderWizard` + `ProviderPreviewCard`):**
  - Segue o mesmo padrão estrutural e visual de `PatientWizard`: `Stepper` no topo,
    card de etapa à esquerda e `ProviderPreviewCard` à direita atualizando em tempo real
    via `useWatch`.
  - **Etapa 1 — Dados do prestador (`ProviderDataStep`):**
    - `name` (obrigatório);
    - `commissionPercentage` (padrão `40`, entre `0` e `100`, até 2 casas decimais);
    - `referralCommissionPercentage` (padrão `45`, entre `0` e `100`, até 2 casas
      decimais, maior ou igual ao `commissionPercentage`).
  - **Etapa 2 — Conta de acesso (`ProviderAccessStep`):**
    - `email` (obrigatório, formato de e-mail);
    - `role` (`PROVIDER` por padrão, ou `ADMIN` para administrador que também atende);
    - `password` (senha provisória: obrigatória no cadastro com mínimo de 8 caracteres;
      opcional na edição — em branco mantém a senha atual, preenchida redefine uma nova
      senha provisória exigindo troca no próximo login);
    - Na edição, permite também ajustar o status (`ACTIVE` / `INACTIVE`) do prestador.
  - **Etapa 3 — Confirmação (`ProviderConfirmationStep`):**
    - Exibe o resumo dos dados do prestador, comissões e conta de acesso antes de
      confirmar a gravação.
    - No cadastro (`create`): cria primeiro o usuário (`POST /api/users`) e em seguida o
      prestador (`POST /api/providers` com o `userId` retornado).
    - Na edição (`edit`): atualiza o usuário (`PUT /api/users/{userId}`), atualiza o
      prestador (`PUT /api/providers/{id}`) e, se o status tiver sido alterado, chama
      `DELETE /api/providers/{id}` (inativar) ou `PATCH /api/providers/{id}/activate`
      (reativar).

## 4. Desenho

### Backend

| Classe | Onde | Mudança / Responsabilidade |
|---|---|---|
| `UserEntity` | `user/domain` | Constante `FIRST_ACCESS_PREFIX = "FIRST_ACCESS:"`; fábrica estática `withProvisionalPassword(email, encodedPassword, role)` (ou método `markPasswordAsProvisional`); `changeToProvisionalPassword(encodedPassword)`; `isFirstAccessPending()`; `extractPasswordHash()` |
| `UserService` | `user/service` | `create(...)` grava senha provisória (`FIRST_ACCESS:` + hash); `createDefinitiveAdmin(...)` (usado pelo `AdminInitializer`) grava hash definitivo sem prefixo; `update(...)` grava senha provisória quando `dto.hasPassword()` for verdadeiro |
| `AdminInitializer` | `auth/config` | Usa a criação com senha definitiva para o primeiro admin |
| `AuthenticatedUser` | `auth/security` | `getPassword()` devolve `user.extractPasswordHash()`, permitindo validar a senha provisória com o `PasswordEncoder` |
| `FirstAccessRequiredException` | `auth/exception` (ou `shared/exception`) | Lançada no `POST /api/auth/login` quando a senha provisória está correta mas `user.isFirstAccessPending()` é `true` |
| `GlobalExceptionHandler` | `shared/exception` | Trata `FirstAccessRequiredException` devolvendo `403 Forbidden` (`ProblemDetail` com `detail` e propriedade `firstAccessRequired = true`) |
| `FirstAccessRequestDTO` | `auth/dto` | `record` com `email` (`@NotBlank`, `@Email`), `provisionalPassword` (`@NotBlank`) e `newPassword` (`@NotBlank`, `@Size(min = 8)`) |
| `AuthService` | `auth/service` | Em `login(...)`, se `user.isFirstAccessPending()`, lança `FirstAccessRequiredException` antes de emitir o token; novo método `completeFirstAccess(FirstAccessRequestDTO)` que valida a senha provisória, impede repetir a mesma senha, grava o hash definitivo e retorna `LoginResponseDTO` |
| `AuthController` | `auth/controller` | Novo endpoint `POST /api/auth/first-access` (`@SecurityRequirements` vazio) |
| `SecurityConfig` | `auth/config` | Libera `POST /api/auth/first-access` junto com `POST /api/auth/login` |

### Contrato da API

`POST /api/auth/login` (quando o usuário está em primeiro acesso e acerta a senha provisória):

```json
// 403 Forbidden (ProblemDetail)
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Primeiro acesso pendente. Defina sua nova senha para continuar.",
  "instance": "/api/auth/login",
  "firstAccessRequired": true
}
```

`POST /api/auth/first-access`:

```json
// request
{
  "email": "prestador@sublime.com",
  "provisionalPassword": "senhaProvisoria123",
  "newPassword": "minhaNovaSenhaPessoal"
}

// 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-11-07T04:00:00Z",
  "user": { "id": 2, "email": "prestador@sublime.com", "role": "PROVIDER" }
}
```

- `400`: campos inválidos (`errors`) ou `newPassword` igual à `provisionalPassword` (`detail`: `"A nova senha deve ser diferente da senha provisória."`).
- `401`: `"E-mail ou senha inválidos."` (usuário inexistente, inativo, sem pendência de primeiro acesso ou senha provisória incorreta).

### Frontend

| Arquivo | Mudança |
|---|---|
| `domain/auth/api/authApi.js` | Adiciona `completeFirstAccess({ email, provisionalPassword, newPassword })` (`POST /api/auth/first-access`) |
| `domain/auth/api/useFirstAccess.js` (novo) | Hook `useFirstAccess` com `useMutation`, chamando `signIn` no `onSuccess` |
| `domain/auth/schemas/firstAccessSchema.js` (novo) | Validação Zod de `newPassword` (mín. 8 caracteres) e `confirmPassword` (confirmação igual) |
| `domain/auth/components/FirstAccessForm.jsx` + `.module.css` (novo) | Formulário de definição da nova senha exibido automaticamente quando o login retorna `firstAccessRequired: true` |
| `domain/auth/pages/LoginPage.jsx` | Detecta `error?.response?.data?.firstAccessRequired === true` no erro do login, guarda `{ email, provisionalPassword }` no estado da página e renderiza o `FirstAccessForm` |
| `domain/user/api/userApi.js` e `useUsers.js` (novos) | `listUsers`, `getUserById`, `createUser`, `updateUser`, `deactivateUser` e respectivos hooks TanStack Query |
| `domain/provider/api/providerApi.js` e `useProviders.js` (novos) | `listProviders`, `getProviderById`, `createProvider`, `updateProvider`, `deactivateProvider`, `activateProvider` e hooks TanStack Query combinando prestador + usuário |
| `domain/provider/schemas/providerSchema.js` (novo) | Schema Zod para o wizard (criação e edição, validando comissões e regra `referralCommissionPercentage >= commissionPercentage`) |
| `domain/provider/components/ProviderFilters.jsx`, `ProviderTable.jsx`, `ProviderDetailsModal.jsx` (novos) | Listagem, busca/filtro por status, ação de editar com ícone de lápis (`Pencil`) e modal de detalhes |
| `domain/provider/components/wizard/ProviderWizard.jsx`, `ProviderDataStep.jsx`, `ProviderAccessStep.jsx`, `ProviderConfirmationStep.jsx`, `ProviderPreviewCard.jsx` (novos) | Fluxo de cadastro e edição em 3 etapas com prévia em tempo real |
| `domain/provider/pages/ProviderPage.jsx` + `.module.css` | Página completa de prestadores restrita ao perfil `ADMIN` |
| `app/layout/navigationItems.js` | Inclui o item "Prestadores" (`/providers`, ícone `UserCheck` / `Stethoscope`, `roles: [ROLES.ADMIN.value]`) |

## 5. Tarefas

**Backend:**
- [x] `UserEntity` e `UserService`: prefixo `FIRST_ACCESS:`, métodos de domínio e distinção entre senha provisória (API) e senha definitiva (`AdminInitializer`).
- [x] `AuthenticatedUser`, `FirstAccessRequiredException`, `FirstAccessRequestDTO`, `AuthService`, `AuthController`, `SecurityConfig` e `GlobalExceptionHandler`.
- [x] Compilar o backend (`./mvnw -q compile`).

**Frontend:**
- [x] Fluxo de primeiro acesso em `domain/auth/` (`authApi.js`, `useFirstAccess.js`, `firstAccessSchema.js`, `FirstAccessForm.jsx`, `LoginPage.jsx`).
- [x] Camada `api/` de `domain/user/` e `domain/provider/`.
- [x] Componentes de listagem, modal e wizard em 3 etapas em `domain/provider/` + item no menu para `ADMIN` (`navigationItems.js`).
- [x] Validar `npm.cmd run lint` e `npm.cmd run build`.

## 6. Documentação ao concluir

- [`domain-model.md`](../domain-model.md), seção `User`: senha provisória na criação/redefinição e troca obrigatória no primeiro acesso ([ADR 0025](../adr/0025-primeiro-acesso-com-senha-provisoria.md)).
- [`architecture.md`](../architecture.md): `403` de primeiro acesso em "Formato dos erros", rota pública `POST /api/auth/first-access` em "Segurança" e transição automática no login em "Rotas e sessão".
- [`roadmap.md`](../roadmap.md): atualizar estado de `auth`, `user` e `provider` no backend e no frontend.

## 7. ADRs a registrar

- **[ADR 0025 — Primeiro acesso com senha provisória no próprio hash e troca automática no login](../adr/0025-primeiro-acesso-com-senha-provisoria.md).**
