# Spec — Tela de Configurações e senha forte

Novo visual da tela de Configurações (`/settings`): um cartão só, com a conta no topo e uma linha
por ajuste, no estilo das demais telas. Junto, a regra de senha forte em todas as senhas.

Iniciada em 2026-11-25.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo e escopo](#2-objetivo-e-escopo)
- [3. Regras](#3-regras)
- [4. Desenho do backend](#4-desenho-do-backend)
- [5. Desenho do frontend](#5-desenho-do-frontend)
- [6. Tarefas](#6-tarefas)
- [7. Casos de teste](#7-casos-de-teste)
- [8. Pendências e pontos abertos](#8-pendências-e-pontos-abertos)
- [9. Documentação ao concluir](#9-documentação-ao-concluir)
- [10. ADRs a registrar](#10-adrs-a-registrar)

## 1. Status

- **Branch:** `feature/estilo-tela-configuracoes`, a partir da `dev`. PR para a `dev`, com merge
  commit.
- **Implementado:** a tela na versão B da prévia e a senha forte no backend e no front (seção 3).
  Conferido por `./mvnw -q compile`, pelo `StrongPasswordValidatorTest` (unidade), por
  `npm run build` e `npm run lint`. **Não feitos:** os testes de integração (precisam do Docker)
  e o teste pela tela (seção 7).
- **Ajustes de 2026-11-26:** a senha atual passou para um modal aberto pelo "Salvar alterações"
  (seção 3) e os tons creme e areia da tela viraram azul (cabeçalho da conta, selo "Admin padrão",
  "Alterações não salvas", força "Média"). Conferidos por `npm run build` e `npm run lint`; não
  testados pela tela.
- **Falta:** os testes não feitos e a documentação de conclusão (seção 9).

## 2. Objetivo e escopo

Parte do item "Visual global em cada tela" do Frontend no [`roadmap.md`](../roadmap.md): a tela
de Configurações era um formulário solto num card, sem o visual das demais.

**Entra:** o layout da tela, o campo de senha com o botão de mostrar, a conferência e a barra
de força da nova senha e a **senha forte** (decidida em 2026-11-25) em todas as senhas
digitadas: cadastro de usuário, senha provisória, primeiro acesso, Configurações e o
`ADMIN_PASSWORD`. **Não mudam:** os endpoints, os campos e as demais regras.

**Fica para depois:** o `PasswordInput`, a conferência e a barra no cadastro de usuário e no
primeiro acesso (seção 8).

## 3. Regras

**Senha forte**

- **No mínimo 8 caracteres, com letra maiúscula, letra minúscula, número e caractere especial**
  (especial: o que não é letra, número nem espaço, ex: `! @ # $ -`). Letras acentuadas contam
  como maiúscula e minúscula. Vale no `POST /api/users`, na senha provisória do
  `PUT /api/users/{id}`, no `POST /api/auth/first-access`, no `PUT /api/users/me` e no
  `ADMIN_PASSWORD` (que passa pela validação do cadastro: inválida, a aplicação não sobe num
  banco sem admin padrão).
- Mensagem (400, no campo): "A senha deve ter no mínimo 8 caracteres, com letra maiúscula, letra
  minúscula, número e caractere especial." ("A nova senha..." no primeiro acesso e no `/me`).
- Senha vazia nas alterações continua significando "manter a atual".
- **Senhas antigas continuam valendo:** o login não confere o formato; a regra pega só as
  senhas novas.
- *Por quê:* pedido do usuário (2026-11-25). *Descartadas:* **só o tamanho** (a regra anterior,
  "o que protege é o tamanho", NIST SP 800-63B); **mínimo 6 com os critérios** (curto demais,
  mesmo com os critérios).

**Tela**

- **Um cartão só, em linhas, na largura da tela** (versão B da prévia de 2026-11-25; alinhado
  com o título, como as demais telas): no topo, a conta (avatar com as iniciais do e-mail, o
  e-mail salvo, o perfil e o selo "Admin padrão" quando `defaultAdmin`); abaixo, uma linha por
  ajuste, com a explicação à esquerda e os campos à direita (empilham na tela estreita).
  *Comparadas:* **A**, cartão da conta ao lado e seções com ícone; **C**, faixa no verde do menu
  e barra de salvar presa ao pé. A B é a mais sóbria e cresce com uma linha nova por ajuste.
- **A senha atual é pedida num modal** ("Confirme que é você", decidido em 2026-11-26), aberto
  pelo "Salvar alterações" só com o formulário válido (erros de e-mail ou de nova senha ficam nos
  campos e o modal não abre). No sucesso, o modal fecha e as senhas são limpas; num erro (ex:
  senha atual incorreta), continua aberto para tentar de novo; durante o salvamento, não fecha.
  *Antes:* uma linha destacada no fim do formulário, que ficava sempre à vista mesmo sem
  alteração a salvar.
- **Rodapé com o estado:** "Nenhuma alteração" / "Alterações não salvas". "Salvar alterações" e
  "Descartar" só se habilitam com o e-mail ou a nova senha alterados. "Descartar" volta aos
  valores salvos.
- **Mostrar e esconder a senha** em cada campo de senha (`PasswordInput`, compartilhado).
- **Conferência da nova senha** enquanto se digita: os quatro critérios da senha forte e
  "Confirmação igual à nova senha".
- **Barra de força** abaixo da nova senha, em três segmentos: **Fraca** (vermelho) abaixo de 8
  caracteres ou com até um dos outros critérios; **Média** (azul) com dois; **Forte** (verde)
  com os três. Só a forte é aceita; as outras mostram o quanto falta.

## 4. Desenho do backend

| Arquivo | Mudança |
|---|---|
| `shared/validation/StrongPassword` + `StrongPasswordValidator` (novos) | a regra; nula ou em branco é aceita (a obrigatoriedade é do `@NotBlank`) |
| `user/dto/UserRequestDTO` | `@StrongPassword` no lugar do `@Size(min = 8)` |
| `user/dto/UserUpdateRequestDTO`, `UserSelfUpdateRequestDTO` | `@StrongPassword` no campo, no lugar do `@AssertTrue` (`isPasswordValid`, `isNewPasswordValid`): o erro chega com o nome do campo |
| `auth/dto/FirstAccessRequestDTO` | `@StrongPassword` no lugar do `@Size(min = 8)` |
| `UserController`, `AuthController` | textos do Swagger |
| `application-test.properties`, `.env.example` | senha do admin no formato novo |

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `domain/auth/schemas/passwordRule.js` (novo) | a regra (`PASSWORD_CRITERIA`, `isStrongPassword`, `strongPassword` para o Zod), a mesma do backend; no `auth` porque todo módulo pode importá-lo |
| `settingsSchema`, `userSchema`, `firstAccessSchema` | a senha nova pelo `strongPassword` |
| `UserFormModal`, `FirstAccessForm` | placeholder com os critérios; o `UserFormModal` sem a tradução de `passwordValid` |
| `shared/components/Input` | `endSlot`: um controle à direita do campo |
| `shared/components/PasswordInput` (novo) | campo de senha com o botão de mostrar e esconder, sobre o `Input` |
| `domain/user/components/AccountSummary` (novo) | a faixa da conta no topo |
| `domain/user/components/PasswordRequirements` (novo) | a conferência da nova senha |
| `domain/user/components/PasswordStrength` (novo) + `display/passwordStrength.js` | a barra de força e o nível (`getPasswordStrength`) |
| `domain/user/components/SettingsForm` | as linhas e o rodapé com o estado; o "Salvar alterações" valida e abre o modal da senha atual |
| `domain/user/components/CurrentPasswordModal` (novo) | o modal da senha atual, sobre o `Modal` |
| `domain/user/pages/SettingsPage` | o cartão sem padding, com a conta e o formulário |

## 6. Tarefas

- [x] Prévia (versões A, B e C) e escolha (B).
- [x] `PasswordInput` e `endSlot` no `Input`.
- [x] Tela no novo layout; barra de força.
- [x] Senha forte no backend (`@StrongPassword`, teste de unidade) e no front (`passwordRule`).
- [x] Senha atual num modal; azul no lugar do creme e do areia (2026-11-26).
- [ ] Testes de integração (`./mvnw test`, com o Docker).
- [ ] Teste pela tela (seção 7).
- [ ] Conclusão: seção 9, `node scripts/check-docs.mjs`, spec removida.

## 7. Casos de teste

Pela interface, com o backend no ar.

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir Configurações como o admin padrão e como um prestador | faixa com o e-mail e o perfil; o selo "Admin padrão" só no primeiro |
| 2 | Abrir sem mexer | "Nenhuma alteração"; Salvar e Descartar desabilitados |
| 3 | Mudar o e-mail e clicar em Descartar | o e-mail volta; "Nenhuma alteração" |
| 4 | Nova senha com 5 caracteres e confirmação diferente | a conferência mostra os pendentes; ao salvar, os erros nos campos e o modal não abre |
| 4a | Digitar `abcdefgh`, depois `Abcdefg1`, depois `Abcdefg1!` | barra Fraca (1 segmento, vermelho), Média (2, azul), Forte (3, verde); critérios marcados um a um |
| 4b | Salvar com `Abcdefg1` (média) | erro no campo: a senha precisa dos quatro critérios |
| 4c | Mesmo caso pelo Swagger (`PUT /api/users/me`, `newPassword: "Abcdefg1"`) | 400 com a mensagem no campo `newPassword` |
| 5 | Olho de cada campo de senha | mostra e esconde o texto; o leitor de tela lê "Mostrar senha" / "Esconder senha" |
| 6 | Alterar o e-mail, Salvar e confirmar o modal com a senha atual vazia | "Informe a senha atual" no campo do modal |
| 6a | Abrir o modal e cancelar (botão, X ou Esc) | o modal fecha; as alterações continuam no formulário |
| 7 | Senha atual errada no modal | alerta "Senha atual incorreta."; o modal continua aberto |
| 8 | Trocar e-mail e senha forte e confirmar com a senha atual certa | "Dados atualizados."; o modal fecha; a faixa mostra o e-mail novo; senhas limpas; "Nenhuma alteração" |
| 9 | Janela estreita | as linhas empilham (explicação em cima, campos embaixo) |
| 10 | Cadastrar usuário com senha `senha1234` | erro no campo; com `Senha-1234`, cadastra |
| 11 | Editar usuário sem senha (só o e-mail) | salva: a vazia mantém a atual |
| 12 | Primeiro acesso com `senha1234` | erro no campo; com uma forte, entra |
| 13 | Login de um usuário com senha antiga fraca | entra normalmente |

## 8. Pendências e pontos abertos

- **`PasswordInput`, conferência e barra de força no cadastro de usuário e no primeiro acesso:**
  hoje só o placeholder mostra os critérios. Levar quando as telas forem mexidas (o login já
  usa o `PasswordInput` desde 2026-11-26).

## 9. Documentação ao concluir

- `domain-model.md`, User: a senha forte no lugar de "Mínimo de 8 caracteres, sem exigir
  maiúscula ou símbolo"; a senha provisória "mínimo de 8" pela regra nova.
- `development.md`, variáveis: o `ADMIN_PASSWORD` no formato novo.
- `architecture.md`: em "Camada `api/`", o exemplo `passwordValid` → `password` (não existe mais
  `@AssertTrue` de senha); em "Estrutura de pastas", o `PasswordInput` em `shared/components`.
- `roadmap.md`: na linha do `user`, "mínimo 8" pela senha forte; no Frontend, tirar Configurações
  do "Visual global em cada tela"; a seção 8 vira item.
- `specs/README.md`: tirar esta spec de "Em andamento".
- Descrição do PR: avisar que o `ADMIN_PASSWORD` de cada `.env` precisa do formato novo para
  subir num banco sem admin padrão.

## 10. ADRs a registrar

- **Senha forte** (seção 3): o formato, onde vale, as alternativas descartadas (só o tamanho,
  NIST; mínimo 6) e a consequência (senhas antigas continuam valendo). Substitui em parte o
  [ADR 0023](../adr/0023-autenticacao-jwt-resource-server.md) ("mínimo de 8 caracteres") e o
  [ADR 0025](../adr/0025-primeiro-acesso-com-senha-provisoria.md) (mínimo da nova senha).
- A tela: não se aplica (decisão de visual do dia a dia, sem ADR no frontend).
