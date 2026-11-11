# Spec — Tela de Usuários

Tela do `ADMIN` para criar, alterar, inativar e reativar os acessos ao sistema (e-mail,
senha provisória e perfil), sobre a API de `User` que já existe.

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

- **Tela implementada e ligada à API (2026-11-10); falta o teste manual** pela interface.
- Branch: `feature/user-page`, criada a partir da `dev` (depois do PR #39, da
  autorização). Base do PR: `dev`.

## 2. Objetivo e escopo

Primeira metade da decisão "Usuários e Prestadores em telas separadas" (ver "Próximos
passos" no [`roadmap.md`](../roadmap.md)): o admin cria o usuário aqui e, depois, vincula
o usuário a um prestador na tela de Prestadores. Depende só do módulo `user` (e do `auth`,
para os papéis).

**Entra:** listagem com busca e filtros, cadastro, alteração (com redefinição opcional da
senha), inativação e reativação, e as proteções do admin padrão na tela.

**Fica para depois:**
- o ajuste do wizard de Prestadores para vincular um usuário existente, em vez de criar o
  usuário junto (spec própria);
- os erros por campo da API no formulário (pendência geral do front, no roadmap).

## 3. Regras

Nenhuma regra nova: a tela mostra as regras de `User` do
[`domain-model.md`](../domain-model.md) ("Acesso ao sistema", "Senha" e "Admin padrão").
O que a tela faz com cada uma:

| Regra da API | Na tela |
|---|---|
| Senha criada ou redefinida pelo admin é provisória | campo "Senha provisória" / "Nova senha provisória", com o aviso de que o usuário define a dele no primeiro login |
| Na alteração, senha vazia mantém a atual | Zod aceita vazio na edição; o envio troca vazio por `null` |
| Inativar, mudar o perfil ou redefinir a senha cortam a sessão | aviso no formulário de edição e na confirmação da inativação |
| Admin padrão não pode ser inativado nem mudar de perfil (409) | pelo `defaultAdmin` da resposta: sem "Inativar" no modal de detalhes, perfil só para leitura no formulário, aviso no modal |
| Reativar devolve o acesso (`PATCH /api/users/{id}/activate`) | "Reativar usuário" no modal de detalhes, com confirmação |
| E-mail duplicado (409), validações do DTO (400) | o `detail` da API no alerta (`showError`) |

## 4. Desenho do backend

Não se aplica: os endpoints de `/api/users` já existem (todos só do `ADMIN`).

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `domain/user/api/userApi.js`, `useUsers.js` | `activateUser` e `useActivateUser` (o resto da camada já existia) |
| `domain/user/pages/UserPage.jsx` | lista pelo `useUsers`, filtros em memória (`filterUsers`), mutations com `showError` no `onError`; o modal de detalhes lê o usuário da lista pelo id, para mostrar o estado novo depois de inativar ou reativar (como o `PatientPage`) |
| `domain/user/components/UserTable.jsx`, `UserFilters.jsx` | tabela (e-mail, perfil, status) e filtros (busca por e-mail, perfil e status) |
| `domain/user/components/UserDetailsModal.jsx` | detalhes, inativar e reativar com confirmação; badge e aviso do admin padrão, sem "Inativar" para ele |
| `domain/user/components/UserFormModal.jsx` | cadastro e alteração (e-mail, perfil, senha provisória); perfil só para leitura no admin padrão |
| `domain/user/schemas/userSchema.js` | formato do formulário: e-mail, perfil e senha (mínimo de 8; vazia na alteração) |
| `app/router.jsx` | rota `/users` no grupo só `ADMIN` |
| `app/layout/navigationItems.js` | item "Usuários" habilitado |

## 6. Tarefas

- [x] Ligar a página à API (listar, criar, alterar, inativar).
- [x] Reativar (`activateUser`, `useActivateUser`, botão no modal).
- [x] Admin padrão: esconder "Inativar" e travar o perfil.
- [x] Textos da senha provisória.
- [x] Rota e menu.
- [x] `npm run build` e `npm run lint`.
- [ ] Teste manual pela interface (seção 7).
- [ ] Conclusão (seção 9).

## 7. Casos de teste

**Pré-requisitos:** backend rodando com banco zerado, login com o admin padrão
(`ADMIN_EMAIL`).

| # | Caso | Esperado |
|---|---|---|
| 1 | `ADMIN` abre "Usuários" no menu | lista com o admin padrão (badge "Administrador padrão"); filtro inicial "Ativos" |
| 2 | `PROVIDER` abre `/users` pela URL | volta para a tela inicial dele (`/consultations`); o menu não mostra "Usuários" |
| 3 | Cadastrar `PROVIDER` com senha de 8+ caracteres | modal fecha, usuário aparece na lista |
| 4 | Cadastrar com e-mail já usado | alerta "Já existe um usuário cadastrado com o e-mail: ..." (409); o modal continua aberto |
| 5 | Cadastrar com senha de 7 caracteres ou e-mail inválido | erro no campo (Zod), sem chamar a API |
| 6 | Login com o usuário novo | a tela troca para a definição da senha (primeiro acesso) |
| 7 | Editar o e-mail com a senha em branco | salva; o usuário continua logando com a senha que já tinha |
| 8 | Editar preenchendo a senha | o usuário logado cai no próximo request (401); no próximo login, primeiro acesso de novo |
| 9 | Mudar o perfil de um usuário logado | a sessão dele cai no próximo request |
| 10 | Inativar (com confirmação) | badge "Inativo", botão vira "Reativar usuário"; some do filtro "Ativos"; a sessão dele cai |
| 11 | Reativar (com confirmação) | badge "Ativo"; o usuário volta a logar |
| 12 | Detalhes do admin padrão | sem "Inativar"; aviso de que não pode ser inativado nem mudar de perfil |
| 13 | Editar o admin padrão | perfil só para leitura; e-mail e senha alteráveis |
| 14 | Busca por parte do e-mail, filtro por perfil e por status | a lista filtra sem nova requisição |

## 8. Pendências e pontos abertos

Nenhum.

## 9. Documentação ao concluir

- `architecture.md`: já atualizado nesta branch ("Rotas e sessão": rota `/users` e menu).
- `roadmap.md`: na conclusão, tirar a tela de Usuários dos próximos passos e marcar o teste
  manual; a linha do `user` no "Estado atual" do front já foi atualizada.
- `domain-model.md`: não muda (nenhuma regra nova).
- Remover esta spec e a linha dela no [`README.md`](README.md).

## 10. ADRs a registrar

Não se aplica: nenhuma decisão grande de frontend.
