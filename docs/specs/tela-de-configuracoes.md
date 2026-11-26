# Spec — Tela de Configurações

Novo visual da tela de Configurações (`/settings`): um cartão só, com a conta no topo e uma linha
por ajuste, no estilo das demais telas.

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
- **Implementado** (só frontend): a versão B da prévia (seção 3). Conferido por `npm run build` e
  `npm run lint`; **teste pela tela não feito** (seção 7).
- **Falta:** o teste pela tela e a documentação de conclusão (seção 9).

## 2. Objetivo e escopo

Parte do item "Visual global em cada tela" do Frontend no [`roadmap.md`](../roadmap.md): a tela
de Configurações era um formulário solto num card, sem o visual das demais.

**Entra:** o layout da tela, o campo de senha com o botão de mostrar e a conferência da nova
senha enquanto se digita. **Não muda:** a API (`GET`/`PUT /api/users/me`), as regras e as
mensagens.

**Fica para depois:** o `PasswordInput` no login e no primeiro acesso (seção 8).

## 3. Regras

Decisões de visual e de comportamento.

- **Um cartão só, em linhas** (versão B da prévia de 2026-11-25): no topo, a conta (avatar com
  as iniciais do e-mail, o e-mail salvo, o perfil e o selo "Admin padrão" quando
  `defaultAdmin`); abaixo, uma linha por ajuste, com a explicação à esquerda e os campos à
  direita (empilham na tela estreita). *Comparadas:* **A**, cartão da conta ao lado e seções com
  ícone; **C**, faixa no verde do menu e barra de salvar presa ao pé. A B é a mais sóbria e
  cresce com uma linha nova por ajuste.
- **A senha atual vai para o fim**, numa linha destacada em areia ("Confirme que é você"),
  junto do salvar: ela confirma qualquer alteração. Antes ficava entre o e-mail e a nova senha.
- **Rodapé com o estado:** "Nenhuma alteração" / "Alterações não salvas". "Salvar alterações" e
  "Descartar" só se habilitam com o e-mail ou a nova senha alterados; digitar só a senha atual
  não conta. "Descartar" volta aos valores salvos.
- **Mostrar e esconder a senha** em cada campo de senha (`PasswordInput`, compartilhado).
- **Conferência da nova senha** enquanto se digita: "Pelo menos 8 caracteres" e "Confirmação
  igual à nova senha". São as regras de formato do `settingsSchema` (o mínimo vem da mesma
  constante, `MIN_PASSWORD_LENGTH`); a senha atual quem confere é a API.

## 4. Desenho do backend

Não se aplica.

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `shared/components/Input` | `endSlot`: um controle à direita do campo |
| `shared/components/PasswordInput` (novo) | campo de senha com o botão de mostrar e esconder, sobre o `Input` |
| `domain/user/components/AccountSummary` (novo) | a faixa da conta no topo |
| `domain/user/components/PasswordRequirements` (novo) | a conferência da nova senha |
| `domain/user/components/SettingsForm` | as linhas, a senha atual no fim e o rodapé com o estado |
| `domain/user/schemas/settingsSchema.js` | `MIN_PASSWORD_LENGTH` |
| `domain/user/pages/SettingsPage` | o cartão sem padding, com a conta e o formulário |

## 6. Tarefas

- [x] Prévia (versões A, B e C) e escolha (B).
- [x] `PasswordInput` e `endSlot` no `Input`.
- [x] Tela no novo layout.
- [ ] Teste pela tela (seção 7).
- [ ] Conclusão: seção 9, `node scripts/check-docs.mjs`, spec removida.

## 7. Casos de teste

Pela interface, com o backend no ar.

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir Configurações como o admin padrão e como um prestador | faixa com o e-mail e o perfil; o selo "Admin padrão" só no primeiro |
| 2 | Abrir sem mexer | "Nenhuma alteração"; Salvar e Descartar desabilitados |
| 3 | Mudar o e-mail e clicar em Descartar | o e-mail volta; "Nenhuma alteração" |
| 4 | Nova senha com 5 caracteres e confirmação diferente | a conferência mostra os dois pendentes; ao salvar, os erros nos campos |
| 5 | Olho de cada campo de senha | mostra e esconde o texto; o leitor de tela lê "Mostrar senha" / "Esconder senha" |
| 6 | Salvar sem a senha atual | "Informe a senha atual" no campo |
| 7 | Senha atual errada | alerta "Senha atual incorreta." |
| 8 | Trocar e-mail e senha com a senha atual certa | "Dados atualizados."; a faixa mostra o e-mail novo; senhas limpas; "Nenhuma alteração" |
| 9 | Janela estreita | as linhas empilham (explicação em cima, campos embaixo) |

## 8. Pendências e pontos abertos

- **`PasswordInput` no login e no primeiro acesso:** os dois formulários usam o `Input` com
  `type="password"`, sem o botão de mostrar. Levar quando forem mexidos.
- O tom marrom do texto em areia (`#7a5a32`, no selo e no "Alterações não salvas") está escrito
  direto no CSS; segue a melhoria "Cores em variáveis CSS em todos os componentes" do roadmap.

## 9. Documentação ao concluir

- `architecture.md`, "Estrutura de pastas": o `PasswordInput` na lista de `shared/components`.
- `roadmap.md`, Frontend: tirar Configurações do "Visual global em cada tela"; a seção 8 vira
  item.
- `specs/README.md`: tirar esta spec de "Em andamento".

## 10. ADRs a registrar

Não se aplica: decisão de visual do dia a dia, sem ADR no frontend (CLAUDE.md).
