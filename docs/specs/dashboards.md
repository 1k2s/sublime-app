# Spec — Dashboards (rendimentos e atendimentos)

O prestador vê quanto rendeu no dia, na semana e no mês e consulta os próprios atendimentos por
período; o admin vê o mesmo de todos os prestadores, com o filtro por prestador.

Iniciada em 2026-11-24.

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

- **Backend pronto e testado** (2026-11-25; testes em 2026-11-26): `launched_at`, rendimento
  (`earnings-summary` e `earnings`), filtros novos da listagem e `day-summary` alinhado. O
  `EarningsIntegrationTest` confere a consulta nativa no MySQL e os casos da seção 7. Ele também
  revelou que, nos testes, as datas eram lidas um dia antes (fuso aplicado tarde; corrigido no
  `TestcontainersConfiguration`, ver a seção 9).
- **Front da tela escrito** (2026-11-26): rota `/dashboard`, menu, período, botões dos
  prestadores, números, lista, abas com o slide e a pizza; `npm run build` e `npm run lint`
  passam. **Ainda não testado pela tela** (seção 7, "Pela interface"). Falta o que está aberto na
  seção 6.
- **Decidido em 2026-11-26:**
  - **A lista do dashboard ordena pela data do lançamento**, do mais recente para o mais antigo,
    no front (`utils/filterConsultations.js`): a mesma data do filtro e do rendimento. É só ordem
    de exibição; a API continua ordenando pela data do atendimento.
  - **Filtros refeitos pela prévia C** (seção 5, "Filtros refeitos em 2026-11-26").
  - **Admin com um prestador escolhido: duas consultas `earnings`**, uma com o `providerId` (os
    números, a pizza e a lista) e outra sem (a lista "Rendimento por prestador"). Em "Todos" as duas têm a
    mesma `queryKey` e viram uma requisição só. Descartado: tirar os números do item do
    `byProvider` (uma requisição a menos, mas dois caminhos no código).
  - **Anel com trilho e barras** (prévia C de quatro, comparada com o anel simples, a meia-lua e a
    pizza cheia): um trilho claro por trás, duas fatias em degradê com pontas arredondadas e um
    respiro entre elas. **Efeitos:** o anel se desenha ao abrir e o total do centro sobe do zero
    (`useAnimatedNumber`; o último quadro é o valor exato da API); o hover numa fatia ou numa
    linha da legenda destaca a fatia, esmaece a outra e mostra o valor dela no centro; trocar de
    período ou de prestador desliza as fatias e as barras até o tamanho novo.
  - **Centro e legenda em HTML**, juntos do anel no meio do card, com os valores em texto (o
    leitor de tela lê; o canvas não). A barra de cada linha da legenda é só a largura do desenho.
    O `Chart.jsx` passou a receber os eventos do ECharts (`onEvents`) e a expor o `dispatchAction`
    pelo `ref`, para a legenda destacar a fatia; registra só o `PieChart` e o canvas.
  - **Personalizado abre com o período que estava escolhido**; a data inicial depois da final
    mostra a mensagem do backend (400) no lugar dos números.
- **Decidido em 2026-11-25:**
  - **Dashboard simples, com um gráfico de pizza** no lugar das barras: a **composição do
    repasse** do período (rendimento dos realizados × faltas cobradas), para os dois perfis. Usa
    os números do `earnings`, sem consulta nova.
  - **As abas continuam** ("Resumo" e "Panorama executivo", com a animação de slide); o Panorama
    passa a ter só a pizza.
  - **Sai a série por dia** (`earnings/daily`), as barras empilhadas por prestador, a linha de
    atendimentos e o gráfico por prestador. O rendimento por prestador do admin fica só nos
    botões do topo (o `byProvider`).
  - **`byProvider` com todos os prestadores ativos**, mesmo sem lançamento (zerados, para o admin
    ver quem não lançou), mais os inativos com lançamento no período; por nome.
  - **Soma do rendimento em SQL nativo** sobre a coluna `payout_value`: o `sum` do JPQL sobre o
    `payoutValue`, que passa pelo `MoneyConverter`, não tem o tipo de retorno garantido.
- **Decidido em 2026-11-24:**
  - o rendimento conta **só os atendimentos realizados** (`ATTENDED`);
  - o período do rendimento é pela **data do lançamento**, não pela data do atendimento;
  - a tela tem **gráficos**, com o **ECharts** (os gráficos mudaram em 2026-11-25: uma pizza);
  - as recomendações da seção 3 foram aceitas: faltas cobradas à parte, `day-summary` alinhado,
    semana de segunda a domingo, mês do calendário, lista pela data do lançamento, tela inicial
    sem mudança.
- **O que já existe** no `consultation`:
  - `GET /api/consultations?providerId&patientId&from&to`: listagem com o período pela data do
    **atendimento** (`occurredAt`), ativos e inativos, sem total (`ConsultationController.java:81`);
  - `GET /api/consultations/day-summary`: o repasse de hoje, pela data do atendimento e **nos
    três status** (`ConsultationService.java:189`), mostrado na tela de Lançamento;
  - o `PROVIDER` só lê os próprios atendimentos (`resolveProviderForReading`).
- **Não existe a data do lançamento:** a tabela `consultation` só grava a data do atendimento
  (`occurred_at`, sem hora). Esta feature cria a coluna (seção 4).
- **Branch:** `feature/dashboards`, a partir da `dev` em `edb92d0` (merge do PR #54). PR para a
  `dev`, com merge commit.

## 2. Objetivo e escopo

Hoje o prestador só vê o repasse do dia, na tela de Lançamento, e só os atendimentos com a data
de hoje: um retroativo lançado hoje não aparece em lugar nenhum da tela dele. O admin não tem
uma visão do período de nenhum prestador sem montar a conta pelo Swagger.

Depende só do `consultation` (leitura). O `provider` entra só no filtro do admin (a lista de
prestadores, que já existe).

**Entra:**

- **Backend:**
  - coluna `launched_at` no `consultation`, gravada no lançamento;
  - resumo do rendimento: hoje, semana e mês, com a quantidade e o repasse somado;
  - total do rendimento de um período qualquer (com o de cada prestador, para o admin);
  - filtros novos na listagem: período pela data do lançamento e status.
- **Frontend:**
  - tela de Dashboard (`/dashboard`, os dois perfis): o rendimento de hoje, da semana e do mês,
    a pizza da composição do repasse, a lista de atendimentos com filtros e o total do período
    filtrado;
  - o ECharts como biblioteca de gráficos do projeto;
  - para o `ADMIN`, o filtro por prestador (vazio = todos).
- Fecha o item "Histórico de atendimentos com filtro por período" do checklist do front.

**Fica para depois (ou não vai existir):**

- **Pagamento do repasse ao prestador** (fechamento do período): continua em "Não modelado
  ainda" no roadmap. O dashboard mostra quanto rendeu, não quanto já foi pago.
- **Outros gráficos** além da pizza: só se a clínica pedir depois de usar a tela (ex: o
  rendimento por dia, que saiu em 2026-11-25). Cada gráfico novo precisa de uma consulta no
  backend.
- **Visão financeira da clínica** (valor cobrado do paciente, `baseValue`): é a Fase 2
  (`Payment`).

## 3. Regras

**Rendimento**

- **Rendimento = soma do `payoutValue`** dos atendimentos **ativos**, com status `ATTENDED`,
  cuja **data do lançamento** cai no período. O backend soma; o front só mostra.
- **Só `ATTENDED`** (decidido em 2026-11-24).
- **Pela data do lançamento** (decidido em 2026-11-24): um retroativo lançado hoje, com a data
  do atendimento da semana passada, conta no rendimento de hoje. Nada muda depois de lançado:
  um atendimento não "entra" num período já visto.
- **Atendimento desfeito** (inativo) não conta, em período nenhum. Como o `PROVIDER` só desfaz
  os de hoje, o rendimento de um dia passado só muda quando o admin desfaz um lançamento dele.
- **Faltas cobradas à parte** (decidido em 2026-11-24). O `NO_SHOW` e o `CANCELED_LATE` são
  cobrados do paciente e geram repasse com o mesmo cálculo
  ([ADR 0014](../adr/0014-tres-status-cobrados.md)), mas ficam fora do rendimento. Cada período
  mostra também o **repasse das faltas cobradas** (os dois status, ativos, pela data do
  lançamento), num segundo número, para o prestador ver o repasse inteiro sem somar de cabeça.
  Descartados: esconder as faltas (o prestador não teria onde conferir o valor) e deixar a falta
  sem repasse (muda o lançamento e o ADR 0014, fora do escopo).
- **O "repasse do dia" da tela de Lançamento** (`day-summary`) passa a seguir a regra do
  rendimento (só `ATTENDED`, pela data do lançamento), com as faltas à parte, para "hoje" ter um
  número só nas duas telas (decidido em 2026-11-24).

**Períodos** (no fuso da clínica, [ADR 0034](../adr/0034-hoje-no-fuso-da-clinica.md))

- **Hoje:** o dia da clínica.
- **Semana:** de segunda a domingo, a semana de hoje (o padrão ISO). Descartado: os últimos 7
  dias, que mudam de início todo dia.
- **Mês:** o mês do calendário de hoje. Descartado: os últimos 30 dias.
- Os três períodos são definidos pelo backend e voltam na resposta (`from` e `to`), para a tela
  mostrar o intervalo sem calcular.

**Quem vê o quê**

- **`PROVIDER`:** só os próprios dados, qualquer que seja o `providerId` da URL (como na
  listagem atual). Sem prestador vinculado → 403 "Seu usuário não está vinculado a um
  prestador. Procure o administrador.".
- **`ADMIN`:** todos os prestadores; com o `providerId`, só aquele. Diferente do `day-summary`,
  aqui o `providerId` é **opcional** para o admin (vazio = todos).
- **Visão do admin:** os números com o total de todos os prestadores e, nos botões do topo, o
  rendimento de cada prestador no período (`byProvider`: os ativos, mesmo zerados, e os
  inativos com lançamento). Escolher o prestador filtra os números, a pizza e a lista.

**Gráfico** (decidido em 2026-11-25)

- **Um gráfico de pizza: a composição do repasse do período**, para os dois perfis. Duas
  fatias: "Atendidos" (o rendimento, `payoutTotal`) e "Faltas cobradas"
  (`chargedAbsencePayoutTotal`), com a quantidade no tooltip; o repasse total (`totalPayout`)
  no centro.
- Os números vêm prontos do `earnings`, pela regra do rendimento. O front não soma nem agrupa a
  lista para montar o gráfico: só desenha (o backend é a fonte da verdade).
- Acompanha o período e o prestador filtrados. Período sem repasse: a pizza dá lugar a uma
  mensagem ("Nenhum repasse no período").
- **Para depois, se a clínica pedir:** o rendimento por dia (barras), atendimentos por status
  (separando as faltas das desmarcações fora do prazo) e por técnica (o `techniqueId` existe
  para esse tipo de relatório; ver `Consultation` no [`domain-model.md`](../domain-model.md)).

**Lista de atendimentos**

- **A lista filtra pela data do lançamento**, a mesma do rendimento, para o total do período
  bater com a lista. Mostra as duas datas (atendido em, lançado em), e o retroativo fica visível
  pela diferença.
- Filtros: período (padrão: o mês de hoje), status e, para o admin, prestador.
- Ativos e inativos, como na listagem atual; o desfeito aparece marcado como "Desfeito" e não
  entra no total.
- O total do período filtrado vem do backend, pela regra do rendimento.

## 4. Desenho do backend

**Data do lançamento**

- Coluna **`launched_at DATETIME NOT NULL`**, na V1 (migrations editáveis até a versão estável,
  [ADR 0028](../adr/0028-migrations-editaveis-ate-versao-estavel.md)): quem puxar zera o banco
  local. Sem banco de produção, não há dado antigo a preencher.
- `DATETIME`, não `DATE`: guarda a ordem dos lançamentos no mesmo dia e serve a uma auditoria
  futura. O filtro por dia usa o intervalo `[início do dia, início do dia seguinte)`.
- O **service** passa o `LocalDateTime.now()` (fuso da clínica) ao construtor da entidade, que
  valida que veio (`validateLaunchedAt`). A entidade não chama o `now()`: os testes de unidade
  recebem a data pronta (ver "Unidade" em "Testes" no [`architecture.md`](../architecture.md)).
- Índice `idx_consultation_provider_launched (provider_id, launched_at)`: as leituras do
  dashboard filtram sempre por prestador (ou todos) e período.
- Caso novo, sem precedente: nenhuma tabela do projeto grava data e hora hoje.

| Arquivo | Mudança |
|---|---|
| `V1__initial_schema.sql` | coluna `launched_at` e o índice |
| `ConsultationEntity` | campo `launchedAt`, parâmetro novo no construtor, `validateLaunchedAt` |
| `ConsultationService.create` | passa o `LocalDateTime.now()` |
| `ConsultationResponseDTO` | campo `launchedAt` |

**Rendimento e listagem**

| Arquivo | Mudança |
|---|---|
| `ConsultationStatus` | `isChargedAbsence()`: `NO_SHOW` e `CANCELED_LATE` (princípio 6) |
| `ConsultationRepository` | `sumEarnings` (nativa): quantidade e soma do `payout_value` dos ativos no período do lançamento, por prestador e status (`group by`), com prestador opcional; `search` com os filtros novos (`launchedFrom`, `launchedBefore`, `status`) |
| `ConsultationService` | `findEarningsSummary` (hoje, semana, mês) e `findEarnings` (período qualquer, 400 se o início vier depois do fim); `resolveProviderIdForEarnings`: `providerId` opcional para o admin (404 se não existir), sempre o próprio para o `PROVIDER`; o `EarningsTally` separa o rendimento das faltas |
| `EarningsSummaryResponseDTO` (novo) | `today`, `week`, `month`, cada um um `EarningsResponseDTO` |
| `EarningsResponseDTO` (novo) | `from`, `to`, `consultationCount`, `payoutTotal` (os realizados) e `chargedAbsenceCount`, `chargedAbsencePayoutTotal` (as faltas cobradas, à parte), `totalPayout` (os dois somados, o repasse total) e `byProvider` |
| `ProviderEarningsResponseDTO` (novo) | `providerId`, `providerName` e os mesmos números, por prestador |
| `DaySummaryResponseDTO` | as faltas cobradas à parte (`chargedAbsenceCount`, `chargedAbsencePayoutTotal`) |
| `ConsultationController` | `GET /api/consultations/earnings-summary?providerId` e `GET /api/consultations/earnings?providerId&from&to`; `launchedFrom`, `launchedTo` e `status` no `GET /api/consultations`; `@Operation` com as regras |
| `SecurityConfig` | as duas rotas novas para `ADMIN` e `PROVIDER` |

- **Soma no banco**, não no service como era o `day-summary`: o mês de todos os prestadores
  carregaria centenas de entidades só para somar. **SQL nativo** pela coluna `payout_value`
  (decidido em 2026-11-25, seção 1): primeiro caso de consulta nativa do projeto.
- **Uma consulta por período:** o `sumEarnings` agrupa por prestador e status; o service soma
  as linhas para o total e, para o admin sem prestador, monta o `byProvider` com as mesmas
  linhas (sem segunda consulta). O resumo faz três (hoje, semana, mês).
- **`byProvider`:** só no `earnings` do admin sem `providerId` (vazio nos demais e no resumo).
  Lista os prestadores ativos (zerados sem lançamento) e os inativos com lançamento, por nome.
- **`day-summary` alinhado:** usa a mesma consulta do rendimento de hoje.
- **Listagem com dois períodos:** os parâmetros atuais `from` e `to` continuam pela data do
  atendimento (a tela de Lançamento usa); os novos `launchedFrom` e `launchedTo` filtram pela do
  lançamento (`[início do dia, início do dia seguinte)`). A ordem continua pela data do
  atendimento. *A conferir:* se a listagem do mês de todos os prestadores precisa de paginação.

## 5. Desenho do frontend

**Layout escolhido nas prévias** (2026-11-24). Foram comparadas três versões: A, cartões no
topo; B, filtros numa coluna lateral; C, o gráfico no centro. A escolhida foi a **C, com
abas**.

**Filtros refeitos em 2026-11-26** (nova rodada de prévias, depois de ver a tela pronta: os
botões de período tinham a mesma cara dos cartões de números, e não se via o que era clicável).
Comparadas: A, uma barra com o período e um seletor de prestador; B, os dois filtros em menus no
cabeçalho; C, a barra de período e a lista por prestador ao lado dos números. A escolhida foi a
**C**, com as abas acima de tudo:

- **Controle tem cara de controle; informação, de cartão.** Os filtros ficam em controles com
  borda ou trilho (segmentado, seletor); os cartões brancos com sombra são só leitura. O valor
  sai dos botões de período: ele já está no cartão "Rendimento".
- **Ordem:** título, as abas, a barra de período (vale para as duas abas, fora do slide) e o
  conteúdo da aba.
- **Barra de período:** Hoje, Esta semana, Este mês e Personalizado num controle segmentado, com
  o intervalo ao lado; no Personalizado, as duas datas entram no lugar do intervalo.
- **Para o admin, a lista "Rendimento por prestador"** ao lado do conteúdo das duas abas: Todos
  e cada prestador, com o rendimento e uma barra da parte de cada um no total (só a largura do
  desenho; nenhum valor é calculado). A lista tem **a mesma altura do conteúdo** ao lado e rola
  por dentro quando passa dela. Clicar numa linha filtra a tela. O `PROVIDER` não tem a lista.
- **Aba "Resumo"** (a primeira, **sem gráficos**): três cartões (rendimento, com a quantidade de
  atendimentos; faltas cobradas; repasse total) e a lista de atendimentos, com o filtro por
  status num seletor. A lista é uma tabela num **componente próprio**, no `DataTable` das
  listagens (decidido em 2026-11-24).
- **Aba "Panorama executivo"** (com o gráfico; simplificada em 2026-11-25): só a **pizza da
  composição do repasse** (seção 3), com a legenda dos valores. Saíram as barras por dia, a
  linha de atendimentos e o gráfico e a tabela por prestador.
- O gráfico só carrega no Panorama: o Resumo, que é o uso do dia a dia, não paga o custo do
  ECharts. O Panorama é montado na primeira visita (componente `lazy`) e continua montado
  depois, para a troca de aba não recriar os gráficos.
- **Ícone à esquerda de cada aba:** o `monitor-dashboard` no Resumo e o `table` no Panorama,
  ambos do Material Design Icons. O projeto usa o `lucide-react`, que não tem esses dois
  desenhos. Para não instalar mais uma biblioteca de ícones por causa de dois, eles entram
  como SVG num componente do módulo (licença Apache 2.0 do MDI, citada no arquivo).
- **Animação de slide entre as abas:** as duas telas ficam lado a lado num trilho, e a troca
  desliza para o lado (para a esquerda ao ir ao Panorama, para a direita ao voltar). A linha
  da aba ativa acompanha, na mesma curva do menu lateral (`--shell-motion`).
- **A tabela sempre na tela** (2026-11-26): a página ocupa a altura da tela, como a de
  Lançamento; o título, as abas e os filtros ficam parados, os painéis têm a altura que sobra, e
  a tabela do Resumo rola por dentro, com o cabeçalho fixo (o `DataTable` ganhou o cabeçalho
  `sticky` e a rolagem nos dois eixos quando o pai limita a altura). Numa tela baixa, a tabela
  guarda uma altura mínima e o painel rola. Com `prefers-reduced-motion`, troca sem animação. A aba fora da tela
  fica `inert` (sem foco pelo teclado).
- *Descartadas:* abas de período, que somariam duas linhas de filtro no topo, e abas por
  prestador, que rolariam para o lado com muitos prestadores.
- **Repasse total** (rendimento + faltas cobradas) vem do backend, num campo do
  `EarningsResponseDTO`: o front não soma.

**ECharts**

- Pacote **`echarts`** (dependência nova, instalada com a permissão do usuário).
- **Import modular** (`echarts/core` + só os gráficos e componentes usados, registrados com
  `echarts.use`), não o `import * as echarts`: o pacote inteiro passa de 1 MB, e o modular leva
  só o que a tela usa. A rota `/dashboard` já é `lazy`: o ECharts só baixa quando o usuário abre
  o dashboard.
- **Registro só da pizza** (`PieChart` e o renderizador canvas), num lugar só, junto do
  `Chart.jsx`; um gráfico novo registra o tipo dele ali. O centro e a legenda são HTML, no
  componente do gráfico.
- **Componente próprio** `shared/components/Chart.jsx`: recebe o `option` do ECharts, cria o
  gráfico num `useEffect`, acompanha o tamanho do contêiner (`ResizeObserver`, por causa do menu
  que empurra o conteúdo) e chama o `dispose` ao desmontar. Descartado o `echarts-for-react`:
  mais uma dependência para um invólucro de poucas linhas.
- **Cores da marca:** o `option` lê as variáveis CSS do projeto (`var(--color-*)`), para o
  gráfico seguir a paleta (ver "Estilo" no [`architecture.md`](../architecture.md)).
- **Valores:** o eixo e o tooltip formatam em reais com o `shared/utils/money.js`.
- Montar o `option` da pizza fica em `domain/consultation/display/` (formato, sem regra); a
  pizza é um componente do módulo (`PayoutCompositionChart`) que usa o `Chart.jsx`.

| Arquivo | Mudança |
|---|---|
| `frontend/package.json` | dependência `echarts` |
| `shared/components/Chart.jsx` (novo) | invólucro do ECharts |
| `domain/consultation/api/consultationApi.js` | `getEarningsSummary`, `getEarnings`, filtros novos do `listConsultations` |
| `domain/consultation/api/useConsultations.js` | um hook por endpoint |
| `domain/consultation/pages/DashboardPage.jsx` (novo) | a tela: período, prestadores, abas, números, pizza e lista |
| `domain/consultation/components/` (novos) | seletor de período, botões dos prestadores (admin), números do Resumo, tabela de atendimentos (componente próprio, no `DataTable`), abas com o slide, Panorama (`lazy`) com a pizza (`PayoutCompositionChart`), ícones das abas |
| `domain/consultation/display/` (novos) | o `option` da pizza |
| `domain/consultation/components/DaySummary.jsx` | "Repasse do dia" passa a ser o rendimento (só os realizados); as faltas cobradas à parte |
| `app/router.jsx` | rota `/dashboard` no grupo `ADMIN` e `PROVIDER` |
| `app/layout/AppSidebar.jsx` | item do menu |

- No módulo `consultation`: o dashboard só lê atendimentos (o mapa de módulos do front é o do
  backend).
- **Tela inicial sem mudança** (decidido em 2026-11-24): o `PROVIDER` abre no Lançamento, o uso
  do dia a dia; o `ADMIN`, em Pacientes.
- Valores como vêm da API, formatados só na exibição (`shared/utils/money.js`).

## 6. Tarefas

**Decisões**

- [x] Fechar os itens *em aberto* da seção 3 e da seção 5.
- [x] Escolher a prévia da tela (C, com as abas "Resumo" e "Panorama executivo").
- [x] Simplificar os gráficos: uma pizza da composição do repasse (2026-11-25).

**Backend**

- [x] `launched_at` na V1, na entidade, no service e no DTO.
- [x] Consultas do rendimento e filtros novos da listagem.
- [x] Endpoints `earnings-summary` e `earnings`, com o `@Operation`.
- [x] Rotas no `SecurityConfig`.
- [x] Alinhar o `day-summary` (backend).
- [x] `./mvnw -q compile`.
- [x] Conferir a consulta nativa `sumEarnings` rodando no MySQL (pelo `EarningsIntegrationTest`,
  2026-11-26).

**Frontend**

- [x] Instalar o `echarts` (6.1.0, 2026-11-26).
- [x] `Chart.jsx` em `shared/components/` (registra só a pizza, a legenda e o tooltip).
- [x] Camada `api/` e hooks (2026-11-26): `getEarningsSummary`, `getEarnings` e os filtros
  novos do `listConsultations`; `useEarningsSummary`, `useEarnings` e
  `useLaunchedConsultations`. O filtro por status da lista roda em memória, na page (lista
  pequena, ver "Camada `api/`" no `architecture.md`); os períodos Hoje, Semana e Mês usam o
  `from`/`to` do `earnings-summary`.
- [x] Tela, pizza, rota e menu (2026-11-26): `DashboardPage`, `useDashboardPeriod`,
  `PeriodSelector`, `ProviderEarningsList`, `EarningsNumbers`, `LaunchedConsultationsTable`,
  `DashboardTabs`, `DashboardTabIcons`, `PayoutCompositionChart` (`lazy`),
  `payoutCompositionOption.js`, `earningsLabels.js` e `filterConsultations.js`; rota no
  `router.jsx` e o `disabled` fora do menu.
- [x] "Repasse do dia" da tela de Lançamento com as faltas à parte (`DaySummary`, 2026-11-26):
  passa a "Rendimento do dia" (só os realizados), com o bloco "Faltas cobradas" (valor e
  quantidade) ao lado.
- [ ] `npm run build` e `npm run lint`.

**Testes**

- [x] Unidade: `ConsultationEntity` sem `launchedAt` → erro (`ConsultationEntityTest`,
  2026-11-26: guarda a data e a hora recebidas, a do lançamento separada da do atendimento no
  retroativo, e recusa a nula com a mensagem exata).
- [x] Integração: os casos da seção 7 pela API (o rendimento, os períodos e o acesso por
  perfil), no `EarningsIntegrationTest` (15 casos; 2026-11-26). A suíte inteira passa (64).
- [ ] Manual pela tela.

**Conclusão**

- [ ] Seção 9 e seção 10.

## 7. Casos de teste

**Pré-requisitos:** banco zerado (V1 editada), dois prestadores com usuário (A e B), um paciente
com contrato e um sem; liberação de retroativo para o A.

**Rendimento (pela API)**

| # | Caso | Esperado |
|---|---|---|
| 1 | A lança hoje um `ATTENDED` | o `today`, o `week` e o `month` do A sobem 1 e o repasse dele |
| 2 | A lança hoje um `NO_SHOW` e um `CANCELED_LATE` | o rendimento não muda; as faltas cobradas sobem 2 e o repasse delas |
| 3 | A lança hoje um retroativo `ATTENDED` com a data de 5 dias atrás | conta no `today` do A |
| 4 | A desfaz o lançamento do caso 1 | o rendimento volta ao de antes |
| 5 | `earnings` com um período sem lançamentos | quantidade 0 e repasse 0,00 |
| 6 | O `from` e o `to` do `week` e do `month` | os limites da regra aprovada |
| 6a | `earnings` com o `from` depois do `to` | 400 |
| 6b | `earnings` do `ADMIN` sem `providerId`, com um prestador ativo sem lançamento | o `byProvider` traz o prestador zerado; a soma do `byProvider` bate com o total |
| 6c | `day-summary` depois dos casos 1 e 2 | o rendimento só com o `ATTENDED`; as faltas à parte |

**Acesso por perfil**

| # | Caso | Esperado |
|---|---|---|
| 7 | A pede o resumo com o `providerId` do B | 200 com os dados do A |
| 8 | Usuário `PROVIDER` sem prestador vinculado | 403 |
| 9 | `ADMIN` sem `providerId` | 200 com o total de A e B |
| 10 | `ADMIN` com o `providerId` do B | 200 só com o B |
| 11 | Sem token | 401 |

**Lista**

| # | Caso | Esperado |
|---|---|---|
| 12 | Listagem com o período do lançamento de hoje | inclui o retroativo do caso 3 |
| 13 | Listagem pela data do atendimento (`from`/`to`, a de antes) | continua como era |
| 14 | Filtro por status | só o status escolhido |

**Pela interface:** os cartões e a lista do `PROVIDER` e do `ADMIN`, o filtro por prestador, o
retroativo e o desfeito na lista, o total do período filtrado, a pizza (acompanha o filtro,
redimensiona ao abrir e fechar o menu, valores em reais no tooltip, período sem repasse), o
"repasse do dia" da tela de Lançamento e o tamanho do bundle no
`npm run build` (o ECharts só no chunk do dashboard).

## 8. Pendências e pontos abertos

- Paginação da listagem (seção 4).

## 9. Documentação ao concluir

- **`domain-model.md`**, em `Consultation`: o atributo `launchedAt`; uma seção "Rendimento"
  com a regra (só `ATTENDED`, ativos, pela data do lançamento, os períodos); os endpoints novos
  e os filtros da listagem em "Leituras"; o `day-summary`, se mudar.
- **`architecture.md`:** a data e hora do lançamento no fuso da clínica, na linha "Hoje"; na
  seção "Frontend", o ECharts na stack e o padrão dos gráficos (`Chart.jsx`, import modular, o
  `option` em `display/`, números do backend, cores lidas das variáveis CSS, legenda em HTML).
  Em "Rotas e sessão": o `/dashboard` no grupo `ADMIN` + `PROVIDER`, o Dashboard no menu dos
  dois perfis e a frase "Itens sem tela pronta (Dashboard) ficam desabilitados" fora.
- **`roadmap.md`:** tirar o item 4 dos Próximos passos; marcar o "Histórico de atendimentos com
  filtro por período" do checklist do front; o módulo `consultation` no Estado atual do front;
  o que a seção 2 deixou para depois.
- **`architecture.md`, linha "Hoje"** (correção achada pelo `EarningsIntegrationTest`,
  2026-11-26): nos testes, o bloco `static` do `SublimeAppApplication` não rodava "antes do
  Spring subir", mas no meio da subida, com o pool já aberto em UTC; o driver lia as datas
  (`DATE`) um dia antes. O `TestcontainersConfiguration` passou a inicializar a classe antes do
  container. Corrigir a frase "que roda também nos testes de integração (eles carregam a classe
  sem chamar o main)".
- **`development.md`:** nada, salvo se a feature mudar o seed.
- **Swagger:** o `@Operation` dos endpoints novos e da listagem.
- **PR:** aviso "zerem o banco local" (V1 editada).

## 10. ADRs a registrar

- **Rendimento do prestador:** só `ATTENDED`, pela data do lançamento. Descartados: os três
  status cobrados (o rendimento seria o repasse devido inteiro) e a data do atendimento (um
  retroativo mudaria um período já visto). Registrar com a decisão sobre as faltas cobradas.
- **Biblioteca de gráficos do front: ECharts**, com import modular e invólucro próprio.
  Descartados: o Recharts (componentes React, mas SVG, mais lento com muitos pontos e menos
  tipos de gráfico), o Chart.js (canvas e leve, menos recursos de interação) e o
  `echarts-for-react` (dependência a mais). É decisão grande do front (vale para todo gráfico
  futuro e é cara de trocar), por isso ADR.
