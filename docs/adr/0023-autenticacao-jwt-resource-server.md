# 0023 — Autenticação por JWT próprio, validado pelo resource server do Spring

- **Status:** aceito (em parte substituído por [0027](0027-admin-padrao-protegido.md): o primeiro admin)
- **Data:** 2026-11-05

## Contexto

A API ficava aberta (`SecurityConfig` provisório com `permitAll`) e a senha do `User` era
gravada em texto puro. O sistema é um só (uma API e um front da clínica), com dois perfis
(`ADMIN`, `PROVIDER`), e o front chama a API de outra origem (`localhost:5173`).

## Decisão

- **O próprio sistema emite o token** no login (`POST /api/auth/login`, e-mail e senha) e o
  **valida pelo resource server do Spring** (`oauth2ResourceServer().jwt()`) em cada
  requisição. Assinatura **HMAC SHA-256** com um segredo único (`JWT_SECRET`, mínimo de
  256 bits, fora do git).
- **Token de 8 horas, sem refresh token.** Claims: `iss`, `sub` (e-mail), `userId`, `roles`
  (lista), `iat`, `exp`.
- **Cada requisição confere o usuário no banco** (`ActiveUserTokenValidator`): o token é
  recusado se o usuário não existe mais, foi inativado ou teve o perfil alterado. Inativar
  ou mudar o perfil corta o acesso na hora.
- **Senha com BCrypt**, mínimo de 8 caracteres.
- **Login recusado com uma mensagem só** (e-mail inexistente, senha errada e usuário
  inativo), para não revelar quais e-mails existem.
- **Primeiro admin por inicializador:** ao subir sem nenhum `ADMIN` ativo, a aplicação cria
  um com `ADMIN_EMAIL`/`ADMIN_PASSWORD`, pelo `UserService` (mesmas regras do cadastro).
- Sem sessão no servidor (`STATELESS`) e sem CSRF (o token vai no header, não em cookie).

## Alternativas descartadas

- **Filtro próprio lendo o token à mão** (`OncePerRequestFilter`): reimplementa o que o
  resource server já faz (assinatura, validade, cabeçalho, erros).
- **Servidor de autorização** (Spring Authorization Server) ou **provedor externo**:
  complexidade de OAuth2 completo para um sistema só, sem outros clientes.
- **Sessão com cookie:** estado no servidor e proteção contra CSRF.
- **Chave RSA (par de chaves):** só compensa quando outro serviço valida o token sem poder
  emiti-lo.
- **Refresh token:** só compensa com sessões longas; acrescenta armazenamento, rotação e
  revogação.
- **Aceitar que o usuário inativado continue acessando até o token vencer** (até 8 horas) e
  **lista de tokens revogados:** decidido o corte imediato; a lista seria estado a mais, e a
  consulta ao banco continuaria existindo.
- **Primeiro admin por migration** (o `INSERT` pularia as regras do domínio,
  [ADR 0021](0021-schema-versionado-flyway.md)) ou **cadastro aberto enquanto o banco
  estiver vazio** (uma janela em que qualquer um cria um admin).

## Consequências

- Uma consulta por chave primária a cada requisição (irrelevante na escala da clínica; se
  pesar, um cache curto).
- Trocar o `JWT_SECRET` invalida todos os tokens emitidos (todos fazem login de novo).
- Usuários criados antes da mudança (senha em texto puro) não conseguem logar: são
  recriados ou têm a senha trocada pela API.
- A **autorização por perfil** fica para uma feature própria: até lá, qualquer usuário
  logado acessa todos os endpoints. O token já carrega o perfil (`ROLE_ADMIN`,
  `ROLE_PROVIDER`), então ela entra sem retrabalho.
