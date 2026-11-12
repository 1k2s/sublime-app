# Spec — Tela de Contratos (contrato real, sem mocks)

A tela de Contratos passa a listar, detalhar, criar e inativar contratos pela API, no modelo
real do contrato (um item por grupo de preço), sem regra de negócio no front.

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

- **Backend pronto:** criar (`POST /api/contracts`), listar, buscar, inativar (`DELETE`), a
  prévia da vigência (`GET /api/contracts/term-preview`) e o aditivo. A regra "o beneficiário é
  outro paciente" já está na `ContractEntity`. Ver `Contract e ContractItem` no
  [`domain-model.md`](../domain-model.md#contract-e-contractitem). Falta só o campo de vigência
  na resposta (R1).
- **Front na `dev`:** mock no `localStorage` (`contractApi.js`), no modelo antigo: um preço no
  cabeçalho (`groupPlanPriceId` / `groupPlanFrequencyPriceId`), término digitado e status
  inventado (`ACTIVE` / `CLOSED`). O formulário não envia `items`, a tabela não tem detalhe e o
  `contractSchema` recusa o titular como beneficiário (`refine`).
- **Branch:** `feature/contract-page`, a partir da `dev`. PR para a `dev`.

## 2. Objetivo e escopo

Deixar a tela de Contratos funcional de ponta a ponta, para o Lançamento ter contratos reais.
Depende de `patient` (titular e beneficiário) e `pricing` (planos, técnicas e linhas de preço
vigentes).

**Entra:**

- listagem pela API, com o nome do titular e do beneficiário e o status (Vigente, Vencido ou
  Inativo) vindo do backend;
- detalhe do contrato (cabeçalho e itens);
- cadastro no modelo real: plano, início, itens (um por grupo de preço) e forma de pagamento,
  com o fim e as opções do plano do Grupo calculados pelo backend;
- inativar contrato criado errado;
- erros por campo da API, incluindo os dos itens;
- saem o mock, os campos do modelo antigo e o `refine` do titular como beneficiário.

**Fica para depois:**

- **Aditivo** (prorrogar, incluir e remover itens, histórico de versões): spec própria
  (decidido em 2026-11-11: o contrato é um módulo complexo, e esta tela já libera o
  Lançamento). Até lá, prorrogar só pelo Swagger.
- **Saldo de sessões no detalhe:** o contrato mostra só os dados do contrato (Fase 1). O saldo,
  de sessões e em R$, fica no módulo `billing` da Fase 2 (ver "Não modelado ainda" no
  [`roadmap.md`](../roadmap.md)).
- **Boleto no formulário:** em aberto (seção 8).

## 3. Regras

**R1. A vigência vem do backend.** A resposta do contrato passa a trazer `current`: ativo e com
o fim a partir de hoje (o `endDate` é inclusivo). A tela mostra:

| `active` | `current` | Status |
|---|---|---|
| sim | sim | Vigente |
| sim | não | Vencido (ativo, com o fim antes de hoje; aguarda a decisão do admin) |
| não | — | Inativo (inativado, ou substituído por aditivo) |

- Filtro com Vigentes (padrão), Vencidos, Inativos e Todos.
- *Por quê:* vigência é regra do backend (princípio 1 do front). Comparar o `endDate` com hoje
  no front repetiria a regra.
- *Alternativa descartada:* só Ativo e Inativo, com o vencido aparecendo como ativo.

**R2. Nome do titular e do beneficiário pelo cruzamento com a lista de pacientes.** A API
devolve só os ids. A page cruza `useContracts()` com `usePatients()` numa função pura do
módulo, como em Prestadores (ver "Telas de listagem" no
[`architecture.md`](../architecture.md#telas-de-listagem)). A busca procura no nome do titular e
no do beneficiário.

**R3. Cadastro em página inteira, no modelo real.** Ordem: titular, beneficiário (opcional),
plano, início, itens e forma de pagamento. Sem "Data de término": o fim é calculado
([ADR 0009](../adr/0009-vigencia-calculada.md)).

- **Plano:** só os ativos e contratáveis (sem o Avulso).
- **Itens, um por grupo de preço** ([ADR 0008](../adr/0008-contrato-com-itens.md)). Cada item
  começa pela técnica (ativa), e o grupo dela define o resto:
  - **grupo por duração:** a duração (as linhas vigentes do grupo **no plano do contrato**) e a
    frequência (`SessionFrequency`);
  - **Pilates em Grupo:** o plano do Grupo (as opções e a sugestão do `term-preview`) e a
    frequência semanal (as linhas vigentes desse plano: 1x, 2x, 3x);
  - contrato com plano de mensalidade (só Grupo): só o item do Grupo, e a única opção de plano
    do Grupo é o próprio plano do contrato.
- **Fim e opções do plano do Grupo:** `GET /api/contracts/term-preview` a cada mudança do
  plano, do início ou das frequências. Quando a prévia falha, a mensagem da API aparece no
  resumo, sem travar a edição (ex: "O plano Transformação nesta frequência passa do prazo
  máximo de 52 semanas.", "Contrato de pacote precisa de pelo menos um item por duração.").
- **Valor:** cada item mostra o valor da sessão da linha escolhida. Sem total: o valor do
  contrato não é dado da API.
- *As listas só oferecem opções válidas* (planos sem o Avulso, grupos já usados fora, linhas
  do plano do contrato, titular fora da busca do beneficiário). É ajuda de tela, como a lista
  de usuários disponíveis em Prestadores: o backend confere de novo.

**R4. Beneficiário opcional, diferente do titular.** A busca do beneficiário não oferece o
titular escolhido, e sai o `refine` do `contractSchema` (decidido em 2026-11-11). A regra é do
backend (400 "O beneficiário deve ser outro paciente."), como a "comissão de pacientes que
trouxe ≥ padrão" em Prestadores. O backend também responde 409 "O beneficiário já está em
outro contrato vigente.".

**R5. Forma de pagamento:** as quatro de hoje (Pix, cartão de crédito, cartão de débito e
dinheiro). Se um contrato vier da API com `BANK_SLIP`, a tela mostra "Boleto".

**R6. Detalhe com os dados do contrato.** Modal com o cabeçalho (titular, beneficiário, plano,
início, fim, forma de pagamento e status) e os itens (grupo, técnica, plano da linha, duração
ou frequência semanal, ritmo e valor da sessão travado). Sem saldo de sessões (seção 2) e sem
editar (o aditivo é a próxima spec).

**R7. Inativar contrato criado errado.** No detalhe, só para contrato ativo (vigente ou
vencido). Contrato não tem reativação, e inativar não é o encerramento (que levará o valor
restante para o saldo do paciente, na Fase 2). A confirmação diz isso: "Inativar este
contrato? Não tem volta: contrato não pode ser reativado. Use só para corrigir um contrato
criado errado (ex: plano ou titular errado)."

- *Por quê:* o aditivo não troca titular, plano nem início, e o backend não aceita um segundo
  contrato vigente para o titular (409 "O paciente já possui um contrato vigente."). Sem a
  ação, corrigir só pelo Swagger.

**R8. Erros da API.** Os erros por campo (`errors`) marcam os campos, incluindo os dos itens
(`items[0].techniqueId` → a técnica do primeiro item). As regras do contrato (400 sem
`errors`) e os 409 aparecem em alerta, com o formulário aberto e os valores digitados.

## 4. Desenho do backend

Módulo `contract`:

| Arquivo | Mudança |
|---|---|
| `domain/ContractEntity.java` | `isCurrentOn(LocalDate date)`: ativo e com o `endDate` a partir da data (inclusivo). Pergunta sobre si mesma, como o `isCurrent()` das linhas de preço (princípio 7) |
| `dto/ContractResponseDTO.java` | campo `current` = `isCurrentOn(LocalDate.now())` |
| `controller/ContractController.java` | "Listar contratos" e "Buscar contrato por ID" explicam o `current` no Swagger |

As consultas do repositório continuam com a mesma regra em JPQL (`active = true and endDate >=
:date`), como as das linhas de preço com `validTo` nulo.

## 5. Desenho do frontend

Módulo `frontend/src/domain/contract/`, com ajustes no `patient` e no `shared`:

| Arquivo | Mudança |
|---|---|
| `api/contractApi.js` | sai o mock. `listContracts` (`GET /contracts`), `createContract` (`POST /contracts`), `deactivateContract` (`DELETE /contracts/{id}`) e `getTermPreview({ planId, startDate, frequencies })` (`GET /contracts/term-preview`). As `frequencies` vão separadas por vírgula: o axios manda array como `frequencies[]=...`, que o Spring não lê |
| `api/useContracts.js` | `useContracts`, `useCreateContract` e `useDeactivateContract` (invalidam `["contracts"]`); `useTermPreview(params)` (`useQuery`, ligado com plano e início preenchidos, `retry: false`: o 400 da prévia é resposta, não falha de rede) |
| `contractPatients.js` | **novo**, função pura: `withPatientNames(contracts, patients)` (acrescenta `patientName` e `beneficiaryName`), como o `providerUsers.js` |
| `contractStatus.js` | **novo**: o status da R1 a partir de `active` e `current` (valor e rótulo), usado pela tabela, pelo detalhe e pelo filtro |
| `paymentMethods.js` | **novo**: os rótulos das formas de pagamento (as cinco do enum, para exibir) e as quatro do formulário (R5) |
| `sessionFrequencies.js` | **novo**: os rótulos do `SessionFrequency` (`MONTHLY` = "1x por mês", `BIWEEKLY` = "Quinzenal", `ONCE_A_WEEK` = "1x por semana" até `FIVE_TIMES_A_WEEK` = "5x por semana"), com os valores idênticos aos do enum do backend |
| `filterContracts.js` | status da R1 (`CURRENT`, `EXPIRED`, `INACTIVE`, `ALL`) e busca no nome do titular e do beneficiário |
| `schemas/contractSchema.js` | modelo novo: titular, beneficiário (opcional), plano, início, forma de pagamento e `items` (pelo menos um; técnica e linha obrigatórias; frequência obrigatória no item por duração). Sem o término e sem o `refine` do titular |
| `components/ContractForm.jsx` | reescrito no modelo real (R3), com `useFieldArray` para os itens. Entrega à page os dados no formato do `ContractRequestDTO` e o `applyApiErrors`, que traduz os nomes da API (`patientId` → `patient`, `items[0].x` → `items.0.x`, `validExclusiveArc` → a linha do item, `frequencyConsistent` → a frequência) |
| `components/ContractItemFields.jsx` | **novo**: os campos de um item (técnica; duração e frequência, ou plano do Grupo e frequência semanal), com as linhas do `useTechniquePrices` da técnica |
| `components/ContractSummary.jsx` | dados do modelo novo: titular, beneficiário, plano, início, fim calculado (ou a mensagem da prévia), itens e forma de pagamento |
| `components/ContractTable.jsx` | colunas: titular (com o beneficiário), plano, itens (técnicas), vigência (início – fim), status e ver detalhes |
| `components/ContractDetailsModal.jsx` | **novo**, no desenho do `UserDetailsModal`: cabeçalho e itens (R6) e o "Inativar contrato" com confirmação (R7) |
| `components/ContractFilters.jsx` | as opções de status da R1 |
| `components/PatientPicker.jsx` | repassa `excludeIds` (R4) |
| `pages/ContractPage.jsx` | no desenho das listagens: `useContracts` + `usePatients`, cruzamento, filtro, modal pelo `id`, cadastro em página inteira com `applyApiErrors` + `showError` e inativar pelo `useDeactivateContract` |
| `patient/components/PatientSearchInput.jsx` | prop opcional `excludeIds`: esses pacientes não aparecem no resultado |
| `shared/components/Badge.jsx` + `.module.css` | variante `warning`, para o Vencido |

## 6. Tarefas

- [ ] Backend: `isCurrentOn`, `current` no DTO e os textos do Swagger; `./mvnw -q compile`
- [ ] `contractApi.js` e `useContracts.js`, sem o mock
- [ ] `contractPatients.js`, `contractStatus.js`, `paymentMethods.js`, `sessionFrequencies.js` e
  `filterContracts.js`
- [ ] `contractSchema.js`, `ContractForm`, `ContractItemFields` e `ContractSummary`
- [ ] `ContractTable`, `ContractDetailsModal`, `ContractFilters` e `ContractPage`
- [ ] `PatientPicker` e `PatientSearchInput` (`excludeIds`); `Badge` `warning`
- [ ] `npm run build` e `npm run lint`
- [ ] casos de teste (seção 7)
- [ ] conclusão: documentação (seção 9), spec removida, `node scripts/check-docs.mjs`

## 7. Casos de teste

**Pré-requisitos:** o catálogo da V4 e, criados na tela de Pacientes, os pacientes ativos `p1` a
`p5`. Nos exemplos, início em 16/11/2026.

**Cadastro**

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir "Novo contrato" | planos sem o Avulso; sem campo de término |
| 2 | `p1`, Evolução, RPG 60 min 1x por semana, Pix | resumo com fim 07/02/2027 (12 semanas) e R$ 167,00 no item; salvo; aparece como Vigente |
| 3 | `p2`, Evolução, RPG 1x por semana + Quiropraxia quinzenal | fim 02/05/2027 (24 semanas) |
| 4 | Num item com RPG, abrir a técnica do segundo item | RPG e as outras técnicas do Individual Padrão não aparecem |
| 5 | `p3`, Evolução, RPG 1x por semana + Pilates em Grupo | plano do Grupo: Mensal e Trimestral, com o Trimestral sugerido; frequência semanal 1x, 2x e 3x; com 2x, R$ 65,81 no item |
| 6 | `p4`, plano Trimestral, Pilates em Grupo 2x | fim 14/02/2027 (13 semanas); a única opção de plano do Grupo é o Trimestral |
| 7 | Transformação, RPG 1x por mês | o resumo mostra "O plano Transformação nesta frequência passa do prazo máximo de 52 semanas."; salvar dá o alerta 400 com a mesma mensagem |
| 8 | Evolução só com o item do Pilates em Grupo | o resumo mostra "Contrato de pacote precisa de pelo menos um item por duração..." |
| 9 | Escolher o titular e abrir a busca do beneficiário | o titular não aparece no resultado |
| 10 | `p5`, Essencial, RPG 1x por semana, início 05/01/2026 | salvo com fim 01/02/2026; status Vencido |
| 11 | Novo contrato para `p1` (já com o vigente do caso 2) | alerta 409 "O paciente já possui um contrato vigente." |
| 12 | Beneficiário que já é beneficiário de outro contrato vigente | alerta 409 "O beneficiário já está em outro contrato vigente." |

**Listagem, detalhe e inativar**

| # | Caso | Esperado |
|---|---|---|
| 13 | Abrir a tela | contratos da API, com o nome do titular e do beneficiário; filtro "Vigentes" |
| 14 | Filtro "Vencidos" | o contrato do caso 10 |
| 15 | Buscar pelo nome do beneficiário | encontra o contrato |
| 16 | Abrir o detalhe do contrato do caso 5 | cabeçalho e os dois itens: Individual Padrão (RPG, Evolução, 60 min, 1x por semana, R$ 167,00) e Em Grupo (Pilates em Grupo, Trimestral, 2x por semana, R$ 65,81) |
| 17 | Inativar o contrato do caso 2, confirmando | status Inativo; aparece em "Inativos"; o detalhe não oferece mais "Inativar" |
| 18 | Depois do 17, novo contrato para `p1` | salvo: o inativado não bloqueia |

**API**

| # | Caso | Esperado |
|---|---|---|
| 19 | `POST /api/contracts` com `beneficiaryId` igual ao `patientId` | 400 "O beneficiário deve ser outro paciente." |
| 20 | `POST /api/contracts` sem `planId` e com um item sem `techniqueId` | 400 com `errors.planId` e `errors["items[0].techniqueId"]`; na tela, cada erro no seu campo |
| 21 | `GET /api/contracts` | cada contrato com `current` |

## 8. Pendências e pontos abertos

- *Em aberto:* o boleto no formulário (o enum `PaymentMethod` tem `BANK_SLIP`). Decidido deixar
  como está por enquanto (2026-11-11).
- **Aditivo:** a próxima spec do contrato; a ordem em relação ao Lançamento não foi decidida. Lá
  entram a prorrogação, a inclusão e a remoção de itens, o `group-plan-options` e como a lista
  mostra as versões (a versão substituída por aditivo também aparece como Inativo).
- Já no roadmap e fora desta feature: o N+1 do `GET /api/contracts` (uma consulta de itens por
  contrato), numa branch própria.

## 9. Documentação ao concluir

- **`domain-model.md`**, "Vigente, vencimento e prorrogação": a resposta traz `current`
  (`ContractEntity.isCurrentOn`), para a tela não repetir a regra.
- **`architecture.md`**, "Telas de listagem": Contratos como exemplo de formulário longo em
  página inteira, sem wizard.
- **`roadmap.md`**:
  - "Próximos passos": sai Contratos e entra o aditivo;
  - pendências do Frontend: sai "Tela de contratos no modelo antigo" (o boleto continua), sai
    Contratos dos erros por campo e sai o titular como beneficiário no Zod;
  - tabela de módulos do front: o `contract` ligado à API.
- **`specs/README.md`**: "Em andamento" volta para nenhuma; esta spec é removida.

## 10. ADRs a registrar

Não se aplica: as decisões são de tela e de escopo, sem decisão grande de frontend (ver "Onde
registrar cada coisa" no `CLAUDE.md`). O campo `current` segue o padrão "Endpoint de apoio à
tela" do
[`architecture.md`](../architecture.md#padrão-de-construção-todos-os-módulos-implementados-seguem).
