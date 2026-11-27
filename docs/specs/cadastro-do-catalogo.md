# Spec — Cadastro do catálogo (técnicas, grupos e planos)

A clínica cadastra, renomeia, inativa e reativa técnicas, grupos de preço e planos pela tela, e
a tabela de Preços passa a mostrar quais técnicas cada grupo cobre.

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

- **Técnicas na tabela de Preços: implementadas** (2026-11-24, só frontend, na branch
  `feature/implementando-filtro-tabela-preco`): os filtros de grupo e de técnica, a coluna
  "Técnica" e a rolagem só na tabela (seção 3). Conferido por `npm run build` e
  `npm run lint`; **teste pela tela não feito**.
- **Falta todo o resto:** o backend, as telas de cadastro e o grupo inativo na tela de Preços.
  Faltam também as decisões marcadas como *em aberto* na seção 3 e a prévia das telas de
  cadastro (seção 5).
- **O que já existe** no `pricing`:
  - criar técnica, grupo e plano (`POST`);
  - inativar e reativar técnica (`DELETE` e `PATCH .../activate`);
  - de-para dos planos (`PUT /api/plans/{id}/equivalent`).

  Pela tela, só a de Preços (consulta e reajuste). Técnica, grupo e plano só se cadastram pelo
  Swagger ou pelo seed (V4).
- **Branch:** `feature/cadastro-do-catalogo`, a partir da `dev` **depois do merge da
  `merge/estruturacao-layout`**: a tela de Preços que esta feature altera está nela. A parte do
  backend não depende disso e pode começar antes. PR para a `dev`, com merge commit.

## 2. Objetivo e escopo

Hoje a clínica só mexe em preço pelo sistema. Uma técnica nova, um grupo novo ou um nome
corrigido dependem de um desenvolvedor. A tabela de Preços mostra grupo, plano, eixo e valor,
mas não diz quais técnicas cada grupo cobre. Quem vê "Individual Especializado" não sabe que
ali estão Quiropraxia e Reabilitação Vestibular.

A cadeia do preço não muda (ver `PricingGroup` no [`domain-model.md`](../domain-model.md)):
técnica → grupo → linha de preço (plano + duração ou frequência) → valor. A técnica não se
liga ao plano; o plano se liga ao grupo pela linha de preço.

Depende só do `pricing`. `contract` e `consultation` não mudam: já recusam técnica e plano
inativos em cadastro novo.

**Entra:**

- **Backend:**
  - renomear técnica, grupo e plano;
  - inativar e reativar grupo e plano;
  - coluna `active` em `pricing_group`;
  - grupo inativo não recebe técnica nova nem preço novo.
- **Frontend:**
  - as técnicas de cada grupo na tabela de Preços;
  - telas de cadastro de técnica, grupo e plano, com o de-para do plano.

**Fica para depois (ou não vai existir):**

- **Trocar o grupo de uma técnica ou o modelo de preço de um grupo:** não vai existir
  ([ADR 0004](../adr/0004-grupo-de-preco-e-duas-tabelas.md)). Reclassificar é criar outra
  técnica ou outro grupo e inativar o antigo.
- **Trocar o tipo, as sessões ou o prazo de um plano:** fixos após a criação (`PlanEntity`).
  Mudar é criar outro plano.
- **Reajuste agendado** (`validFrom` futuro): continua nas melhorias futuras do roadmap.

## 3. Regras

**Renomear (técnica, grupo e plano)**

- Só o nome muda. Grupo da técnica, modelo do grupo e tipo e quantidades do plano continuam
  fixos.
- Mensagens:
  - nome em branco → 400 "O nome da técnica é obrigatório." (ou "do grupo de preço", "do
    plano"), as mesmas da criação;
  - nome de outro registro → 409 "Já existe uma técnica cadastrada com o nome: X." (idem para
    grupo e plano).
- Manter o próprio nome (inclusive mudando só maiúsculas ou acentos) não é conflito. A
  collation do MySQL compara sem maiúsculas e sem acento; a checagem exclui o próprio `id`.
  Precedentes: `PatientService.update` com `existsByCpfAndIdNot` (`PatientService.java:83`) e
  `UserService` com `existsByEmailAndIdNot` (`UserService.java:174`).
- Registro inativo também pode ser renomeado: é correção de cadastro.
- *Em aberto:* **o nome novo aparece nos contratos e atendimentos antigos**, que referenciam o
  `id`. Recomendação: aceitar. Renomear serve para corrigir grafia ou padronizar nome. Uma
  técnica que mudou de fato é uma técnica nova.

**Inativar e reativar grupo**

- Mesmo padrão da técnica: `deactivate()` e `activate()` na entidade, com `requireActive()` e
  `requireInactive()` (409 "O grupo de preço já está inativo." / "... já está ativo.").
  Precedente: `TechniqueEntity.java:52-75`.
- *Em aberto:* **grupo com técnicas ativas pode ser inativado?** Recomendação: não. 409 "O
  grupo tem técnicas ativas (RPG, Massoterapia). Inative-as antes de inativar o grupo." Assim,
  nenhuma técnica ativa aponta para um grupo inativo, e contrato e lançamento não precisam
  conferir o grupo: eles já recusam técnica inativa (`ContractItemEntity.java:139`,
  `ConsultationEntity.java:213`).
- Grupo inativo:
  - não recebe técnica nova: 400 "O grupo de preço está inativo.";
  - não recebe preço novo nem reajuste: 400, mesma mensagem, no construtor da linha de preço,
    como o plano inativo (`SessionDurationPriceEntity.java:97`);
  - contratos que já travaram linha dele continuam valendo até o fim (preço travado).
- Reativar o grupo não reativa as técnicas: cada uma volta pela própria reativação.

**Inativar e reativar plano**

- Mesmo padrão: `deactivate()` e `activate()` com 409 quando o estado já é esse.
- Plano inativo não entra em contrato novo (`ContractEntity.java:340`) nem em linha de preço
  nova (`SessionDurationPriceEntity.java:97`): as duas regras já existem.
- *Em aberto:* **plano de contrato vigente pode ser inativado?** Recomendação: sim. Pela regra
  atual, o aditivo copia o plano e aceita o que é mantido mesmo inativo. Inativar só impede
  contratos novos.
- *Em aberto:* **o Avulso (`SINGLE_SESSION`) pode ser inativado?** Ele é o preço de quem é
  atendido sem contrato (caso 3 da "Resolução de preço" em `Consultation`). Sem Avulso ativo,
  todo lançamento sem contrato dá 400. E não dá para criar outro Avulso enquanto um está ativo
  (409). Recomendação: não. 409 "O Avulso é o preço de quem é atendido sem contrato e não pode
  ser inativado." O valor dele muda por reajuste, e o nome por renomear.
- *Em aberto:* **plano que é equivalente (de-para) de outro.** O `planFor` não confere se o
  equivalente está ativo, então o caso 2 do lançamento continua usando o plano inativo.
  Recomendação: manter assim. O de-para é o que foi combinado; para trocar, define-se outro
  equivalente (`PUT /api/plans/{id}/equivalent`, que já exige equivalente ativo).
- Reativar um `SINGLE_SESSION` com outro ativo → 409, a mesma regra da criação. Só se aplica
  se a regra anterior ficar diferente da recomendação.

**Quem cadastra**

- *Em aberto:* recomendação: só o `ADMIN`, como os `POST` de hoje. O `SecurityConfig` já tem o
  `ADMIN` como padrão, e só o `GET /api/techniques` é aberto ao `PROVIDER`, para o lançamento.

**Listagens**

- Técnica: sem mudança (`GET /api/techniques`, as ativas; `GET /api/techniques/all`, todas com
  `active`).
- Plano: sem mudança (`GET /api/plans` já devolve todos, com `active`).
- Grupo: `GET /api/pricing-groups` passa a trazer `active` e continua devolvendo todos.
  - *Por quê:* a tela de Preços precisa do grupo inativo para mostrar o histórico dele.
  - Segue o plano, não a técnica, para não criar um segundo endpoint de listagem.
  - A tela de Preços mostra o grupo inativo com o selo "Inativo" e sem "Novo Preço /
    Reajustar".

**Técnicas na tabela de Preços**

- O dado vem de `GET /api/techniques`, que já traz o `pricingGroupId` de cada técnica. O
  backend não muda.
- Só as técnicas ativas: as inativas não entram em contrato nem em lançamento novo.
- **Formato decidido (2026-11-24, implementado):**
  - **Coluna "Técnica"**, com uma linha por técnica ativa e versão do plano. Cada valor se
    repete em todas as técnicas do grupo.
    - A decisão foi do usuário. Ela substitui a que tinha descartado a coluna, por mostrar a
      técnica direto na linha.
    - Risco aceito: a tela pode dar a entender que dá para reajustar uma técnica só. O
      detalhe e a alteração continuam sendo do grupo (mostram o nome dele), e o reajuste vale
      para todas as técnicas dele.
  - **Filtros "Grupo de atendimento" e "Técnica"**, lado a lado, os dois começando em "Todos".
    - Com o grupo em "Todos", a tabela mostra o catálogo inteiro, com a coluna "Grupo" e as
      colunas dos dois modelos de preço (duração e frequência). A célula fica "—" quando o
      grupo da linha não tem aquele eixo.
    - A lista de técnicas segue o grupo escolhido. Com o grupo em "Todos", ela mostra todas,
      separadas pelo grupo.
    - Trocar o grupo volta a técnica para "Todas". Escolher uma técnica leva o filtro de
      grupo para o grupo dela (com isso, a coluna "Grupo" sai). Voltar a técnica para "Todas"
      mantém o grupo.
    - Sem aviso de "estes valores valem também para...": o usuário dispensou.
  - **Grupo sem técnica ativa** continua na tabela, com "—" na coluna "Técnica", para o preço
    dele não sumir.
  - **Rolagem:** só a tabela rola, com o cabeçalho das colunas parado. O título e os filtros
    ficam fixos.
  - Comparadas na prévia de 2026-11-24: uma tabela só com a coluna "Grupo" ou uma seção por
    grupo, antes de o usuário pedir a coluna "Técnica". As prévias A, B e C de 2026-11-18
    ficaram de fora.
- O "Todos" busca o histórico de cada grupo, uma requisição por grupo
  (`usePricingGroupPriceHistories`, que divide o cache com o hook de um grupo). São poucos
  grupos. Se o catálogo crescer, vale criar um endpoint com o histórico de todos.
- *Em aberto:* **plano sem preço no grupo.** Hoje o plano sem linha no grupo some da tabela.
  Ex: o Evolução Familiar no Individual Especializado e no Em Dupla; vender esse plano com
  Quiropraxia dá 400 no contrato. Todo plano de pacote (`PACKAGE`) deve ter preço em todos os
  grupos por duração?
  - **Se sim:** a tela aponta a falta ("sem preço", com atalho para "Novo Preço / Reajustar").
    A lista dos planos sem linha vem do backend (regra de quais planos cada grupo aceita).
  - **Se não:** fica como está.

## 4. Desenho do backend

**Módulo `pricing`**

| Arquivo | Mudança |
|---|---|
| `PricingGroupEntity` | campo `active`; `rename(name)`, `deactivate()`, `activate()`, com `validateName`, `requireActive`, `requireInactive` |
| `TechniqueEntity` | `rename(name)` (reusa a validação do nome); o construtor recusa grupo inativo (`requireActivePricingGroup`) |
| `PlanEntity` | `rename(name)`, `deactivate()` (com `requireNotSingleSession`, se a regra do Avulso for aprovada), `activate()` |
| `SessionDurationPriceEntity`, `SessionFrequencyPriceEntity` | o construtor recusa grupo inativo, ao lado da checagem do plano inativo |
| `TechniqueRepository` | `existsByNameAndIdNot`; `findByPricingGroupIdAndActiveTrue` (nomes das técnicas ativas, para a mensagem do 409) |
| `PricingGroupRepository`, `PlanRepository` | `existsByNameAndIdNot` |
| `dto/RenameRequestDTO` (novo) | `name` (`@NotBlank`). Um só para os três: os três renomeiam igual |
| `PricingGroupResponseDTO` | `active` |
| `TechniqueService`, `PricingGroupService`, `PlanService` | `rename`; `deactivate` e `activate` no grupo e no plano. O grupo confere as técnicas ativas antes de inativar (depende do banco, fica no service) |
| `TechniqueController` | `PUT /api/techniques/{id}` (renomear) |
| `PricingGroupController` | `PUT /api/pricing-groups/{id}`, `DELETE /api/pricing-groups/{id}` (inativa), `PATCH /api/pricing-groups/{id}/activate` |
| `PlanController` | `PUT /api/plans/{id}`, `DELETE /api/plans/{id}` (inativa), `PATCH /api/plans/{id}/activate` |

- Os `@Operation` dos endpoints novos descrevem as regras da seção 3 (409, 400, o que fica
  fixo).
- `SecurityConfig`: nada a mudar se ficar só o `ADMIN` (é o padrão).

**Banco**

- Coluna `active BIT(1) NOT NULL` em `pricing_group`, com os grupos do seed ativos.
- **Uma migration nova** (a versão estável, [ADR 0052](../adr/0052-versao-estavel-do-banco.md):
  as migrations aplicadas não se editam mais): `V5__pricing_group_active.sql` (`ADD COLUMN
  active BIT(1) NOT NULL DEFAULT 1`), com os grupos que já existem ativos pelo `DEFAULT`.

## 5. Desenho do frontend

Antes do código, as prévias (CLAUDE.md): a tabela de Preços (feita e implementada, ver a
seção 3) e as telas de cadastro (a fazer).

| Arquivo | Mudança |
|---|---|
| `domain/pricing/api/pricingApi.js`, `usePricing.js` | renomear (os três), inativar e reativar (grupo e plano), criar técnica, grupo e plano; `useAllTechniques` (`GET /api/techniques/all`) |
| `domain/pricing/pages/PricingCatalogPage.jsx` | **feito:** filtros de grupo e de técnica com "Todos" e a rolagem só na tabela. **Falta:** o selo "Inativo" e tirar o "Novo Preço / Reajustar" no grupo inativo |
| `domain/pricing/components/` | **feito:** colunas "Grupo" e "Técnica" no `PriceTable`. **Falta:** os modais de cadastro e de renomear |
| `domain/pricing/utils/techniquePriceRows.js` | **feito:** as linhas por técnica e os filtros |
| `domain/pricing/pages/` (nova) | o cadastro do catálogo. *A definir na prévia:* uma tela com abas Técnicas, Grupos e Planos, ou três telas. No padrão "Telas de listagem" do [`architecture.md`](../architecture.md) (filtro na page, modal pelo `id`, `ListMessage`, um hook por endpoint para inativar e reativar) |
| `app/router.jsx`, `app/layout/navigationItems.js` | a rota nova, só `ADMIN` |

- O front não repete regra: técnicas ativas do grupo, conflito de nome e Avulso protegido vêm
  da API (409 e 400 pelo `showError` e pelos erros por campo).
- O modal de técnica escolhe o grupo e avisa que o grupo não muda depois.

## 6. Tarefas

**Decisões**

- [ ] Regras *em aberto* da seção 3: nome novo em registros antigos, grupo com técnicas
  ativas, plano de contrato vigente, Avulso, equivalente inativo, quem cadastra, plano sem
  preço no grupo.
- [ ] Migration: V5 ou edição da V1 e V4.
- [x] Prévia da tabela de Preços (2026-11-24: coluna "Técnica" e os filtros, ver a seção 3).
- [ ] Prévias das telas de cadastro; escolha.

**Backend**

- [ ] Coluna `active` no grupo (migration e entidade).
- [ ] Renomear: entidades, repositórios, `RenameRequestDTO`, services, controllers.
- [ ] Inativar e reativar grupo e plano.
- [ ] Grupo inativo recusado na técnica nova e na linha de preço nova.
- [ ] `@Operation` dos endpoints novos.
- [ ] Testes de unidade das entidades: `rename`, `deactivate` e `activate` (grupo e plano), o
  Avulso protegido.
- [ ] `./mvnw -q compile` e `./mvnw test`.

**Frontend**

- [x] Técnicas na tabela de Preços: coluna, filtros e rolagem só na tabela.
- [ ] Teste pela tela das técnicas na tabela de Preços.
- [ ] Grupo inativo na tela de Preços.
- [ ] Telas de cadastro (versão escolhida).
- [ ] `npm run build` e `npm run lint`.

**Conclusão**

- [ ] Casos de teste da seção 7, pela API e pela tela.
- [ ] Seção 9, `node scripts/check-docs.mjs`, spec removida.

## 7. Casos de teste

Pré-requisitos: banco com o seed da V4, login de `ADMIN` e de `PROVIDER`.

**Renomear (API)**

| # | Caso | Esperado |
|---|---|---|
| 1 | Renomear RPG para "RPG Global" | 200; `GET /api/techniques` traz o nome novo |
| 2 | Renomear RPG para "Massoterapia" | 409 |
| 3 | Renomear RPG para "rpg" (só maiúsculas) | 200 (é o próprio registro) |
| 4 | Renomear com nome em branco | 400 |
| 5 | Renomear grupo e plano (casos 1 a 4 em cada) | mesmo comportamento |
| 6 | Renomear com o login de `PROVIDER` | 403 |

**Grupo (API)**

| # | Caso | Esperado |
|---|---|---|
| 7 | Inativar o Em Dupla com o Pilates em Dupla ativo | 409, com o nome da técnica (se a regra for aprovada) |
| 8 | Inativar o Pilates em Dupla e depois o Em Dupla | 204; `GET /api/pricing-groups` traz `active: false` |
| 9 | Inativar o Em Dupla de novo | 409 |
| 10 | Criar técnica no Em Dupla inativo | 400 |
| 11 | Novo preço no Em Dupla inativo | 400 |
| 12 | Histórico de preços do Em Dupla inativo | 200, com as linhas |
| 13 | Reativar o Em Dupla | 204; o Pilates em Dupla continua inativo |

**Plano (API)**

| # | Caso | Esperado |
|---|---|---|
| 14 | Inativar o Essencial | 204; contrato novo com o Essencial → 400 "O plano está inativo." |
| 15 | Aditivo de um contrato Essencial vigente, depois de inativar o plano | aceito (o plano é mantido) |
| 16 | Inativar o Avulso | 409 (se a regra for aprovada) |
| 17 | Reativar o Essencial; reativar de novo | 204; 409 |

**Tela**

| # | Caso | Esperado |
|---|---|---|
| 18 | Tabela de Preços em cada grupo | as técnicas ativas do grupo aparecem |
| 19 | Grupo inativo no seletor | selo "Inativo", histórico visível, sem "Novo Preço / Reajustar" |
| 20 | Cadastrar, renomear, inativar e reativar técnica, grupo e plano | listas atualizadas; erros da API na tela |
| 21 | Login de `PROVIDER` | a rota e o menu do cadastro não aparecem |

## 8. Pendências e pontos abertos

- **Evolução Familiar sem preço** no Individual Especializado e no Em Dupla
  ([`development.md`](../development.md), catálogo). É dado da clínica, não código: depois
  desta feature, cadastra-se pela tela de Preços.
- A tela de Preços repete no front a regra de quais planos cada grupo aceita (`isPlanAllowed`,
  pendência do roadmap). Se a regra do plano sem preço trouxer essa lista do backend, a
  pendência se resolve junto.

## 9. Documentação ao concluir

- **`domain-model.md`:**
  - em `PricingGroup`, o `active` (também no diagrama), inativar e reativar e o que o grupo
    inativo recusa;
  - em `Technique` e `Plan`, renomear e (no plano) inativar e reativar, com as regras
    aprovadas;
  - a lista do "Soft delete" e da "Reativação" no topo ganha `PricingGroup` e `Plan`.
- **`architecture.md`:** o `RenameRequestDTO` na tabela do "Padrão de construção", se virar
  padrão.
- **`development.md`:** a migration nova na tabela de migrations.
- **`roadmap.md`:**
  - tirar o "`PUT` de grupo, técnica e plano (renomear)" das melhorias futuras;
  - tirar o "cadastro de grupo, técnica e plano pela tela" das pendências do Frontend;
  - marcar o "Catálogo de `pricing` consultável" no checklist;
  - atualizar o `pricing` no "Estado atual".
- **`specs/README.md`:** tirar esta spec de "Em andamento".

## 10. ADRs a registrar

- **Grupo e plano inativáveis, com as condições** (grupo sem técnicas ativas, Avulso
  protegido), se as regras forem aprovadas: as alternativas descartadas (inativar em cascata
  as técnicas do grupo; deixar inativar o Avulso) e o motivo.
- O ADR 0004 não muda: renomear não troca o grupo da técnica nem o modelo do grupo.
