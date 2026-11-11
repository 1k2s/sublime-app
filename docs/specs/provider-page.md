# Spec — Tela de Prestadores (vincular usuário existente)

A tela de Prestadores passa a cadastrar o prestador **vinculando um usuário já existente**
(criado na tela de Usuários), num modal, sem regra de negócio nem percentual fixo no front.

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

- **Backend pronto:** o `POST /api/providers` recebe o `userId` de um usuário existente e os
  dois percentuais opcionais (vazio = padrão da clínica); o `PUT` recebe nome e os dois
  percentuais, obrigatórios. Ver `Provider` no [`domain-model.md`](../domain-model.md#provider).
- **Front na `dev`:** listagem padronizada (ver "Telas de listagem" no
  [`architecture.md`](../architecture.md#telas-de-listagem)) e cadastro/edição num wizard de 3
  etapas que **cria o usuário junto** e, na edição, altera também o e-mail, o perfil, a senha e
  o status do usuário.
- **Front na branch:** implementado (seção 5); build e lint sem erros. **Falta:** os casos de
  teste (seção 7) e a conclusão (seção 9).
- **Branch:** `feature/provider-page`, a partir da `dev`. PR para a `dev`.
- **Referência descartada como merge:** a branch `feature/providers-page` (Victor) tem o mesmo
  fluxo (seleção de usuário, percentuais opcionais, modal), mas parte de uma base antiga (pasta
  `shared/ui`, que não existe mais), não chama a API e confere o perfil na page. Dela vêm só as
  ideias.

## 2. Objetivo e escopo

Deixar a tela de Prestadores funcional de ponta a ponta no fluxo decidido em 2026-11-07
(Usuários e Prestadores em telas separadas: primeiro o usuário, depois o prestador). Depende
dos módulos `user` (lista de usuários) e `provider`.

**Entra:**

- cadastro com seleção do usuário (ativo, sem prestador, perfil `PROVIDER` ou `ADMIN`);
- percentuais opcionais no cadastro, aplicados pelo backend quando vazios;
- edição só do que é do prestador (nome e percentuais);
- modal no lugar do wizard;
- erros por campo da API (`applyApiFieldErrors`);
- remoção das regras e dos valores fixos do front (o 40/45 e a regra "indicação ≥ padrão").

**Fica para depois:**

- **Categorias de prestador** (Padrão, Estagiário...) com o par de percentuais: já está nas
  ideias do roadmap. Até lá, a exceção é digitar os percentuais no cadastro.
- **Testes automatizados** da `ProviderEntity`: pausados até as telas funcionarem (decidido em
  2026-11-11).

## 3. Regras

**R1. O cadastro vincula um usuário existente.** Um campo de seleção lista os usuários
**ativos, sem prestador**, dos perfis `PROVIDER` e `ADMIN` (o admin que também atende é um
prestador válido, decidido em 2026-11-11). Cada opção mostra `e-mail (Perfil)`.

- *Por quê:* criar o usuário junto deixava um usuário sem prestador quando a segunda chamada
  falhava (o wizard chamava `createUser` e depois `createProvider`). Agora é uma chamada só.
- A lista é só uma ajuda: o backend confere de novo (404 usuário não encontrado; 409 "Já existe
  um prestador vinculado ao usuário de id: N"; 400 "O usuário está inativo.").
- Sem usuário disponível: o campo fica só com "Selecione" e o texto "Nenhum usuário disponível.
  Cadastre o usuário na tela de Usuários."

**R2. Percentuais opcionais no cadastro.** Os campos começam **vazios**, com o texto
"Padrão da clínica" dentro. Vazio vai como `null` e o backend aplica o padrão (hoje 40% e 45%).
Texto de ajuda (sem números): "Deixe em branco para usar o padrão da clínica. A comissão dos
pacientes que ele trouxe nunca é menor que a padrão."

- *Por quê:* o padrão mora nas constantes da `ProviderEntity`; o front não repete os números
  (se o padrão mudar, o front não fica gravando o antigo).
- Exceção caso a caso (ex: estagiário com 20% e 25%): digitar os dois.
- O padrão é aplicado campo a campo: preencher só a comissão padrão com 50 deixa a indicação
  com 45, e o backend recusa (R4).
- *Alternativa descartada:* o backend devolver o padrão num endpoint para a tela mostrar os
  números. Mais trabalho para pouco ganho.

**R3. A edição altera só o prestador.** Nome e os dois percentuais, obrigatórios, preenchidos
com os valores atuais. O usuário vinculado aparece só para leitura (o vínculo é fixo).

- O status sai do formulário: inativar e reativar ficam só no modal de detalhes (já existe).
- E-mail, perfil e senha do usuário são editados na tela de Usuários.

**R4. O front valida só o formato.** Nome com 3 a 100 caracteres; percentual de 0 a 100, com
até 2 casas decimais, aceitando vírgula; usuário obrigatório no cadastro.

- A regra "indicação ≥ padrão" fica só no backend: 400 "O percentual de pacientes trazidos pelo
  prestador não pode ser menor que o percentual padrão.", mostrada em alerta (não é erro de um
  campo, vem sem `errors`). O modal continua aberto com os valores digitados.

**R5. Formato do percentual.** Enviado como texto com ponto (`"30,5"` → `"30.5"`), como o
`sessionValue` do `PriceFormModal`; exibido com o `formatPercentage` de
`shared/utils/money.js` (`"30.5"` → `"30,5%"`).

- *Por quê:* texto não passa pelo `Number` do JavaScript; o backend lê o `BigDecimal` exato.

**R6. E-mail e perfil vêm do cruzamento de duas listas na page.** A page usa `useProviders()`
(a lista da API, sem junção) e `useUsers()` (o mesmo cache da tela de Usuários) e cruza as duas
por `userId` numa função pura do módulo. A mesma função dá a lista de usuários disponíveis (R1).

- *Alternativas descartadas:*
  - **juntar dentro da `queryFn`** (como hoje, `listProvidersWithUsers`): busca os usuários duas
    vezes (a seleção do cadastro também precisa deles) e o cache `["providers"]` não acompanha
    uma edição feita na tela de Usuários;
  - **o backend devolver o e-mail no `ProviderResponseDTO`**: muda a API e a consulta (busca do
    usuário de cada prestador) por um dado que só esta tela usa, e a tela já precisa da lista de
    usuários para o cadastro.

**R7. O detalhe mostra o status do usuário vinculado.** Campo "Status do Usuário" (selo
Ativo/Inativo), ao lado do e-mail e do perfil. Com o usuário inativo, uma nota: "Usuário
inativo: o prestador não acessa o sistema. Para reativar o prestador, reative antes o usuário
na tela de Usuários."

- *Por quê:* reativar o prestador exige usuário ativo (400 "O usuário está inativo."); vendo o
  status, o admin sabe que precisa reativar o usuário antes.

## 4. Desenho do backend

Não se aplica: os endpoints e as regras já existem (ver [Status](#1-status)).

## 5. Desenho do frontend

Módulo `frontend/src/domain/provider/`:

| Arquivo | Mudança |
|---|---|
| `components/ProviderFormModal.jsx` + `.module.css` | **novo**, no desenho do `UserFormModal`. Cadastro: usuário (seleção), nome, os dois percentuais (opcionais). Edição: usuário só leitura, nome, os dois percentuais (obrigatórios). Entrega à page os dados no formato do DTO e o `applyApiErrors` |
| `components/wizard/` | **removida** (5 componentes e 2 CSS) |
| `schemas/providerSchema.js` | `createProviderSchema` e `updateProviderSchema` (objetos, como no `userSchema`): sem e-mail, perfil, senha, status e sem a regra "indicação ≥ padrão" |
| `api/providerApi.js` | só as chamadas dos endpoints (`listProviders`, `createProvider`, `updateProvider`, `deactivateProvider`, `activateProvider`), recebendo os dados já no formato do DTO. Saem `mapProviderViewModel`, `formatPercentageInput`, `listProvidersWithUsers` e `create/updateProviderWithUser` |
| `api/useProviders.js` | `useProviders` com `listProviders`; `useCreateProvider` e `useUpdateProvider` (`{ id, data }`) com as chamadas diretas; invalidam só `["providers"]` |
| `providerUsers.js` | **novo**, funções puras: `withLinkedUser(providers, users)` (acrescenta `email`, `role` e `userActive`) e `getAvailableUsers(users, providers)` (ativos e sem prestador) |
| `filterProviders.js` | status pelo `active` (como o `filterUsers`), no lugar do `status` em texto |
| `pages/ProviderPage.jsx` | no desenho da `UserPage`: sem as views de página inteira; `form` (`null` / `{ provider: null }` / `{ provider }`) abre o modal; `useUsers()`; carregando ou erro se qualquer das duas listas estiver; `handleSave` com `applyApiErrors` + `showError` no `onError` |
| `components/ProviderTable.jsx` | `active` no lugar de `status`; percentuais pelo `formatPercentage`; perfil sem o valor de reserva "Prestador" |
| `components/ProviderDetailsModal.jsx` + `.module.css` | `active` no lugar de `status`; percentuais pelo `formatPercentage`; status do usuário e a nota (R7, classe `.note`). Correção: o `close()` zera a confirmação ao fechar (antes, ela reaparecia aberta no próximo prestador), como no `UserDetailsModal` |

Os campos da API e do formulário têm os mesmos nomes (`userId`, `name`,
`commissionPercentage`, `referralCommissionPercentage`): o `applyApiFieldErrors` não precisa
traduzir nomes.

## 6. Tarefas

- [x] `providerSchema.js`, `providerApi.js`, `useProviders.js`
- [x] `providerUsers.js` e `filterProviders.js`
- [x] `ProviderFormModal`
- [x] `ProviderPage`, `ProviderTable`, `ProviderDetailsModal`
- [x] remover `components/wizard/`
- [x] `npm run build` e `npm run lint`
- [ ] casos de teste (seção 7)
- [ ] conclusão: documentação (seção 9), spec removida, `node scripts/check-docs.mjs`

## 7. Casos de teste

**Pré-requisitos:** banco com o admin padrão e, criados na tela de Usuários: `u1` (`PROVIDER`,
ativo), `u2` (`ADMIN`, ativo), `u3` (`PROVIDER`, inativo) e `u4` (`PROVIDER`, ativo, já com
prestador `P4`).

**Cadastro**

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir "Novo prestador" | seleção com `u1` e `u2` (e-mail e perfil); sem `u3` (inativo) nem `u4` (já vinculado) |
| 2 | `u1`, nome, percentuais em branco | salvo com 40% e 45%; modal fecha; aparece na lista |
| 3 | `u2`, comissão 20 e indicação 25 | salvo com 20% e 25% |
| 4 | Percentual com vírgula: 30,5 e 35,75 | salvo; exibido como 30,5% e 35,75% |
| 5 | Comissão 50, indicação em branco | alerta 400 "O percentual de pacientes trazidos pelo prestador não pode ser menor que o percentual padrão."; modal aberto com os valores; nenhum usuário criado |
| 6 | Comissão 50, indicação 40 | o mesmo alerta |
| 7 | Percentual 35,555 | erro no campo (até 2 casas), sem chamada à API |
| 8 | Percentual 101 | erro no campo (0 a 100) |
| 9 | Nome com 2 caracteres | erro no campo |
| 10 | Sem usuário selecionado | "Selecione o usuário" |
| 11 | Todos os usuários ativos já vinculados | seleção vazia e o texto "Nenhum usuário disponível..." |
| 12 | Depois do caso 2, abrir "Novo prestador" | `u1` não aparece mais |
| 13 | Duas abas: vincular `u1` na aba A e depois na aba B | aba B: alerta 409 "Já existe um prestador vinculado..." |

**Edição**

| # | Caso | Esperado |
|---|---|---|
| 14 | Editar um prestador | nome e percentuais atuais; usuário só leitura; sem status nem senha |
| 15 | Mudar para 35 e 40 | salvo; tabela e detalhe atualizados |
| 16 | Apagar um percentual | erro no campo (obrigatório na edição) |
| 17 | Indicação menor que a comissão | alerta 400 da regra (caso 5) |

**Status e listagem**

| # | Caso | Esperado |
|---|---|---|
| 18 | Inativar pelo detalhe | some do filtro "Ativos"; detalhe mostra "Inativo" |
| 19 | Inativar o usuário do prestador inativo (tela de Usuários) e reativar o prestador | detalhe mostra "Status do Usuário: Inativo" e a nota; alerta 400 "O usuário está inativo." |
| 20 | Reativar o usuário e depois o prestador | reativado |
| 21 | Buscar por parte do e-mail | encontra o prestador |
| 22 | Mudar o e-mail do usuário na tela de Usuários e voltar a Prestadores | tabela mostra o e-mail novo |
| 23 | Clicar em "Desativar prestador", fechar pelo X e abrir outro prestador | a confirmação não aparece aberta |

**API (erros por campo)**

| # | Caso | Esperado |
|---|---|---|
| 24 | `POST /api/providers` sem `userId` e com `name` vazio | 400 com `errors.userId` e `errors.name` (os nomes que o formulário usa) |

## 8. Pendências e pontos abertos

Nenhuma de negócio em aberto. Já no roadmap e fora desta feature: a comparação de prestadores
sem `id` (`isSameAs`) e as categorias de prestador.

## 9. Documentação ao concluir

- **`architecture.md`**, "Telas de listagem": Prestadores passa a ser exemplo de modal (sai do
  exemplo de wizard).
- **`domain-model.md`**: nada muda (as regras do `Provider` já são as implementadas).
- **`roadmap.md`**:
  - "Próximos passos": tirar o item Prestadores;
  - pendências do Frontend: tirar os percentuais 40/45 fixos, a regra do `providerSchema` e o
    wizard de Prestadores dos erros por campo;
  - tabela de módulos do front: o `provider` vincula um usuário existente, em modal.
- **`specs/README.md`**: "Em andamento" volta para nenhuma; esta spec é removida.

## 10. ADRs a registrar

Não se aplica: ajustes de tela, sem decisão grande de frontend (ver "Onde registrar cada
coisa" no `CLAUDE.md`). O fluxo de telas separadas já está decidido no roadmap e o vínculo
fixo no [ADR 0013](../adr/0013-prestador-com-usuario.md).
