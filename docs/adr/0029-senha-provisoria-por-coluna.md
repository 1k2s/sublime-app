# 0029 — Senha provisória marcada por uma coluna, não por prefixo no hash

- **Status:** aceito
- **Data:** 2026-11-07

## Contexto

O [ADR 0025](0025-primeiro-acesso-com-senha-provisoria.md) definiu o primeiro acesso com
senha provisória e guardou esse estado como um prefixo (`FIRST_ACCESS:`) no próprio campo
`password`, para não criar coluna nem migration. Com as migrations editáveis até a versão
estável ([ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md)), esse motivo deixou de
valer.

## Decisão

- **Coluna `password_change_required`** (`BIT(1) NOT NULL DEFAULT b'0'`) em `user`, no campo
  `passwordChangeRequired` da `UserEntity`.
  - `withProvisionalPassword` e `changeToProvisionalPassword` gravam o hash e ligam o campo;
  - `changePassword` (senha definitiva: primeiro acesso ou Configurações) o desliga;
  - `isFirstAccessPending()` lê o campo.
- O campo `password` guarda sempre o hash BCrypt puro.
- **O fluxo do primeiro acesso do ADR 0025 não muda** (403 no login, troca obrigatória em
  `POST /api/auth/first-access`). Este ADR substitui só a forma de guardar o estado.

## Alternativas descartadas

- **Manter o prefixo no campo `password`:** mistura a credencial com um estado do usuário,
  e todo código que lê a senha precisa lembrar de tirar o prefixo; esquecer não dá erro de
  compilação, só faz a conferência da senha falhar. O padrão de mercado é um campo próprio
  (ex: "o usuário deve alterar a senha no próximo logon" do Active Directory e do Microsoft
  Entra, o desafio `NEW_PASSWORD_REQUIRED` do AWS Cognito).

## Consequências

- Saem a constante `FIRST_ACCESS_PREFIX` e o `extractPasswordHash()`; o `AuthenticatedUser`
  e o `UserService` usam o `getPassword()` direto.
- Uma coluna a mais em `user`, criada editando a `V1`. As consequências do ADR 0025 sobre a
  `V1` intacta e a entidade sem atributo novo deixam de valer.
- Melhoria anotada no roadmap: expiração da senha provisória.
