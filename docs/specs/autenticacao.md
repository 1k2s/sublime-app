# Spec — Autenticação

Login por e-mail e senha com token JWT, rotas da API protegidas e tela de login
ligada à API. As regras abaixo foram **decididas e ainda não implementadas**; ao
concluir, seguem para os documentos de referência (ver "Documentação ao concluir").

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo](#2-objetivo)
- [3. Regras](#3-regras)
  - [Login e token](#login-e-token)
  - [Senha](#senha)
  - [Rotas protegidas e erros](#rotas-protegidas-e-erros)
  - [Primeiro admin](#primeiro-admin)
  - [Sessão no frontend](#sessão-no-frontend)
  - [Em aberto](#em-aberto)
- [4. Desenho](#4-desenho)
  - [Backend](#backend)
  - [Contrato da API](#contrato-da-api)
  - [Frontend](#frontend)
- [5. Tarefas](#5-tarefas)
- [6. Documentação ao concluir](#6-documentação-ao-concluir)
- [7. ADRs a registrar](#7-adrs-a-registrar)

## 1. Status

- **Nada implementado.** Branch: `feature/autenticacao`, a partir da `dev` (depois do
  merge da `chore/monorepo`).
- Hoje: o `SecurityConfig` provisório (`config/SecurityConfig.java`) libera todas as
  rotas; a senha do `User` é gravada em texto puro; o login do front é mockado
  (`domain/auth/api/authApi.js`).

## 2. Objetivo

Só usuários cadastrados e **ativos** usam o sistema: login por e-mail e senha, token
JWT em todas as chamadas, senha guardada com hash e a tela de login ligada à API.

- **Depende de:** `user`.
- **Fora do escopo (feature seguinte, spec própria): autorização por perfil.** Quais
  endpoints cada perfil (`ADMIN`/`PROVIDER`) acessa, o que cada um vê (ex: o prestador
  só os próprios atendimentos), o `launchedByUserId` no `Consultation` e o prestador do
  lançamento vindo do login. Cada endpoint exige uma decisão de negócio; separar deixa
  esta feature menor. O token já sai com o perfil, então a autorização entra depois sem
  retrabalho.
- **Consequência aceita até a autorização:** qualquer usuário logado (inclusive
  `PROVIDER`) acessa todos os endpoints, como cadastrar usuários.

## 3. Regras

### Login e token

- **Login por e-mail e senha** (`POST /api/auth/login`), que devolve um token JWT, a
  data de expiração e os dados do usuário (`id`, `email`, `role`).
- **O token vale 8 horas** (um expediente), **sem refresh token**: venceu, o usuário faz
  login de novo.
  - *Descartado:* refresh token. Só compensa com sessões longas e acrescenta
    armazenamento, rotação e revogação de tokens.
- **Login recusado tem sempre a mesma mensagem** — e-mail inexistente, senha errada ou
  usuário inativo: **401 "E-mail ou senha inválidos."**
  - Motivo: mensagens diferentes revelam quais e-mails existem no sistema.
  - Custo aceito: o usuário inativo não sabe o motivo; o administrador explica.
- **Usuário inativo não faz login.** O soft delete do `User` (`active = false`) passa a
  bloquear o acesso.
- **Sem cadastro público:** usuários são criados pelo administrador
  (`POST /api/users`), como hoje.

### Senha

- **Guardada com hash BCrypt**, nunca em texto puro; nunca devolvida nas respostas
  (como hoje).
- **Mínimo de 8 caracteres**, sem outras exigências (maiúscula, símbolo...).
  - Motivo: a recomendação atual (NIST SP 800-63B) valoriza o tamanho, não a mistura de
    caracteres, que leva a senhas previsíveis.
- **Alterar o usuário não exige trocar a senha.** No `PUT /api/users/{id}` a senha passa
  a ser opcional: vazia ou ausente mantém a atual; preenchida, troca (com o mesmo
  mínimo de 8).
  - Motivo: com hash, a senha atual não pode voltar para o formulário; com a senha
    obrigatória, o admin teria de inventar uma senha nova para corrigir um e-mail.
- **Usuários já cadastrados** (banco local, senha em texto puro) não conseguem logar
  depois da mudança: são recriados pela API. Não há migration: a coluna `password`
  (`VARCHAR(255)`) já comporta o hash (60 caracteres).

### Rotas protegidas e erros

- **Públicas:** `POST /api/auth/login` e o Swagger (`/swagger-ui.html`,
  `/swagger-ui/**`, `/api-docs/**`).
- **Todas as demais exigem o token** no header `Authorization: Bearer <token>`.
- **Erros no formato `ProblemDetail`, em português** (como os demais da API):

| Situação | Status | `detail` |
|---|---|---|
| Login recusado | 401 | "E-mail ou senha inválidos." |
| Sem token, token inválido ou vencido | 401 | "Sessão inválida ou expirada. Faça login novamente." |
| Sem permissão para o recurso | 403 | "Você não tem permissão para acessar este recurso." |

O 403 só passa a acontecer com a autorização por perfil; o tratamento já fica pronto.

### Primeiro admin

Com todas as rotas protegidas, ninguém consegue criar o primeiro usuário pela API.

- **Ao subir, se não existir nenhum `ADMIN` ativo**, o sistema cria um com o e-mail e a
  senha das variáveis de ambiente `ADMIN_EMAIL` e `ADMIN_PASSWORD`, pelas mesmas regras
  do cadastro (hash, e-mail único, mínimo de 8).
- **Sem as variáveis**, a aplicação sobe e registra um aviso no log, sem criar ninguém.
- Existindo um `ADMIN` ativo, nada acontece (as variáveis são ignoradas).
- *Descartados:* migration com o hash da senha (o `INSERT` pula as regras do domínio,
  [ADR 0021](../adr/0021-schema-versionado-flyway.md)); deixar o `POST /api/users` aberto
  enquanto o banco estiver vazio (uma janela em que qualquer um cria um admin).

### Sessão no frontend

- **Token, usuário e expiração no `localStorage`**, como hoje (`sublime.authToken`,
  `sublime.authUser`).
- **A sessão termina** quando:
  - o usuário sai (logout);
  - o app é aberto com o token já vencido (pela `expiresAt` guardada);
  - a API responde 401 (garantia final, ex: token vencido no meio do uso).
- **Ao sair, o cache de dados (TanStack Query) é limpo**, para o próximo usuário do
  mesmo navegador não ver as listas do anterior.
- **Tela inicial por perfil:** `ADMIN` → `/patients`; `PROVIDER` → `/consultations`.
- **Papéis com os valores do backend:** `ADMIN` e `PROVIDER`.
- **Mensagens de erro da API** aparecem para o usuário (o `detail` do `ProblemDetail`),
  nunca a mensagem técnica do Axios ("Request failed with status code 401").
- A tela de login tem só e-mail e senha (sem "criar conta").

### Em aberto

- **Usuário inativado com o token ainda válido.** O token é verificado só pela
  assinatura e pela validade (sem consultar o banco), então um usuário inativado
  continua acessando até o token vencer — **no máximo 8 horas**. *Recomendação: aceitar.*
  A alternativa (consultar o `active` a cada requisição) custa uma ida ao banco por
  chamada e perde a principal vantagem do token. Se precisar de corte imediato no
  futuro, a solução é uma lista de tokens revogados.

## 4. Desenho

### Backend

**Novo módulo `auth`** (`br.com.senai.sublime_app.auth`), acima do `user`:

```
pricing          user
   |            /    \
   |      provider    auth     ← novo: depende só de user
   |         |
   |      patient
    \       /
    contract
        \
     consultation
```

O `user` continua sem importar nada do `auth`: recebe só o `PasswordEncoder`, que é
uma interface do Spring Security.

| Classe | Onde | Responsabilidade |
|---|---|---|
| `SecurityConfig` | `auth/config` | substitui o provisório de `config/`: rotas públicas e protegidas, resource server JWT com o conversor de perfis, `STATELESS`, CSRF desligado, CORS (o bean atual), entry point e access denied handler |
| `JwtProperties` | `auth/config` | `record` com `@ConfigurationProperties("jwt")`: `secret` e `expiration` (`Duration`). Recusa subir com segredo menor que 32 bytes (256 bits) |
| `JwtConfig` | `auth/config` | `JwtEncoder` (`NimbusJwtEncoder` + `ImmutableSecret`) e `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`) a partir de uma única `SecretKey` |
| `PasswordEncoder` e `AuthenticationManager` | `auth/config` | `BCryptPasswordEncoder`; `AuthenticationManager` declarado explicitamente (`ProviderManager` + `DaoAuthenticationProvider` com o `UserDetailsService` e o `PasswordEncoder`) |
| `AuthenticatedUser` | `auth/security` | `UserDetails` sobre o `UserEntity`: `getUsername()` = e-mail, autoridade `ROLE_<role>`, **`isEnabled()` = `active`** |
| `DatabaseUserDetailsService` | `auth/security` | `UserDetailsService`: busca por e-mail no `UserRepository` |
| `ProblemDetailAuthenticationEntryPoint` / `ProblemDetailAccessDeniedHandler` | `auth/security` | escrevem o 401/403 como `ProblemDetail` (tabela de erros acima) |
| `TokenService` | `auth/service` | só **gera** o token (a validação é do resource server) |
| `AuthService` | `auth/service` | login: autentica pelo `AuthenticationManager`, gera o token e monta a resposta |
| `AuthController` | `auth/controller` | `POST /api/auth/login`, com `@Tag`/`@Operation`/`@ApiResponses` em português |
| `LoginRequestDTO` / `LoginResponseDTO` | `auth/dto` | `record`s do contrato abaixo |
| `AdminInitializer` | `auth/config` | `ApplicationRunner` do "Primeiro admin", criando pelo `UserService` (não grava direto no repositório, para passar pelas regras) |

**Token (claims):** `iss` = `sublime-app`, `sub` = e-mail, `userId`, `roles` =
`["ADMIN"]` (lista), `iat`, `exp`. O conversor do resource server lê a claim `roles`
com o prefixo `ROLE_` (`JwtGrantedAuthoritiesConverter` com
`setAuthoritiesClaimName("roles")` e `setAuthorityPrefix("ROLE_")`), para o
`hasRole("ADMIN")` da autorização funcionar.

**Configuração** (`application.properties`): `jwt.secret=${JWT_SECRET}` (sem valor
padrão: a aplicação não sobe sem ele), `jwt.expiration=8h`,
`sublime.admin.email=${ADMIN_EMAIL:}` e `sublime.admin.password=${ADMIN_PASSWORD:}`.
Nenhum segredo no git.

**Erros de login:** `AuthenticationException` (`BadCredentialsException`,
`DisabledException`) no `GlobalExceptionHandler` → 401 com a mensagem única. O
`DaoAuthenticationProvider` confere se o usuário está ativo **antes** da senha, então
tratar as duas exceções igual é o que impede revelar um usuário inativo.

**Mudanças no `user`:**
- `UserService` recebe o `PasswordEncoder` e grava o hash na criação e na troca de
  senha; ganha a consulta "existe `ADMIN` ativo?" para o inicializador
  (`UserRepository.existsByRoleAndActiveTrue`).
- `UserRequestDTO` (criação): senha com `@Size(min = 8)`.
- Novo `UserUpdateRequestDTO` (alteração), com a senha opcional (padrão "DTO de
  alteração separado"); o `PUT` passa a usá-lo.
- `UserEntity`: `update(email, role)` e `changePassword(encodedPassword)` separados (a
  entidade recebe sempre o hash; o tamanho mínimo, que só existe na senha digitada,
  fica no DTO).

**Dependências (`pom.xml`):** saem `spring-boot-starter-security-oauth2-authorization-server`
e `spring-boot-starter-security-oauth2-client` (e os `-test` deles), que não servem para
este modelo; entram `spring-boot-starter-security`,
`spring-security-oauth2-resource-server` e `spring-security-oauth2-jose` (versões do BOM
do Boot). As três estão no cache local do Maven (4.1.1 / 7.1.1). O starter de resource
server do Boot 4.1.1 **não está** no cache e não é necessário (o `JwtDecoder` é nosso).

**Swagger (`OpenApiConfig`):** esquema de segurança `bearer` (JWT) aplicado a todos os
endpoints, para o botão "Authorize" funcionar; o login fica sem exigência de token.

### Contrato da API

`POST /api/auth/login`

```json
// request
{ "email": "admin@sublime.com", "password": "********" }

// 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-10-30T20:00:00Z",
  "user": { "id": 1, "email": "admin@sublime.com", "role": "ADMIN" }
}
```

- 400: campos inválidos (mapa `errors`).
- 401: "E-mail ou senha inválidos."

### Frontend

| Arquivo | Mudança |
|---|---|
| `shared/ui/Badge.jsx`, `PageHeader.module.css`, `Select.module.css` | **pré-requisito:** corrigir o build (arquivos cortados) |
| `shared/http/apiError.js` (novo) | `getApiErrorMessage(error, fallback)`: devolve o `detail` do `ProblemDetail` ou o `fallback` |
| `domain/auth/api/authApi.js` | troca o mock por `httpClient.post("/auth/login", { email, password })`; devolve `{ token, expiresAt, user }` |
| `domain/auth/context/AuthContext.jsx` | `signIn` guarda também a `expiresAt`; `readStoredUser` descarta a sessão vencida; `signOut` limpa o cache (`queryClient.clear()`) |
| `domain/auth/roles.js` | valores `ADMIN` e `PROVIDER` |
| `domain/auth/pages/LoginPage.jsx` | erro pela `getApiErrorMessage` |
| `app/router.jsx` | a rota `/` leva à tela inicial do perfil (`getHomePath`), no lugar do `/consultations` fixo |

O `httpClient` não muda: já envia o token e trata o 401.

## 5. Tarefas

**Backend** (compilar e revisar ao fim de cada bloco):
- [ ] Dependências no `pom.xml`; `./mvnw -q compile`.
- [ ] `user`: hash da senha, `@Size(min = 8)`, `UserUpdateRequestDTO`, `update`/`changePassword` na entidade, `existsByRoleAndActiveTrue`.
- [ ] `auth`: `JwtProperties`, `JwtConfig`, `PasswordEncoder`, `AuthenticationManager`, `AuthenticatedUser`, `DatabaseUserDetailsService`, `TokenService`, `AuthService`, `AuthController`, DTOs.
- [ ] `SecurityConfig` definitivo (remover o provisório de `config/`), conversor de perfis, entry point e access denied handler; `AuthenticationException` no `GlobalExceptionHandler`.
- [ ] `AdminInitializer`.
- [ ] Swagger com o esquema `bearer`.
- [ ] Teste manual pelo Swagger: login certo; senha errada; e-mail inexistente; usuário inativo (mesma mensagem); chamada sem token, com token inválido e com token vencido (401); criar e alterar usuário (com e sem senha); primeiro admin com e sem as variáveis.

**Frontend** (`npm run build` e `npm run lint` ao fim):
- [ ] Corrigir o build.
- [ ] `getApiErrorMessage`.
- [ ] `authApi.js` real.
- [ ] `AuthContext`: `expiresAt` e limpeza do cache.
- [ ] `roles.js` e tela inicial por perfil.
- [ ] Teste pela tela: login de cada perfil, senha errada, sessão vencida, logout seguido de outro login.

**Documentação:** ver a seção seguinte.

## 6. Documentação ao concluir

- [`domain-model.md`](../domain-model.md), seção `User`: senha com hash e mínimo de 8;
  usuário inativo sem acesso; senha opcional na alteração; primeiro admin.
- [`architecture.md`](../architecture.md): módulo `auth` no mapa de módulos; padrão de
  segurança (resource server, rotas públicas, token); 401/403 em "Formato dos erros";
  na seção "Frontend", "Rotas e sessão" (expiração, limpeza do cache, tela inicial por
  perfil).
- [`development.md`](../development.md): variáveis `JWT_SECRET`, `ADMIN_EMAIL` e
  `ADMIN_PASSWORD` (como definir no PowerShell), recriar os usuários de teste, login no
  Swagger ("Authorize"), e o usuário como primeiro passo da ordem de cadastro.
- [`roadmap.md`](../roadmap.md): estado atual e próximos passos (autorização por perfil).
- ADRs (seção seguinte) e remoção desta spec.

## 7. ADRs a registrar

- **Autenticação por JWT próprio com o Resource Server do Spring (HMAC).** O próprio
  sistema emite o token e o valida pelo `oauth2ResourceServer().jwt()`. *Alternativas
  descartadas:* filtro próprio (`OncePerRequestFilter`) lendo o token à mão (reimplementa
  o que o Spring já faz); servidor de autorização (Spring Authorization Server) ou
  provedor externo (complexidade sem necessidade para um sistema só); sessão com cookie
  (estado no servidor e CSRF); chave RSA (só compensa quando outro serviço valida o token
  sem poder assiná-lo). Inclui o primeiro admin por inicializador.
- **Sessão no frontend: token no `localStorage`, 8 horas, sem refresh token.**
  *Alternativas descartadas:* cookie `httpOnly` (protege o token de XSS, mas exige
  configuração de CSRF e `SameSite` e muda o `httpClient`); refresh token.
