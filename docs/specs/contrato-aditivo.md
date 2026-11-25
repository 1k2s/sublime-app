# Spec — Aditivo do contrato

Tela do aditivo: alterar um contrato ativo (vigente ou vencido) gerando uma nova versão
(prorrogar, incluir e remover itens, trocar beneficiário, forma de pagamento e parcelas), com
prévia do resultado antes de salvar.

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

*Atualizado em 2026-11-25.*

- **Pausado em 2026-11-25** (troca de computador), com o código pronto e testado; falta a
  conclusão (tarefas da seção 6).
- **Em duas branches, PRs para a `dev` com "merge commit":**
  - **`feature/contrato-aditivo`**: o backend R1 a R5, **já na `dev`** (PR #56), com o aviso
    "zerem o banco local" (colunas novas na `V1`).
  - **`feature/contrato-aditivo-front`** (atual, criada da `dev` depois do #56): o frontend (seção
    5), a R6 no backend (item por duração em contrato de mensalidade) e os ajustes do teste da
    tela. A conclusão (documentação e ADRs) entra neste PR.
- **Feito:** a análise, o desenho, o backend (R1 a R6, testes de unidade em
  `ContractEntityAmendmentTest` e `ContractEntityMonthlyFeeTest`) e o frontend (`npm run build` e
  `npm run lint`), **testado pela tela**, com os ajustes registrados na seção 5. **Pela API, todos
  os casos dos blocos A, B, C, D e F passaram** (por script, com o banco zerado).
- **Para continuar noutro computador:** zerar o banco local (`docker compose down -v` e `up -d`)
  e subir o backend. Os scripts de dados e dos casos da API foram usados só nesta sessão e **não
  estão no repositório**; para testar de novo pela tela, cadastre os dados pela própria tela ou
  pelo Swagger (pré-requisitos da seção 7).
- **Próximo:** a conclusão (seção 6, "Conclusão"), em partes: `domain-model.md` → `architecture.md`
  → ADRs → `roadmap.md`, remoção da spec e conferências; depois o commit e o PR desta branch. A
  decisão da tabela clara (seção 8) pode ficar para depois.
- **O que o backend tinha antes da feature** (ver "Versionamento (aditivos)" em `Contract`, no
  [`domain-model.md`](../domain-model.md)):
  - `POST /api/contracts/{id}/amendments`: gera a nova versão com o estado completo (item que
    não vier é removido); titular, plano e início copiados; `endDate` opcional, só para
    estender. Hoje **copia as parcelas** (`ContractEntity.amend`).
  - `GET /api/contracts/{id}/group-plan-options`: opções do plano do Grupo pela vigência atual.
  - `GET /api/consultations/session-balance?contractId`: saldo de cada item por duração.
  - A resposta do contrato traz `previousContractId` (a versão anterior), mas não diz se a
    versão foi substituída nem o número da versão.
- **O que o frontend já tem:** a tela de Contratos com o cadastro (`ContractForm`,
  `ContractItemFields`, `useContractDraft`), o detalhe (`ContractDetailsModal`) e a prévia da
  criação. Hoje prorrogar só pelo Swagger.

## 2. Objetivo e escopo

O admin altera um contrato pela tela, vendo antes de salvar o novo fim, o valor de cada item, o
total e as parcelas. Depende de `contract` (backend e front) e lê o saldo de sessões do
`consultation`.

**Entra:**
- prorrogar (novo fim);
- incluir e remover itens, trocar técnica, linha de preço e frequência;
- trocar beneficiário e forma de pagamento;
- **escolher as parcelas** da nova versão (regra nova, R2);
- prévia do aditivo antes de salvar (endpoint novo, R1);
- o "Editar" no detalhe do contrato;
- a lista com **um contrato por linha** (a versão atual) e o histórico das versões no detalhe
  (R3).

**Fica para depois:**
- o controle de pagamentos (`billing`, Fase 2; esboço na seção 8): o aditivo não sabe o que
  já foi pago;
- o destino das sessões restantes de um item removido (vai para o saldo do paciente, no
  `billing`);
- a impressão da versão para assinatura.

## 3. Regras

Decididas em 2026-11-18 (R1 a R3) e 2026-11-25 (formato da API, R4 e R5), ainda não implementadas.

**R1. Prévia do aditivo** (aprovada): `POST /api/contracts/{id}/amendments/preview`, sem gravar.
- **Pedido próprio** (`ContractAmendmentPreviewRequestDTO`): só o que muda as contas: `endDate`
  (opcional, prorrogação), `items` (pode vir vazia ou incompleta) e `installments` (opcional; vazio
  ou acima do máximo = o máximo, e a resposta diz as aplicadas). O aditivo de verdade recusa
  acima do máximo. Mesmo padrão da criação, que tem a prévia (`ContractPreviewRequestDTO`)
  separada do cadastro.
- **Resposta:** o mesmo `ContractPreviewResponseDTO` da criação (novo fim, semanas, valor de
  cada item, total, máximo de parcelas, aplicadas e valores), com as opções do plano do Grupo pela
  **nova** vigência. A tela aproveita o que já mostra a prévia da criação.
- A conta do fim do aditivo fica pública (`calculateAmendedEndDate`, método da versão atual que
  recebe os rascunhos dos itens), para a prévia usar a mesma conta, como o `durationWeeksFor` da
  criação. A prévia não chama o `amend` (ele inativa a versão atual).
- *Por quê:* o fim do aditivo segue outra regra que o da criação (mantém o fim atual, estende
  se pedido, ou recalcula a partir da data do aditivo quando entra um item por duração). Sem
  prévia, o admin só vê o resultado depois de salvar, e a correção é outro aditivo.
- *Descartadas:* reaproveitar o `POST /api/contracts/preview` da criação (calcula o fim pelo
  início); calcular no front (regra do backend); usar o pedido do aditivo na prévia (exige forma
  de pagamento e itens, e daria 400 com o formulário em preenchimento; forma de pagamento e
  beneficiário não mudam as contas).

**R2. Parcelas no aditivo** (aprovada): o admin escolhe o número de parcelas **em qualquer
aditivo**, de 1 até os meses da **nova** vigência (máximo 12), a mesma regra da criação
(`Installments.validateCount`). As parcelas dividem **o total da nova versão**. Mensagem acima
do máximo: a mesma da criação ("O número de parcelas deve ser de 1 a N (meses do contrato).").
- **Formato:** `installments` obrigatório no `ContractAmendmentRequestDTO`, com as mesmas
  anotações do cadastro (`@NotNull`, 1 a 12). O `amend` recebe as parcelas e as valida depois de
  calcular o novo fim. Sem migration (a coluna `installments` já existe).
- *Por quê:* as parcelas são só o **plano de pagamento**; a dívida do paciente é o consumo
  (esboço do `billing`, seção 8). Incluir ou remover um item muda o total sem mexer no fim, e
  prorrogar muda o máximo.
- *Descartadas:* copiar sempre as parcelas (hoje: o total muda e as parcelas não); renegociar
  só na prorrogação (incluir um item também muda o total); descontar as parcelas já pagas (o
  sistema não sabe o que foi pago até o `billing`).
- Substitui em parte o [ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md) ("Aditivo:
  copia as parcelas da versão anterior").

**R3. Lista com um contrato por linha** (aprovada): a lista mostra **só a versão atual** de cada
contrato, com a indicação do aditivo (ex: "Aditivo 2"); o histórico das versões fica no detalhe.
O status "Inativo" passa a ser só o contrato inativado.
- *Por quê:* hoje um contrato com dois aditivos vira três linhas, duas como "Inativo", iguais a
  um contrato encerrado.
- Precisa de dado novo do backend: o número da versão e se ela foi substituída. Montar isso no
  front cruzando a lista seria reimplementar a cadeia de versões.
- *Descartada:* todas as versões na lista, com a substituída como "Substituído".
- **Formato:**
  - `GET /api/contracts` devolve **só a versão atual** de cada contrato (a que nenhuma outra
    substituiu: `not exists` no repository). O inativado continua, como "Inativo". Só a tela de
    Contratos usa a listagem.
  - `GET /api/contracts/{id}/versions` (novo): a cadeia inteira, da mais nova para a mais antiga,
    para o histórico no detalhe.
  - `ContractResponseDTO` ganha `amendmentNumber` (0 na original, 1 no primeiro aditivo...; a tela
    mostra "Aditivo N" sem conta) e `superseded` (substituída por um aditivo). Hoje substituída e
    inativada aparecem iguais (`active: false`); as duas não se misturam (versão substituída não
    pode ser inativada).
- **`amendmentNumber` gravado** numa coluna (`amendment_number`), preenchida no `amend` com o da
  versão anterior + 1 e nunca alterada. O `previousContract` continua sendo a ligação entre as
  versões; a coluna guarda o resultado da contagem.
  - *Por quê:* contar pela cadeia (`getVersionChainIds`) carrega cada versão anterior (relação
    LAZY, uma consulta por passo); na lista, seriam consultas a mais por contrato a cada abertura
    da tela. Gravando, a conta é feita uma vez, no aditivo, com a versão anterior já carregada.
  - *Descartadas:* contar pela cadeia a cada leitura; consulta recursiva no SQL (`WITH
    RECURSIVE`: SQL nativo, que o projeto não usa, só para isso).
- **`createdAt` gravado** numa coluna (`created_at`, `DATE`): o dia em que a versão foi criada
  (cadastro ou aditivo), "hoje" no fuso da clínica, nunca alterado. Na resposta, `createdAt`.
  - *Por quê:* o contrato só tinha o início do acordo (`start_date`, copiado igual em todas as
    versões) e o fim; o histórico não tinha como mostrar quando cada aditivo foi feito. E a data
    do aditivo já entra na conta do fim (item por duração incluído começa a contar nela) sem ser
    guardada: com a coluna, a versão guarda a data que a calculou.
  - `DATE`, como o `occurred_at` do atendimento: o histórico precisa do dia.
- **`superseded` calculado**, não gravado: na lista é sempre falso (só vêm as atuais); no
  `/versions`, sai da posição (todas menos a primeira) e, para a primeira, da mesma consulta do
  detalhe (pedida a partir de uma versão do meio, ela é substituída); no `GET /{id}`, uma consulta
  só ("existe versão com `previous_contract_id` = este id?", coluna com índice `UNIQUE`).

**R4. Sai o `GET /api/contracts/{id}/group-plan-options`** (aprovada): ele existe só para o
aditivo e calcula as opções do plano do Grupo pela vigência **atual**, mas a validação do aditivo
usa a **nova** (`ContractService.validateGroupItemPlan`). A prévia (R1) traz as opções certas, e
nenhuma tela usa o endpoint.

**R5. Aditivo sem alteração é recusado** (aprovada em 2026-11-25): a API recusa o aditivo igual à
versão atual, com 400 "Nenhuma alteração no contrato.". A tela já bloqueia ("Prosseguir"
inativo, com "Nenhuma alteração"; seção 5).
- **O que conta como alteração** (comparado com a versão atual): algum item incluído ou removido;
  num item mantido, outra técnica, outra linha de preço ou outra frequência; outro beneficiário;
  outra forma de pagamento; outro número de parcelas; `endDate` informado (a prorrogação, que já
  precisa passar do fim atual).
- **Onde:** no `ContractEntity.amend`, depois de montar a nova versão, num método de validação com
  nome descritivo (princípio 7).
- *Por quê:* o backend é a fonte da verdade; regra só na tela pode ser contornada pela API, e uma
  versão idêntica só cria um "Aditivo N" vazio no histórico.
- A tela compara por conta própria para mostrar as "Alterações" (exibição). Se as duas comparações
  divergirem num caso de borda, o confirmar devolve o 400 com a mensagem, e o admin volta ao
  formulário.

**R6. Item por duração em contrato de mensalidade** (aprovada em 2026-11-25, no teste da tela): o
contrato de plano de mensalidade (só com o Pilates em Grupo) passa a aceitar itens por duração, na
criação e no aditivo. Antes, ele aceitava só o item do Grupo, e incluir outra técnica exigia um
contrato novo.
- **Linha do item:** do **plano equivalente** do de-para (`PlanEntity.planFor`: Mensal →
  Essencial, Trimestral → Evolução, Semestral → Transformação, Anual → Vitalidade, Familiares →
  Familiares), fixo. Plano sem equivalente: 400 (a mensagem do `planFor`). É o caminho inverso do
  item do Grupo no pacote, e o mesmo que o lançamento já usa para uma sessão por duração de quem só
  tem o Grupo.
- **Vigência:** o maior prazo entre o do plano de mensalidade (`termWeeks`) e o dos itens por
  duração (sessões do pacote equivalente na frequência). No aditivo, vale a regra que já existe: o
  item incluído conta da data do aditivo e pode estender o fim.
- **No aditivo, o item com a mesma linha da versão anterior é aceito sem reconferir o plano**
  (`ContractEntity.hasSameLineAsPrevious`). *Por quê:* o de-para pode mudar depois (o admin troca o
  equivalente do plano); sem isso, um item antigo, com a linha do equivalente de antes, travaria todo
  aditivo seguinte do contrato. Mesma ideia do preço e da técnica do item mantido, aceitos como estão.
- **O item do Grupo continua obrigatório** no contrato de mensalidade (400 sem ele), espelhando o
  "pacote precisa de pelo menos um item por duração": para ficar só com itens por duração, contrato
  novo de pacote.
- *Por quê:* no teste, o contrato da Carla (Trimestral) não deixava incluir nenhuma técnica, e
  "faça outro contrato" não é como a clínica trabalha. O de-para já existe nos dois lados para esse
  cruzamento.
- *Descartadas:* escolher o pacote numa lista, como o plano do Grupo no pacote (o de-para já é a
  regra da clínica, sem mais uma escolha na tela); só no aditivo (a regra é da entidade e vale na
  criação); permitir o contrato de mensalidade sem o item do Grupo.
- Substitui em parte os ADRs 0008 ("contrato só com Grupo usa plano de mensalidade"), 0009
  (vigência do contrato só com Grupo) e 0011 (sem exceção para o contrato só com Grupo).

## 4. Desenho do backend

Módulo `contract`:

| Arquivo | Mudança |
|---|---|
| `ContractController` | `POST /{id}/amendments/preview` (R1); `GET /{id}/versions` (R3); sai o `GET /{id}/group-plan-options` (R4); `@Operation` do aditivo com as parcelas (R2) e da listagem com "só a versão atual" (R3) |
| `ContractAmendmentPreviewRequestDTO` (novo) | `endDate`, `items` e `installments`, todos opcionais (R1) |
| `ContractAmendmentRequestDTO` | `installments` obrigatório, 1 a 12 (R2) |
| `ContractResponseDTO` | `amendmentNumber`, `createdAt` e `superseded` (R3) |
| `ContractGroupPlanOptionsResponseDTO` | removido (R4) |
| `ContractEntity` | `amend` recebe e valida as parcelas (R2), recusa o aditivo sem alteração (R5) e grava o `amendmentNumber` (R3); a conta do fim do aditivo fica pública e estática (R1); campos `amendmentNumber` e `createdAt` (preenchido no `create` e no `amend` com "hoje", passado pelo service, como o `amend` já recebe a data do aditivo) |
| `ContractEntity` (R6) | `durationWeeksFor`: no plano de mensalidade, o maior entre o `termWeeks` e o prazo dos itens por duração (sessões do pacote equivalente); `validateItemMatchesPlan`: item por duração com linha do `plan.planFor(DURATION_BASED)`; `validateItemsMatchPlan`: plano de mensalidade exige o item do Grupo; item com a mesma linha da versão anterior aceito como está (`hasSameLineAsPrevious`) |
| `ContractService` | prévia do aditivo (R1); listagem só das atuais, `versions` e o `superseded` (R3); sai o `groupPlanOptionsFor(Long)` (R4) |
| `ContractRepository` | consulta das versões atuais (`not exists`) e "existe sucessora" (R3) |
| `V1__initial_schema.sql` | colunas `amendment_number INT NOT NULL DEFAULT 0` e `created_at DATE NOT NULL` em `contract` (R3; migration editável, [ADR 0028](../adr/0028-migrations-editaveis-ate-versao-estavel.md): aviso "zerem o banco local" no PR) |

## 5. Desenho do frontend

Desenho aprovado em 2026-11-25, a partir das prévias comparadas (versões descartadas no fim da
seção).

**Fluxo:** lista → botão de ações → detalhe (aba Contrato) → "Editar" → passo 1 (formulário) →
"Prosseguir" → passo 2 (revisão) → "Confirmar aditivo" → tudo fecha, com a mensagem "Aditivo
salvo." e a lista atualizada (como o cadastro).

**Passo 1, formulário do aditivo** (modal grande, o mesmo do cadastro, já preenchido):
- No topo, travados: titular, plano, início e o fim atual.
- Itens como no cadastro; cada item mostra o saldo de sessões (todas as versões); o item incluído
  aparece marcado como "Novo".
- "Vigência e pagamento": "Prorrogar até" (opcional), forma de pagamento e parcelas (das opções da
  prévia do aditivo, R1).
- Rodapé fixo com o "antes → depois" do fim e do total (com as parcelas), da prévia do aditivo.
- Botões: "Cancelar" e "Prosseguir →". **Sem nenhuma alteração, "Prosseguir" fica
  inativo, com "Nenhuma alteração" ao lado.**

**Passo 2, revisão** (modal, título "Revisar aditivo · {titular}", sem subtítulo):
- Bloco **"Alterações"** (título em destaque), **dinâmico: só as linhas do que mudou**:

  | Linha | Aparece quando | Compara |
  |---|---|---|
  | Adicionado: {técnica} ({duração}, {frequência}, {sessões}, {valor}) | grupo novo | formulário × versão atual |
  | Removido: {técnica} (restavam N sessões) | item da versão atual saiu | formulário × versão atual |
  | Alterado: {técnica}, de {antes} para {depois} | técnica, duração ou frequência mudou | formulário × versão atual |
  | Novo beneficiário: {antes} → {depois} | beneficiário mudou | formulário × versão atual |
  | Nova forma de pagamento: {antes} → {depois} | forma mudou | formulário × versão atual |
  | Novo fim: {antes} → {depois} | fim mudou | prévia × versão atual |
  | Novo total: {antes} → {depois} | total mudou | prévia × versão atual |
  | Novas parcelas: {antes} → {depois} | quantidade ou valor mudou | prévia × versão atual |

  Lista vazia = "Nenhuma alteração" do passo 1 (a mesma comparação). A comparação é só de
  exibição (`display/amendmentChanges.js`): os valores vêm da API.
- Botões: "← Voltar" (volta ao passo 1 preenchido) e "✓ Confirmar aditivo".

**Lista** (um contrato por linha, R3): na coluna de status, o selo do estado (Vigente, Vencido,
Inativo) e, se houver aditivo, o selo "Aditivo N". A versão original fica só com o estado.
"Inativo" passa a ser só o contrato inativado.

**Detalhe, com abas:**
- **Contrato:** a versão atual, com o "Criado em" junto dos outros dados (plano, pagamento, início,
  fim, valor, parcelas, beneficiário) e os itens. Rodapé: "Inativar", "Fechar" e "Editar".
- **Histórico (N):** abre direto na versão mais nova (o que o último aditivo mudou), com a linha do
  tempo das versões no topo (ver abaixo) para abrir as outras: título com o número e o selo ("Atual"
  ou "Substituída"), os dados (com o "Criado em") e os itens. Rodapé: "← Voltar" (para a aba
  Contrato) e "Fechar". *Mudou no teste da tela:* antes, a aba abria numa lista de versões e cada
  linha abria a versão; com a linha do tempo, a lista ficou sobrando.
- **O que mudou, marcado na versão aberta** (decidido em 2026-11-25, no teste da tela): comparada
  com a versão anterior a ela (Aditivo 1 × Original, Aditivo 2 × Aditivo 1...). Os dados que mudaram
  (forma de pagamento, fim, beneficiário, total, parcelas) ficam em azul (os tokens de sugestão),
  com "antes: {valor}" embaixo; o item incluído ou alterado fica em azul com o selo "Novo" ou
  "Alterado"; o item removido aparece no fim, riscado, com o selo "Removido". O Original e a aba
  Contrato não têm marcação. Só exibição (`display/versionChanges.js`), com o critério de item da R5.
- **Altura fixa nas duas abas:** o modal não muda de tamanho ao trocar de aba; a lista de itens (aba
  Contrato e versão aberta) e a de versões rolam por dentro.
- **Versão aberta com a linha do tempo** (escolhida em 2026-11-25 entre três prévias: barra com
  moldura, linha do tempo, faixa escura no lugar das abas): no topo, as versões da original para a
  atual, com a aberta em azul; clicar noutra abre aquela versão. Os dados e os itens ficam em
  molduras de fundo claro, separadas: borda sólida na versão atual (também na aba Contrato) e
  tracejada nas versões anteriores. *Por quê:* a versão aberta tinha o mesmo desenho da aba
  Contrato, e parecia outra aba.
- **Tabela da lista de Contratos mais leve** (escolhida em 2026-11-25 entre três prévias:
  cabeçalho claro, cabeçalho verde suave, só linhas): cabeçalho claro (o fundo da moldura do
  detalhe; um bege testado na tela não ficou bom) com rótulos em cinza e
  borda de 2px embaixo; linhas brancas, sem listra; com o mouse em cima, cinza claro neutro (o verde
  se confundia com o das etiquetas) e barra verde à esquerda; a linha inteira abre o detalhe
  (`onRowClick` do `DataTable`, com o cursor de mão), e a linha do contrato aberto fica marcada
  como no hover enquanto o detalhe ou o aditivo está aberto (`selectedRowKey`). O botão do olho usa
  o `IconButton variant="soft"` (no hover, o verde das etiquetas e o ícone maior); a etiqueta
  "Vigente" usa o mesmo verde da "Aditivo N". *Por quê:* o cabeçalho verde escuro e as listras pesavam perto dos modais. Criada como
  **variante da tabela compartilhada** (`DataTable variant="light"`), usada só na `ContractTable`;
  a tabela padrão das outras listagens não muda agora. **Se aprovada em Contratos, a variante vira
  o padrão da `DataTable`** (todas as listagens), decisão que mexe na tabela padrão da spec
  [`layout-das-telas.md`](layout-das-telas.md); pendência na seção 8.
- **Padrões do módulo** (2026-11-25): etiquetas em retângulo com cantos arredondados
  (`ContractBadge`, `shape="rounded"` do `Badge`); o plano do Grupo com uma opção só já vem
  escolhido (cadastro e aditivo); o paciente escolhido (titular, beneficiário) em negrito, com
  "Remover" que põe o cursor na busca.
- **O histórico é só para consultar.** Voltar ao estado de uma versão anterior é fazer um novo
  aditivo com aquele conteúdo (ex: para voltar ao Aditivo 2, cria-se o Aditivo 4). A API já só
  altera a versão ativa.

**Arquivos** (o recorte dos componentes pode mudar na implementação):

| Arquivo | Mudança |
|---|---|
| `api/contractApi.js` e `useContracts.js` | aditivo, prévia do aditivo e versões (`/versions`) |
| `components/ContractDetailsModal.jsx` | abas Contrato e Histórico, versão aberta, rodapés e "Editar" |
| componentes do aditivo (passo 1 e passo 2) | formulário a partir do `ContractForm`, `ContractItemFields`, `ContractTermSection` e `ContractResultBar` (antes → depois); revisão com as "Alterações" |
| `hooks/useContractDraft.js` | a prévia do aditivo no lugar da prévia da criação, no modo aditivo |
| `display/amendmentChanges.js` (novo) | a lista dinâmica das "Alterações" |
| `display/contractStatus.js` | "Inativo" só para o inativado |
| `components/ContractTable.jsx` | selo "Aditivo N" |
| `pages/ContractPage.jsx` | fluxo do "Editar" até a mensagem de sucesso |
| `api/contractApi.js` (`getSessionBalance`) | saldo de sessões por item, lido do endpoint do `consultation` (exceção ao mapa, seção 8) |

**Como ficou** (2026-11-25, depois do teste da tela):

| Arquivo | O que tem |
|---|---|
| `api/contractApi.js`, `api/useContracts.js` | `amendContract`, `getAmendmentPreview`, `listContractVersions`, `getSessionBalance`; `useAmendContract`, `useAmendmentPreview`, `useContractVersions`, `useContractSessionBalance`; `useContractPreview` com `enabled` |
| `components/ContractDetailsModal.jsx` | abas com a linha deslizante; Histórico com a linha do tempo (bolinha que desliza) e a versão aberta; Inativar e Editar na aba Contrato |
| `components/ContractVersionDetails.jsx` (novo) | dados e itens de uma versão, com as marcações do que mudou e a moldura (`frame`: sólida ou tracejada) |
| `components/ContractAmendmentReview.jsx` (novo) | passo 2, as "Alterações" em azul |
| `components/ContractBadge.jsx` (novo) | o `Badge` do módulo, retangular (`shape="rounded"`) |
| `components/ContractForm.jsx`, `ContractItemFields.jsx`, `ContractTermSection.jsx`, `ContractResultBar.jsx` | modo aditivo (prop `contract`): dados travados, beneficiário, saldo e "Novo" nos itens, "Prorrogar até", antes → depois, "Prosseguir"; plano do Grupo com uma opção já escolhido |
| `components/PatientPicker.jsx` | nome em negrito; "Remover" põe o cursor na busca |
| `components/ContractTable.jsx` | selo "Aditivo N"; tabela clara, linha clicável e marcada, olho com hover suave |
| `hooks/useContractDraft.js` | `amendedContract`: prévia do aditivo, linha travada do item mantido, pedido do aditivo, `formItems` |
| `display/amendmentChanges.js`, `display/versionChanges.js`, `display/contractVersions.js` (novos) | as "Alterações" do aditivo; o que mudou entre duas versões; rótulo e selo da versão |
| `display/contractStatus.js` | "Vigente" no verde do "Aditivo N" |
| `utils/amendmentFormValues.js` (novo), `utils/resolveItemLine.js` | formulário preenchido com a versão atual; `resolveAmendedItemLine` (linha travada) e o pacote equivalente na mensalidade (R6) |
| `pages/ContractPage.jsx` | fluxo Editar → passo 1 → passo 2 → "Aditivo salvo."; Cancelar volta ao detalhe |
| **Compartilhados** | `Badge` (`variant="suggestion"`, `shape="rounded"`), `Modal` (`size="medium"`), `DataTable` (`variant="light"`, `onRowClick`, `selectedRowKey`), `IconButton` (`variant="soft"`), `date.js` (`addDaysToIsoDate`); tokens `--color-table-head` e `--color-table-head-line` no `index.css` |

**Descartados nas prévias:** o aditivo em duas colunas (versão atual × nova; aperta em tela de
notebook); o aditivo sem revisão (o erro só se corrige com outro aditivo); o "Aditivo N" em texto
embaixo do plano (menos visível); o histórico em linha do tempo no fim do detalhe (cresce com
muitos aditivos e não abre cada versão).

## 6. Tarefas

**Desenho (parte 2):**
- [x] Tela do aditivo, lista e detalhe: prévias visuais e as escolhidas (seção 5).
- [x] Formato da prévia do aditivo e dos dados da versão na API (R1 a R4, seção 4).
- [x] Como a tela mostra o saldo de sessões (seção 8: função no `contractApi.js`).

**Backend** (feito em 2026-11-25):
- [x] Parcelas no aditivo (R2).
- [x] Prévia do aditivo, com o pedido próprio e a conta do fim pública (R1).
- [x] Colunas `amendment_number` e `created_at`; `amendmentNumber`, `createdAt` e `superseded` na
      resposta; listagem só das atuais e `GET /{id}/versions` (R3).
- [x] Remover o `group-plan-options` (R4).
- [x] Recusar o aditivo sem alteração (R5).
- [x] Testes de unidade das regras novas (`ContractEntityAmendmentTest`: parcelas no aditivo,
      aditivo sem alteração, número e data da versão, fim do aditivo).

**Frontend** (branch `feature/contrato-aditivo-front`; feito e testado pela tela em 2026-11-25):
- [x] "Editar" no detalhe e formulário do aditivo com a prévia.
- [x] Lista com um contrato por linha e histórico das versões no detalhe.

**R6, item por duração em contrato de mensalidade** (feito em 2026-11-25, na branch do front):
- [x] Backend: `ContractEntity` e testes (`ContractEntityMonthlyFeeTest`).
- [x] Front: todas as técnicas em qualquer plano (`useContractDraft`); linha do pacote equivalente
      (`resolveItemLine`).

**Ajustes do teste da tela** (feitos em 2026-11-25; seção 5): histórico com linha do tempo e
molduras, alterações marcadas, altura fixa e tamanho do detalhe, padrões do módulo (etiquetas,
plano do Grupo, paciente escolhido), tabela clara, Cancelar voltando ao detalhe.
- [x] Casos de teste: pela tela (bloco E) e pela API (blocos A, B, C, D e F).

**Conclusão** (pendente; fazer em partes, cada uma revisada antes da seguinte):
- [ ] Parte 1, [`domain-model.md`](../domain-model.md): as regras R1 a R6 (seção 9).
- [ ] Parte 2, [`architecture.md`](../architecture.md): a exceção ao mapa de módulos e as
      variantes novas dos componentes compartilhados (seção 9).
- [ ] Parte 3, ADRs (seção 10) e os status dos substituídos (0032, 0008, 0009, 0011).
- [ ] Parte 4, [`roadmap.md`](../roadmap.md) (inclui as pendências da seção 8 sem decisão),
      remover esta spec e a entrada no [`specs/README.md`](README.md); conferências do "Ao concluir
      uma tarefa" (build, lint, compile, `check-docs`, nomes removidos, "ainda não"/"hoje" nas
      referências, Swagger).
- [ ] Commit e descrição do PR desta branch (notas na seção 9).
- [ ] *Pode ficar para depois:* decidir se a tabela clara vira o padrão das listagens (seção 8).

## 7. Casos de teste

**Pré-requisitos:** banco zerado (colunas novas na `V1`) com o catálogo semeado; um ADMIN e um
PROVIDER; pacientes ativos A (titular), B (para beneficiário) e C (titular de outro contrato).
Contrato 1: paciente A, plano Evolução (12 sessões a 1x por semana = 12 semanas, máximo de 3
parcelas), dois itens por duração, 3 parcelas, com alguns atendimentos lançados. Contrato 2: só com o Pilates em Grupo
(plano de mensalidade). Pela API (Swagger ou testes de integração) e pela tela.

**A. Parcelas no aditivo (R2)**

| # | Caso | Esperado |
|---|---|---|
| A1 | Aditivo sem `installments` | 400 "O número de parcelas é obrigatório." |
| A2 | `installments` 0 ou 13 | 400 com a mensagem de mínimo 1 ou de máximo 12 |
| A3 | `installments` acima dos meses da nova vigência (contrato 1 sem prorrogar: 4) | 400 "O número de parcelas deve ser de 1 a 3 (meses do contrato)." |
| A4 | Prorrogar o contrato 1 para 26 semanas com 6 parcelas | 201; `installments` 6 (o máximo passou a 6) |
| A5 | Incluir um item (o total muda) com as mesmas 3 parcelas | 201; as 3 parcelas dividem o **novo** total, com os centavos na primeira |
| A6 | Só trocar a forma de pagamento, mesmas parcelas | 201; total e parcelas iguais aos da versão anterior |
| A7 | Aditivo numa versão já substituída | 409 "Só a versão ativa do contrato pode ser alterada." (como hoje) |
| A8 | Aditivo idêntico à versão atual (mesmos itens, beneficiário, pagamento e parcelas, sem `endDate`) | 400 "Nenhuma alteração no contrato." (R5); nenhuma versão nova |
| A9 | Aditivo que só muda uma coisa (só o `endDate`, ou só as parcelas, ou só a frequência de um item) | 201 em cada caso: conta como alteração |

**B. Prévia do aditivo (R1)**

| # | Caso | Esperado |
|---|---|---|
| B1 | Prévia com itens, sem forma de pagamento e sem beneficiário | 200 (o pedido da prévia não exige o que não muda as contas) |
| B2 | Prévia sem `installments` / com acima do máximo | 200; `installments` = o máximo da nova vigência |
| B3 | Prévia que inclui um item por duração | o fim vai para hoje + o prazo do item (se passar do fim atual); igual ao fim gravado ao salvar o mesmo aditivo |
| B4 | Prévia com `endDate` igual ou antes do fim atual | 400 "A nova data de término precisa ser posterior ao fim atual do contrato." |
| B5 | Prévia do contrato 1 mais de uma vez | nada gravado: a listagem e o `amendmentNumber` não mudam; a versão continua ativa |
| B6 | Prévia numa versão substituída | 409 "Só a versão ativa do contrato pode ser alterada." (a prévia segue a mesma regra do aditivo) |
| B7 | Prévia que prorroga um contrato de pacote | `groupPlanOptions` pela **nova** vigência |
| B8 | Prévia de contrato inexistente | 404 |

**C. Versões, número do aditivo e data (R3)**

| # | Caso | Esperado |
|---|---|---|
| C1 | Contrato novo | `amendmentNumber` 0, `createdAt` = hoje, `superseded` falso |
| C2 | Primeiro e segundo aditivo | `amendmentNumber` 1 e 2; `createdAt` de cada um = o dia em que foi feito |
| C3 | `GET /api/contracts` depois de dois aditivos | uma linha só para o contrato: a versão atual (Aditivo 2); as substituídas não vêm |
| C4 | Contrato inativado (sem aditivo) | continua na listagem, `active` falso e `superseded` falso |
| C5 | `GET /api/contracts/{id}` de uma versão substituída | `superseded` verdadeiro |
| C6 | `GET /api/contracts/{id}/versions` da versão atual | a cadeia toda, da mais nova para a mais antiga; `superseded` falso só na primeira |
| C7 | `GET /api/contracts/{id}/versions` de uma versão do meio | a cadeia **a partir dela para trás** (a tela sempre pede pela atual) |
| C8 | `/versions` de contrato inexistente | 404 |

**D. Rotas (R4 e autorização)**

| # | Caso | Esperado |
|---|---|---|
| D1 | `GET /api/contracts/{id}/group-plan-options` | 404 (a rota saiu) |
| D2 | PROVIDER na prévia do aditivo e no `/versions` | 403; ADMIN acessa (caso positivo) |

**E. Tela**

| # | Caso | Esperado |
|---|---|---|
| E1 | Lista | uma linha por contrato; selo do estado e, se houver aditivo, "Aditivo N"; a versão original só com o estado |
| E2 | Ações → detalhe | aba Contrato com a versão atual e o "Criado em"; rodapé Inativar, Fechar e Editar |
| E3 | Aba Histórico com vários aditivos | abre na versão mais nova, com a linha do tempo da original à atual; rodapé Voltar (para a aba Contrato) e Fechar |
| E4 | Abrir outra versão pela linha do tempo | a versão em azul na linha; título com número e selo "Substituída", dados e itens em molduras, com o "Criado em"; sem Editar nem Inativar |
| E4b | Abrir o Aditivo 1 e o Aditivo 2 da Elisa | no 1, marcados a forma de pagamento e as parcelas (com "antes: ..."); no 2, o fim e as parcelas; o Original sem marcação; trocar de aba não muda o tamanho do modal |
| E5 | Editar | passo 1 preenchido; titular, plano, início e fim atual travados; saldo de sessões em cada item |
| E6 | Passo 1 sem nenhuma mudança | "Prosseguir" inativo, com "Nenhuma alteração" |
| E7 | Incluir um item | o item marcado "Novo"; o rodapé mostra o fim e o total antes → depois |
| E8 | Revisar: só prorrogação | "Alterações" com só "Novo fim" |
| E9 | Revisar: só a forma de pagamento | só "Nova forma de pagamento" |
| E10 | Revisar: remover um item | "Removido: {técnica} (restavam N sessões)", mais as linhas do que mudou por causa dele |
| E11 | Revisar: mudar a frequência de um item | "Alterado: {técnica}, de {antes} para {depois}" |
| E12 | Voltar no passo 2 | volta ao passo 1 com tudo preenchido |
| E13 | Confirmar | tudo fecha; "Aditivo salvo."; a lista mostra o "Aditivo N" novo |
| E14 | Erro da API ao confirmar (ex: beneficiário em outro contrato vigente) | a mensagem da API aparece; Voltar leva ao formulário para corrigir |
| E15 | Contrato vencido (ativo, fim no passado) | pode ser editado; prorrogado, volta a "Vigente" |

**F. Item por duração em contrato de mensalidade (R6)**

| # | Caso | Esperado |
|---|---|---|
| F1 | Aditivo da Carla (Trimestral): incluir RPG 60 min 1x por semana | técnicas por duração na lista; valor do RPG pelo Evolução; fim = o maior entre o atual e hoje + 12 semanas; 201 ao confirmar |
| F2 | Cadastro com Trimestral + Pilates em Grupo + RPG quinzenal | fim em 24 semanas (passa das 13 do Trimestral); máximo de 6 parcelas |
| F3 | Contrato de mensalidade só com item por duração (cadastro, ou aditivo que remove o Grupo) | 400 "Contrato com plano de mensalidade precisa do item do Pilates em Grupo..." |
| F4 | Pela API, item por duração com a linha de outro pacote (ex: Essencial num Trimestral) | 400 "O preço do item precisa ser do plano Evolução (equivalente do Trimestral)." |

## 8. Pendências e pontos abertos

**Esboço do `billing`** (Fase 2; decidido na análise de 2026-11-18). Vai para o
[`roadmap.md`](../roadmap.md) na conclusão, no lugar dos itens "`Payment`" e "Saldo do
paciente" de "Não modelado ainda":
- **Nome do módulo: `billing`**, não `payment`: cobre o que é cobrado e o que é pago.
  `Payment` é uma entidade dentro dele. Precedente de módulo com nome do assunto: o `pricing`.
- **A dívida do paciente é o consumo** (atendimentos lançados × valor), comparado com os
  pagamentos. É o "controle de atendimento" que a clínica faz hoje à mão: o paciente que vai
  mais que o contratado (ex: aumentou a frequência por uma dor nas costas) termina devendo, e
  quem desmarca com antecedência termina com crédito. O sistema já grava o consumo
  (`Consultation.baseValue`, saldo negativo aceito pelo ADR 0018); falta o pago.
- **O contrato** trava o preço, define a expectativa de uso (sessões e frequência) e o plano de
  pagamento (as parcelas). As parcelas **não** são a dívida.
- **Sessão além do contratado** vale o **valor do item do contrato** (a linha travada), como o
  lançamento já faz.
- **Pilates em Grupo:** mensalidade, sem controle de sessões: a dívida é a parcela do mês, não o
  consumo.
- **Avisos:** quando o consumo passa do contratado ou do que foi pago, para a clínica avisar o
  paciente cedo (antes de o valor virar 2x ou 3x o acordado).
- **Crédito no fim do contrato:** abater em novas sessões, levar para outro contrato ou
  estornar (o destino que o item "Saldo do paciente" já prevê).
- **Módulos:** o `billing` fica **acima** do `consultation` no mapa e **lê** o `contract` e o
  `consultation`, sem juntá-los: são agregados com regras e ciclos de vida diferentes
  (versionado, imutável). Precedente: o saldo de sessões é calculado no `consultation`, lendo o
  `contract`.
- A frase do roadmap "pagamentos realizados × valor consumido" estava certa; o "Saldo do
  paciente" se junta a ela no mesmo módulo.

**Saldo de sessões na tela do aditivo** (decidido em 2026-11-25): o `session-balance` é do
`consultation` (conta os atendimentos; no backend, o `contract` não pode depender dele), e no front
o módulo `contract` não pode importar o `consultation` (mapa de módulos no
[`architecture.md`](../architecture.md)).
- **Decisão:** uma função no `contractApi.js` (`getSessionBalance`) chama o
  `GET /api/consultations/session-balance`, sem importar o módulo `consultation`. Buscado ao abrir
  o aditivo; sem invalidação entre módulos (na próxima abertura, busca de novo).
- **Exceção ao mapa, com limite:** a camada `api/` de um módulo pode **ler** um dado publicado por
  um módulo acima dele, pelo endpoint, sem importar o código dele; **nunca escreve nem repete a
  regra**. No DDD, é consumir o serviço que outro contexto publica: o saldo continua calculado no
  dono (`consultation`), e o front só mostra. Vai para o `architecture.md` na conclusão.
- *Descartadas:* compor a tela no `app/` (a solução "de livro", mas indireta só para um número por
  item); mudar o endpoint para `/api/contracts/{id}/session-balance` (o `contract` dependeria do
  `consultation`, ou a rota ficaria num controller que não é o dono).

**Tabela clara como padrão das listagens** (em aberto, 2026-11-25): a `DataTable variant="light"`
está só em Contratos. Se for aprovada, ela vira o padrão da `DataTable` (Pacientes, Prestadores,
Usuários, Preços), a variante sai e a spec [`layout-das-telas.md`](layout-das-telas.md) (tabela
padrão com cabeçalho no verde do menu e listra) é atualizada. Na conclusão, sem decisão, vai para o
[`roadmap.md`](../roadmap.md).

**Numeração do ADR dos testes de integração** (0035 → 0036 no texto, nos links e nos
comentários): corrigida aqui e também na `dev` (commits 660c42d e c28a9e2), com o mesmo conteúdo.
Não entra nas notas do PR.

## 9. Documentação ao concluir

- [`domain-model.md`](../domain-model.md), `Contract`: "Valor e parcelas" (o aditivo escolhe as
  parcelas, R2), "Versionamento (aditivos)" (número do aditivo gravado, versão substituída,
  listagem só das atuais e `/versions`, R3) e os endpoints de apoio (a prévia do aditivo, R1; sai o
  `group-plan-options`, R4). O `group-plan-options` também aparece nas linhas 610 e 786 do
  `domain-model.md` e no item do aditivo no `roadmap.md`; o ADR 0010 o cita, mas ADR aceito não é
  editado.
- [`architecture.md`](../architecture.md), seção "Frontend": a exceção ao mapa de módulos (a
  camada `api/` pode ler, pelo endpoint, um dado publicado por um módulo acima; nunca escreve nem
  repete a regra; precedente: o saldo de sessões no aditivo); e os padrões novos: o `Modal
  size="medium"` (detalhe com abas), as variantes dos compartilhados (`Badge` `suggestion` e
  `shape`, `DataTable` `light`/`onRowClick`/`selectedRowKey`, `IconButton` `soft`) e o azul de
  sugestão para "novo ou alterado" (aditivo e histórico).
- [`roadmap.md`](../roadmap.md): o aditivo concluído; o esboço do `billing` (seção 8) no lugar
  de "`Payment`" e "Saldo do paciente"; o que a feature deixou para depois (seção 2).
- [`domain-model.md`](../domain-model.md), R6: "Plano do item" e "Contrato de pacote precisa..."
  em `ContractItem` (linhas 643-652), "Contrato só com Grupo" em "Vigência e frequência" (757), o
  "Sem exceção" do plano em "Versionamento" (869-871) e os usos do de-para em `Plan` (414-421).
- ADRs da seção 10; o ADR 0032 com o status "aceito (em parte substituído por NNNN)"; os ADRs
  0008, 0009 e 0011 com "em parte substituído" pelo ADR da R6.
- Remover esta spec e a entrada em [`specs/README.md`](README.md).

**Para a descrição do PR** (além do que mudou, das decisões e do que foi testado):
- *PR do backend (#56, já na `dev`):* o aviso "zerem o banco local" (colunas `amendment_number` e
  `created_at` na `V1__initial_schema.sql`, migration editável pelo
  [ADR 0028](../adr/0028-migrations-editaveis-ate-versao-estavel.md)) e as mudanças na API (lista só
  da versão atual, `group-plan-options` removido, `installments` obrigatório e aditivo sem
  alteração recusado).
- *PR desta branch (`feature/contrato-aditivo-front`):*
  - **Mudança na regra (R6):** contrato de plano de mensalidade passa a aceitar itens por duração,
    pelo pacote equivalente; o item do Grupo continua obrigatório nele. Sem migration.
  - **Componentes compartilhados com variantes novas**, sem mudar o padrão das outras telas:
    `Badge` (`suggestion`, `shape="rounded"`), `Modal` (`size="medium"`), `DataTable` (`light`,
    `onRowClick`, `selectedRowKey`), `IconButton` (`soft`). O `PatientPicker` ("Remover", nome em
    negrito) vale também no cadastro de contrato.
  - **Em aberto:** a tabela clara como padrão de todas as listagens (seção 8).

## 10. ADRs a registrar

- **Parcelas escolhidas no aditivo** (R2), substituindo em parte o ADR 0032. *Descartadas:*
  copiar sempre; só na prorrogação; descontar as já pagas.
- **Prévia do aditivo num endpoint próprio, com pedido próprio** (R1). *Descartadas:* a prévia
  da criação; o cálculo no front; o pedido do aditivo na prévia.
- **Lista pela versão atual, com o número do aditivo gravado** (R3). *Descartadas:* todas as
  versões na lista; contar a cadeia a cada leitura; consulta recursiva no SQL.
- **Item por duração em contrato de mensalidade, pelo pacote equivalente** (R6), substituindo em
  parte os ADRs 0008, 0009 e 0011. *Descartadas:* escolher o pacote numa lista; só no aditivo;
  contrato de mensalidade sem o item do Grupo.
