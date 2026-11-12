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

- **Backend no início da feature:** criar (`POST /api/contracts`), listar, buscar, inativar
  (`DELETE`), a prévia da vigência (na época, `GET /api/contracts/term-preview`; trocado pelo
  `POST /api/contracts/preview` na R9) e o aditivo. A regra "o beneficiário é outro paciente" já
  estava na `ContractEntity`. Ver `Contract e ContractItem` no
  [`domain-model.md`](../domain-model.md#contract-e-contractitem). Faltava o campo de vigência na
  resposta (R1).
- **Front na `dev`:** mock no `localStorage` (`contractApi.js`), no modelo antigo: um preço no
  cabeçalho (`groupPlanPriceId` / `groupPlanFrequencyPriceId`), término digitado e status
  inventado (`ACTIVE` / `CLOSED`). O formulário não envia `items`, a tabela não tem detalhe e o
  `contractSchema` recusa o titular como beneficiário (`refine`).
- **Na branch (pausada em 2026-11-12, para continuar em outro computador):**
  - **Feito:** tudo da seção 6, com `./mvnw -q compile`, `npm run build`, `npm run lint` e
    `node scripts/check-docs.mjs` sem erros. Casos pela API certos: 2, 3, 5 a 8, 10 a 12, 17 a
    21 e 22 a 30. Primeiro teste pela tela certo (o cadastro antes do redesenho).
  - **Na ordem em que aconteceu:** a tela no modelo real; o redesenho do cadastro depois do
    primeiro teste (R3); o **valor total e as parcelas** (R9: coluna `installments` na V1,
    `POST /api/contracts/preview` no lugar do `GET /term-preview`); o **visual global** (fundo
    `#F9FBFA`, bordas frias, cards com sombra, menu e título com mais destaque; seção 5); e, entre
    prévias visuais, a **opção A**: cadastro no modal grande com rodapé fixo e itens em linhas
    compactas (R3).
  - **Falta:** testar pela tela o modal (casos 1, 1a e 2), o valor e as parcelas (31 a 35) e dar
    uma olhada no visual novo nas outras telas (Pacientes, Prestadores, Preços, Lançamento);
    depois, a conclusão (seção 9).
  - **Antes de testar em outro computador:** a **V1 foi editada** (coluna `installments`), então
    o banco local precisa ser zerado (`docker compose down -v` e `docker compose up -d`) e os
    pacientes de teste recriados (pela tela ou pela API). Também vira aviso no PR.
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
- valor total do contrato e parcelamento (R9; decidido em 2026-11-12, depois do primeiro teste
  pela tela);
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

**R3. Cadastro num modal grande, no modelo real** (versão escolhida em 2026-11-12 entre
prévias visuais, depois do primeiro teste pela tela). O "Novo contrato" abre um **modal grande
sobre a lista**, com o fundo desfocado, como os modais de Usuários e Prestadores. O conteúdo
rola, e um **rodapé fixo** mostra sempre o **fim do contrato** e o **valor total com as
parcelas**, junto com Cancelar e Criar contrato: o efeito de cada escolha aparece sem rolar. O
**clique no fundo não fecha** (um clique sem querer perderia o formulário); fecham o X, o Esc e
o Cancelar. Dentro: **Paciente** (titular e beneficiário lado a lado) e **Plano** lado a lado na
primeira linha; depois **Itens do contrato** e **Vigência e pagamento** (início, forma de
pagamento e parcelas). Sem "Data de término": o fim é calculado
([ADR 0009](../adr/0009-vigencia-calculada.md)). Sem card de resumo: ele repetia os campos.
*Descartado nas prévias:* página inteira com uma barra fixa embaixo (sem o desfoque) e um estado
inicial sem itens (fica como ideia).

- **Plano num seletor próprio** (`RichSelect`): a lista abre com os planos separados pelos
  títulos **Pacotes** e **Mensalidades**, cada plano com a quantidade como etiqueta ("12
  sessões", "13 semanas"; não a técnica, que mudaria a cada técnica nova no catálogo); fechado,
  o campo mostra o plano e a etiqueta. Teclado (setas, Enter, Esc; com a lista aberta, o Esc só
  fecha a lista, não o modal) e clique fora fecham, como a busca de pacientes. Só os ativos e
  contratáveis (sem o Avulso). *Versões descartadas no teste:* cartões grandes e painéis com
  faixas clicáveis, que deixavam a página comprida demais.
- **Alternador Individual | Familiar** (começa em Individual): mostra uma das duas versões de
  cada plano. Trocar o modo leva o plano escolhido para a versão correspondente (Evolução ↔
  Evolução Familiar) ou o limpa, se ela não existe (o Essencial não tem Familiar). A API não tem
  um campo para o Familiar: ele é reconhecido pelo nome (`planFamily.js`); um plano renomeado sem
  o sufixo só aparece no modo Individual. *Alternativa descartada:* um campo no plano, com
  migration, para uma questão de exibição.
- **Itens, um por grupo de preço** ([ADR 0008](../adr/0008-contrato-com-itens.md)), em **linhas
  compactas**: os rótulos (Técnica, Duração / plano do Grupo, Frequência, Valor da sessão)
  aparecem uma vez, num cabeçalho, e cada item é uma linha, com a lixeira no fim e o aviso logo
  abaixo. Cada item começa pela técnica (ativa), e o grupo dela define o resto:
  - **grupo por duração:** a duração e a frequência (`SessionFrequency`, das semanais para a
    quinzenal e a mensal);
  - **Pilates em Grupo:** a frequência semanal e, no pacote, o plano do Grupo (as opções e a
    sugestão da prévia; [ADR 0010](../adr/0010-plano-do-item-do-grupo.md)). Na mensalidade, o
    plano do Grupo é o próprio plano do contrato (aparece como texto na linha).
- **O item guarda as escolhas, não a linha de preço.** A linha sai do plano (`resolveItemLine`:
  técnica + plano + duração, ou técnica + plano do Grupo + frequência semanal). Trocar o plano
  mantém os itens e só atualiza os valores, para comparar planos. As opções de duração e de
  frequência semanal vêm de todas as linhas da técnica, sem depender do plano.
- **Item sem linha no plano mostra um aviso e bloqueia o salvar** (nada é apagado sozinho):
  "Sem preço de 60 min no plano Evolução Familiar."; com mensalidade e item por duração, "RPG é
  do grupo Individual Padrão: as mensalidades são só para o Pilates em Grupo.".
- **Prévia:** `POST /api/contracts/preview` (R9) a cada mudança do plano, do início, dos itens
  ou das parcelas, com os itens que já têm linha. Devolve o fim, as opções do plano do Grupo, o
  valor de cada item, o total e as parcelas. Enquanto recalcula, a tela mantém a prévia anterior
  (o fim e o total não piscam, e as opções do Grupo continuam). Quando a prévia falha, a
  mensagem da API aparece no card Vigência e pagamento, sem travar a edição (ex: "O plano Transformação
  nesta frequência passa do prazo máximo de 52 semanas.", "Contrato de pacote precisa de pelo
  menos um item por duração.").
- **Plano do Grupo fora das opções:** se a prévia mudar e o plano do Grupo escolhido sair das
  opções, o item mostra "As opções do plano do Grupo mudaram: escolha de novo." e não salva.
- **Valor:** cada item mostra o valor da sessão da linha do plano escolhido e, da prévia, as
  sessões e o total do item ("12 sessões · R$ 2.004,00"; no Grupo, "26 aulas").
- *As listas só oferecem opções válidas* (planos sem o Avulso, grupos já usados fora, titular
  fora da busca do beneficiário). É ajuda de tela, como a lista de usuários disponíveis em
  Prestadores: o backend confere de novo.

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

**R9. Valor total e parcelas** (decidido em 2026-11-12). Tudo calculado no backend; a tela só
mostra.

- **Valor de cada item:** sessões × valor da sessão da linha travada. Item por duração: as
  sessões do plano (Evolução, RPG 60 min = 12 × R$ 167,00 = R$ 2.004,00). Pilates em Grupo: as
  aulas do plano do Grupo, semanas × vezes por semana (Trimestral 2x = 13 × 2 = 26 × R$ 65,81 =
  R$ 1.711,06).
- **Total do contrato:** a soma dos itens, sem desconto.
- **Parcelas:** de 1 até os meses do contrato, no máximo 12, em qualquer forma de pagamento,
  sem juros. A tela começa no máximo. **Meses pelas semanas** (52 semanas = 12 meses),
  arredondados para o mais próximo: 4 semanas = 1, 12 e 13 = 3, 24 e 26 = 6, 52 = 12. Pelas
  semanas, e não pelo calendário, porque assim o mesmo prazo dá sempre o mesmo máximo (pelo
  calendário, 26 semanas dão 6 meses e 1 dia, que arredondados para cima seriam 7).
- **Valor das parcelas:** o total dividido pelas parcelas, com os centavos que sobram na
  primeira (R$ 1.711,06 em 3x = 1ª de R$ 570,36 + 2 × R$ 570,35).
- **Gravado só o número de parcelas** (`installments`, de 1 a 12 no banco). O total e o valor
  das parcelas saem das linhas travadas nos itens, que não mudam: não há total gravado para
  ficar diferente dos itens.
- **Cadastro:** parcelas acima do máximo → 400 "O número de parcelas deve ser de 1 a 3 (meses
  do contrato).". **Prévia:** parcelas acima do máximo valem o máximo (ex: o plano mudou para um
  mais curto); a resposta diz as aplicadas, e a tela envia essas.
- **Aditivo:** copia as parcelas da versão anterior. Renegociar o parcelamento na prorrogação
  fica para a spec do aditivo.
- **Na tela:** o card final vira "Vigência e pagamento" (início, forma de pagamento, parcelas;
  fim e valor total em destaque, com a divisão "3x: 1ª de R$ 1.238,36 + 2 × R$ 1.238,35"). O
  detalhe mostra o valor total, as parcelas e o total de cada item; a listagem ganha a coluna
  Valor (total e "em 3x").
- *Alternativas descartadas:* gravar o total (pode divergir dos itens); meses pelo calendário
  (o mesmo plano daria máximos diferentes conforme o início); calcular no front (regra de
  preço é do backend).

## 4. Desenho do backend

Módulo `contract`:

| Arquivo | Mudança |
|---|---|
| `domain/ContractEntity.java` | `isCurrentOn(LocalDate date)`: ativo e com o `endDate` a partir da data (inclusivo). Pergunta sobre si mesma, como o `isCurrent()` das linhas de preço (princípio 7) |
| `dto/ContractResponseDTO.java` | campo `current` = `isCurrentOn(LocalDate.now())` |
| `controller/ContractController.java` | "Listar contratos" e "Buscar contrato por ID" explicam o `current` no Swagger. `POST /api/contracts/preview` no lugar do `GET /term-preview` (R9) |
| `db/migration/V1__initial_schema.sql` | coluna `installments INT NOT NULL` em `contract`, com `CHECK (installments BETWEEN 1 AND 12)` (V1 editada: ADR 0028) |
| `domain/ContractItemValue.java` | **novo**, Value Object: sessões do item × valor da sessão, pela linha (R9); `sumOf` (o total) |
| `domain/Installments.java` | **novo**, Value Object: `maxFor(semanas)` (meses, de 1 a 12), `validateCount` e `split` (centavos na primeira) |
| `domain/ContractEntity.java` | campo `installments`, validado na criação (`validateInstallments`) e copiado no aditivo; `getTotalValue()`, `getMaxInstallments()` e `getInstallmentValues()` |
| `domain/ContractItemEntity.java`, `ContractItemDraft.java` | o valor do item (`getValue()` / `value()`), pela mesma conta |
| `dto/ContractRequestDTO.java` | `installments` obrigatório, de 1 a 12 |
| `dto/ContractPreviewRequestDTO.java`, `ContractPreviewResponseDTO.java`, `ContractItemPreviewDTO.java` | **novos**: a prévia (plano, início, itens e parcelas → fim, opções do Grupo, valor de cada item, total, máximo e divisão das parcelas) |
| `dto/ContractResponseDTO.java`, `ContractItemResponseDTO.java` | `totalValue`, `installments`, `firstInstallmentValue` e `installmentValue`; no item, `sessionCount` e `totalValue` |
| `service/ContractService.java` | `preview(...)` no lugar do `previewTerm`, montando os itens pelo mesmo `toDrafts` do cadastro |

As consultas do repositório continuam com a mesma regra em JPQL (`active = true and endDate >=
:date`), como as das linhas de preço com `validTo` nulo.

## 5. Desenho do frontend

Módulo `frontend/src/domain/contract/`, com ajustes no `patient` e no `shared`:

| Arquivo | Mudança |
|---|---|
| `api/contractApi.js` | sai o mock. `listContracts` (`GET /contracts`), `createContract` (`POST /contracts`), `deactivateContract` (`DELETE /contracts/{id}`) e `getContractPreview(payload)` (`POST /contracts/preview`) |
| `api/useContracts.js` | `useContracts`, `useCreateContract` e `useDeactivateContract` (invalidam `["contracts"]`); `useContractPreview(payload)` (`useQuery`, ligado com plano e início preenchidos, `retry: false`: o 400 da prévia é resposta, não falha de rede; `keepPreviousData`: a prévia anterior fica enquanto recalcula) |
| `formatInstallments.js` | **novo**: o texto das parcelas ("3x: 1ª de … + 2 × …", "3x de …", "À vista (1x)"), usado no rodapé do cadastro, no detalhe e na tabela |
| `contractPatients.js` | **novo**, função pura: `withPatientNames(contracts, patients)` (acrescenta `patientName` e `beneficiaryName`), como o `providerUsers.js` |
| `contractStatus.js` | **novo**: o status da R1 a partir de `active` e `current` (valor e rótulo), usado pela tabela, pelo detalhe e pelo filtro |
| `paymentMethods.js` | **novo**: os rótulos das formas de pagamento (as cinco do enum, para exibir) e as quatro do formulário (R5) |
| `sessionFrequencies.js` | **novo**: os rótulos do `SessionFrequency`, com os valores idênticos aos do enum do backend, na ordem da tela: "1x por semana" (`ONCE_A_WEEK`) até "5x por semana" (`FIVE_TIMES_A_WEEK`), "Quinzenal" (`BIWEEKLY`) e "1x por mês" (`MONTHLY`) |
| `resolveItemLine.js` | **novo**, função pura: a linha de preço de um item pelas escolhas e pelo plano (R3), ou o aviso de por que não há linha. Não depende da prévia (a prévia recebe os itens resolvidos aqui); se o plano do Grupo ainda está nas opções, o formulário confere depois |
| `filterContracts.js` | status da R1 (`CURRENT`, `EXPIRED`, `INACTIVE`, `ALL`) e busca no nome do titular e do beneficiário |
| `schemas/contractSchema.js` | modelo novo: titular, beneficiário (opcional), plano, início, forma de pagamento e `items` (pelo menos um; técnica obrigatória; duração e frequência no item por duração; frequência semanal no do Grupo e, no pacote, o plano do Grupo). Sem o término e sem o `refine` do titular |
| `components/ContractForm.jsx` | reescrito no modelo real (R3): o conteúdo que rola (os quatro cards) e o rodapé fixo (`ContractResultBar`), `useFieldArray` para os itens e `useQueries` para as linhas de preço de todas as técnicas escolhidas (mesmo cache do `useTechniquePrices`). Entrega à page os dados no formato do `ContractRequestDTO` e o `applyApiErrors`, que traduz os nomes da API (`patientId` → `patient`, `items[0].x` → `items.0.x`, `sessionDurationPriceId` → a duração, `sessionFrequencyPriceId` → a frequência semanal, `frequencyConsistent` → a frequência) |
| `components/ContractPlanPicker.jsx` | **novo**: o campo Plano, com o alternador Individual \| Familiar e o `RichSelect` dos planos, em grupos Pacotes e Mensalidades (R3) |
| `shared/components/RichSelect.jsx` + `.module.css` | **novo**, genérico: seleção com opções ricas (título, detalhe, etiquetas e grupos), teclado e clique fora. Em `shared` por ser um campo de formulário como o `Select` |
| `planFamily.js` | **novo**, funções puras: reconhece o plano Familiar pelo nome e acha a versão correspondente no outro modo (R3) |
| `components/ContractItemFields.jsx` | **novo**: um item numa linha compacta (técnica; duração e frequência, ou plano do Grupo e frequência semanal; o valor da sessão, as sessões e o total do item, da prévia; a lixeira) e o aviso do item; `ContractItemsHeader`, o cabeçalho das colunas |
| `components/ContractTermSection.jsx` | **novo** (no lugar do `ContractSummary`, removido): o card "Vigência e pagamento", com início, forma de pagamento e parcelas (mostra as aplicadas pela prévia) e a mensagem da prévia quando ela falha |
| `components/ContractResultBar.jsx` | **novo**: o rodapé fixo do cadastro, com o fim do contrato, o valor total com as parcelas e os botões |
| `shared/components/Modal.jsx` + `.module.css` | `size="large"` (largo e alto; o conteúdo cuida da rolagem e do rodapé) e `closeOnBackdrop` (com `false`, o clique no fundo não fecha) |
| `components/ContractTable.jsx` | colunas: titular (com o beneficiário), plano, itens (técnicas), vigência (início – fim), valor (total e "em 3x"), status e ver detalhes. O botão usa o ícone de olho, não o lápis das outras telas: o contrato não tem edição nesta spec (decidido em 2026-11-12; com o aditivo, o detalhe ganha o "Editar") |
| `components/ContractDetailsModal.jsx` | **novo**, no desenho do `UserDetailsModal`: cabeçalho com o valor total e as parcelas, itens com as sessões e o total de cada um (R6, R9) e o "Inativar contrato" com confirmação (R7) |
| `components/ContractFilters.jsx` | as opções de status da R1 |
| `components/PatientPicker.jsx` | repassa `excludeIds` (R4) |
| `pages/ContractPage.jsx` | no desenho das listagens: `useContracts` + `usePatients`, cruzamento, filtro, modal pelo `id`, cadastro no modal grande (`Modal size="large" closeOnBackdrop={false}`) com `applyApiErrors` + `showError` e inativar pelo `useDeactivateContract` |
| `patient/components/PatientSearchInput.jsx` | prop opcional `excludeIds`: esses pacientes não aparecem no resultado |
| `shared/components/Badge.jsx` + `.module.css` | variante `warning`, para o Vencido |

**Visual global** (pedido durante a feature, em 2026-11-12; vale para todas as telas; ver
"Estilo" no [`architecture.md`](../architecture.md#estilo)):

| Arquivo | Mudança |
|---|---|
| `index.css` | fundo `--color-bg: #f9fbfa` (era bege `#faf7f0`); borda `--color-border: #e4e9e6` (era bege `#e5e0d5`); novas `--color-title` e `--shadow-card`; o `body` usa as variáveis |
| 13 `.module.css` e o `AppSidebar.jsx` | os dois tons bege escritos direto trocados por `var(--color-bg)` e `var(--color-border)` |
| `shared/components/Card.module.css` | sem borda, com a sombra `--shadow-card` |
| `app/layout/AppSidebar.jsx` | sem a linha da direita (sombra); itens maiores (15px, 46px, ícones de 20px); item ativo com fundo verde-claro, texto e ícone na cor do sistema, negrito e a barra à esquerda |
| `shared/components/PageHeader.module.css` | título de 28px em `--color-title`; descrição de 15px |

## 6. Tarefas

- [x] Backend: `isCurrentOn`, `current` no DTO e os textos do Swagger; `./mvnw -q compile`
- [x] `contractApi.js` e `useContracts.js`, sem o mock
- [x] `contractPatients.js`, `contractStatus.js`, `paymentMethods.js`, `sessionFrequencies.js` e
  `filterContracts.js`
- [x] `contractSchema.js`, `ContractForm`, `ContractItemFields` e `ContractSummary`
- [x] Redesenho do cadastro depois do primeiro teste (R3): `ContractPlanPicker`,
  `resolveItemLine`, `ContractTermSection` no lugar do `ContractSummary`, ordem da frequência
- [x] `ContractTable`, `ContractDetailsModal`, `ContractFilters` e `ContractPage`
- [x] `PatientPicker` e `PatientSearchInput` (`excludeIds`), junto com o formulário, que os usa
- [x] `Badge` `warning`
- [x] `npm run build` e `npm run lint`
- [x] Valor total e parcelas (R9): backend (V1, `ContractItemValue`, `Installments`, prévia
  `POST`, campos na resposta), `./mvnw -q compile`; front (prévia nova, card "Vigência e
  pagamento", detalhe e tabela), build e lint
- [x] Cadastro no modal grande com rodapé fixo e itens em linhas compactas (opção A das
  prévias), build e lint
- [x] casos de teste pela API (seção 7)
- [x] Visual global (seção 5): fundo e bordas frias, cards com sombra, menu e título
- [ ] casos de teste pela tela: o modal (1, 1a e 2) e o valor e as parcelas (31 a 35)
- [ ] conferir o visual novo nas outras telas (Pacientes, Prestadores, Preços, Lançamento)
- [ ] conclusão: documentação (seção 9), spec removida, `node scripts/check-docs.mjs`

## 7. Casos de teste

**Pré-requisitos:** o catálogo da V4 e, criados na tela de Pacientes, os pacientes ativos `p1` a
`p5`. Nos exemplos, início em 16/11/2026.

**Cadastro**

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir "Novo contrato" | modal grande sobre a lista, com o fundo desfocado; Paciente e Plano lado a lado, Itens em linhas, Vigência e pagamento; rodapé fixo com o fim, o valor e os botões; planos em Pacotes e Mensalidades, sem o Avulso; sem campo de término; frequência de "1x por semana" a "1x por mês" |
| 1a | Com o modal aberto: clicar no fundo; abrir a lista de planos e apertar Esc; apertar Esc de novo | o fundo não fecha; o primeiro Esc fecha só a lista; o segundo fecha o modal |
| 2 | `p1`, Evolução, RPG 60 min 1x por semana, Pix | fim 07/02/2027 (12 semanas) no rodapé e R$ 167,00 no item; salvo; o modal fecha e o contrato aparece como Vigente |
| 3 | `p2`, Evolução, RPG 1x por semana + Quiropraxia quinzenal | fim 02/05/2027 (24 semanas) |
| 4 | Num item com RPG, abrir a técnica do segundo item | RPG e as outras técnicas do Individual Padrão não aparecem |
| 5 | `p3`, Evolução, RPG 1x por semana + Pilates em Grupo | plano do Grupo: Mensal e Trimestral, com o Trimestral sugerido; frequência semanal 1x, 2x e 3x; com 2x, R$ 65,81 no item |
| 6 | `p4`, plano Trimestral, Pilates em Grupo 2x | fim 14/02/2027 (13 semanas); a única opção de plano do Grupo é o Trimestral |
| 7 | Transformação, RPG 1x por mês | o card Vigência e pagamento mostra "O plano Transformação nesta frequência passa do prazo máximo de 52 semanas."; salvar dá o alerta 400 com a mesma mensagem |
| 8 | Evolução só com o item do Pilates em Grupo | o card Vigência e pagamento mostra "Contrato de pacote precisa de pelo menos um item por duração..." |
| 8a | RPG 60 min 1x por semana; alternar entre Essencial, Evolução e Transformação | os itens continuam preenchidos e o valor muda (R$ 180,00, R$ 167,00 e o do Transformação) |
| 8b | Evolução Familiar com Quiropraxia 60 min | aviso no item "Sem preço de 60 min no plano Evolução Familiar."; não salva |
| 8c | RPG no item e trocar para Trimestral | aviso "RPG é do grupo Individual Padrão: as mensalidades são só para o Pilates em Grupo."; não salva; voltar a um pacote tira o aviso |
| 8d | Escolher Evolução e alternar para Familiar; depois escolher Essencial (Individual) e alternar | Evolução Familiar selecionado, itens mantidos; com o Essencial, o plano é limpo (não tem Familiar) |
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

**Valor e parcelas (R9)**, pela API (rodados em 2026-11-12, com o banco zerado):

| # | Caso | Esperado |
|---|---|---|
| 22 | Prévia: Evolução, RPG 60 min 1x + Grupo Trimestral 2x | itens 12 × 167,00 = 2.004,00 e 26 × 65,81 = 1.711,06; total 3.715,06; máximo 3; 1ª 1.238,36 + 2 × 1.238,35 |
| 23 | O mesmo pedindo 6x / 2x | aplica 3x / 2 × 1.857,53 |
| 24 | Prévia: Essencial RPG 1x / Evolução RPG quinzenal / Transformação RPG 1x / Vitalidade RPG 1x | máximo 1 / 6 / 6 / 12 (4, 24, 26 e 52 semanas) |
| 25 | Prévia: Trimestral só com Grupo 2x | 13 semanas; 1.711,06; máximo 3; 1ª 570,36 + 2 × 570,35 |
| 26 | Prévia: Evolução sem itens | 400 "Contrato de pacote precisa de pelo menos um item por duração..." |
| 27 | Cadastro do caso 22 em 3x | 201 com o total e as parcelas na resposta |
| 28 | Cadastro Evolução RPG em 4x | 400 "O número de parcelas deve ser de 1 a 3 (meses do contrato)." |
| 29 | Cadastro sem parcelas / com 13 | 400 com `errors.installments` |
| 30 | `GET /api/contracts` | cada contrato com `totalValue`, `installments`, `firstInstallmentValue` e `installmentValue` |

**Valor e parcelas**, pela tela:

| # | Caso | Esperado |
|---|---|---|
| 31 | Novo contrato: Evolução, RPG 60 min 1x + Grupo Trimestral 2x | itens com "12 sessões · R$ 2.004,00" e "26 aulas · R$ 1.711,06"; valor total R$ 3.715,06; parcelas em 3x (máximo); "3x: 1ª de R$ 1.238,36 + 2 × R$ 1.238,35" |
| 32 | Escolher 2x e trocar o plano para Essencial | parcelas vão para 1x (o máximo do Essencial); "À vista (1x)" |
| 33 | Salvar em 2x um contrato de 3 meses | salvo com 2x; a listagem mostra o total e "em 2x"; o detalhe, o valor total, as parcelas e o total de cada item |
| 34 | Trocar o plano entre Evolução e Transformação com os itens preenchidos | valores e total mudam; o fim e o total não piscam enquanto recalcula |
| 35 | "Adicionar item" com um item preenchido; depois remover | o item novo aparece vazio, sem erro na tela; remover atualiza o total |

## 8. Pendências e pontos abertos

- **Design do cadastro de contrato:** resolvido em 2026-11-12 pela opção A das prévias (modal
  grande com rodapé fixo, itens em linhas compactas; R3). Fica como ideia o **estado inicial sem
  itens** (uma área "Nenhum item ainda" com atalhos para as técnicas), que estava nas prévias.
- *Em aberto:* o boleto no formulário (o enum `PaymentMethod` tem `BANK_SLIP`). Decidido deixar
  como está por enquanto (2026-11-11).
- **Aditivo:** a próxima spec do contrato; a ordem em relação ao Lançamento não foi decidida. Lá
  entram a prorrogação, a inclusão e a remoção de itens, o `group-plan-options` e como a lista
  mostra as versões (a versão substituída por aditivo também aparece como Inativo).
- Já no roadmap e fora desta feature: o N+1 do `GET /api/contracts` (uma consulta de itens por
  contrato), numa branch própria.
- **Exports sem uso em outros módulos** (achados na varredura de 2026-11-12, de antes desta
  feature; não mexidos): `usePatient` (`usePatients.js`), `getUserById` (`userApi.js`),
  `isValidCpf` (`masks.js`), `personalDataSchema` e `addressFieldsSchema` (`patientSchema.js`).
  Vão para o roadmap na conclusão, como limpeza à parte.

## 9. Documentação ao concluir

- **Já feito, junto com o código** (regra implementada vai para o documento de referência no
  mesmo passo):
  - `domain-model.md`: o `current` em "Vigente, vencimento e prorrogação"; a subseção **"Valor
    e parcelas"** (R9); `installments` na tabela e no diagrama do `Contract`; o
    `POST /api/contracts/preview` no lugar do `term-preview` (endpoints de apoio e "Plano do
    item do Grupo");
  - `architecture.md`: o `POST /api/contracts/preview` como exemplo de "Endpoint de apoio à
    tela"; em "Telas de listagem", o formulário longo em **modal grande com rodapé fixo** (ex:
    Contratos) e o cruzamento com os pacientes (`contractPatients.js`); `RichSelect` em
    `shared/components`; em "Estilo", as variáveis novas e os cards com sombra.
- **Na conclusão, conferir de novo** se o `domain-model` e o `architecture` batem com o código
  final.
- **`roadmap.md`**:
  - "Próximos passos": sai Contratos e entra o aditivo;
  - pendências do Frontend: sai "Tela de contratos no modelo antigo" (o boleto continua), sai
    Contratos dos erros por campo e sai o titular como beneficiário no Zod;
  - tabela de módulos do front: o `contract` ligado à API;
  - "Cores em variáveis CSS" (Melhorias): o bege saiu (fundo e bordas em variáveis), mas ainda
    há outros tons escritos direto no CSS;
  - a ideia do estado inicial sem itens (seção 8) e os exports sem uso em outros módulos
    (seção 8).
- **Descrição do PR:** avisar que a V1 foi editada (zerem o banco local) e que o visual mudou em
  todas as telas.
- **`specs/README.md`**: "Em andamento" volta para nenhuma; esta spec é removida.

## 10. ADRs a registrar

- **Valor do contrato e parcelas** (próximo número livre, hoje 0032): valor e parcelas
  calculados pelas linhas travadas, só o número de parcelas gravado; meses pelas semanas (52 =
  12), arredondados; centavos na primeira parcela; prévia por `POST` com os itens no lugar do
  `GET /term-preview`. Alternativas descartadas: gravar o total, meses pelo calendário, calcular
  no front, prévia por query string. O ADR 0010 cita o `term-preview` entre os endpoints de
  apoio; a decisão dele não muda (só o endpoint), então ele não é marcado como substituído e o
  ADR novo registra a troca.
- O resto não se aplica: as decisões são de tela e de escopo, sem decisão grande de frontend
  (ver "Onde registrar cada coisa" no `CLAUDE.md`). O campo `current` segue o padrão "Endpoint
  de apoio à tela" do
  [`architecture.md`](../architecture.md#padrão-de-construção-todos-os-módulos-implementados-seguem).
