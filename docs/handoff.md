# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code. Leia também
> `docs/architecture.md` e `docs/domain-model.md` antes de começar.
> Atualizado em 2026-10-15.
>
> ⚠️ Está **versionado** na branch `feature/consultation` (entrou via `git add -f`).
> Antes de abrir o PR: `git rm --cached docs/handoff.md` + commit (o
> `.git/info/exclude` volta a ignorá-lo).

## Como eu gosto de trabalhar

- **Explique antes de alterar.** Para cada mudança, diga o que muda e por quê
  (em 1–3 frases) antes de aplicar. Passos pequenos; nada de várias alterações em
  lote sem explicação.
- **Quando eu mandar algo, pare e leia.** Mensagem minha ou motivo de recusa de
  uma edição pode ser complemento ou dúvida — responda antes de seguir. Nunca
  dispare várias edições depois de uma recusa. Dúvida ≠ pedido de mudança.
- **Ao apontar um problema, mostre o código.** Explicação + trecho exato
  (arquivo:linha) + um exemplo concreto do que dá errado.
- **Análises longas em partes.** Apresente uma parte, espere meu retorno e só
  então siga para a próxima.
- **Correção ≠ melhoria ≠ feature.** Numa rodada de correções, faça só a mudança
  mínima. Melhorias você lista para eu decidir — não aplica junto.
- **Decisões de negócio são minhas.** Quando houver dúvida de regra, pergunte; não
  presuma. Quando houver escolha técnica, dê uma recomendação, não só as opções.
- **Consulte o projeto antes de sugerir — e antes de escrever.** Toda recomendação
  técnica parte de como os módulos existentes já resolvem o mesmo caso (citar o
  precedente em arquivo:linha) e dos princípios de monolito modular e DDD do
  `architecture.md`. Se for caso novo, sem precedente, diga isso.
- **Seguir o princípio 7 do `architecture.md`** (DTO = formato, entidade/VO =
  regras dela, service = regras que dependem do banco + orquestração).
- **Sem `if` direto no construtor/fábrica/método de estado.** Validação em método
  privado com nome que diz o que valida (ex: `validateNameAndSessionCount`,
  `requireActive()`).
- **Mensagens ao usuário em português** (exceptions, DTO, handler). Só nomes de
  código em inglês.
- **Um service por agregado.**
- **Dados de teste passam pelas classes (API), nunca por SQL direto.**
- **Handoff só é atualizado quando eu pedir** (uso para trocar de computador).
- Conversa e documentação em português; código em inglês.
- Compile (`./mvnw -q compile`) depois das mudanças e diga claramente o que foi
  ou não testado.
- **Onde cada coisa roda:** desenvolvimento e execução no meu computador (VS Code +
  Docker/MySQL). A sessão no Claude Code web é só para conversar/planejar.

## Padrão de construção (todos os módulos já estão nele)

| Camada | Padrão | Precedente |
|---|---|---|
| Controller | construtor explícito, `@Tag` + `@Operation` + `@ApiResponses` em português, POST → 201, DELETE (soft) → 204 (409 se já inativo) | `PatientController`, `TechniqueController` |
| DTO | `record`; request com `message` em português; response com `fromEntity` estático; DTO de alteração separado quando um campo não pode mudar | `PlanRequestDTO`, `ProviderUpdateRequestDTO` |
| Service | um por agregado, construtor explícito, `@Transactional` (`readOnly` em leitura), `getXOrThrow` (404), `existsBy...` (409), sem `save()` após alterar (dirty checking), soft delete = `deactivate` | `PatientService`, `ProviderService` |
| Entidade | construtor de negócio + `private static validate...`, `active = true`, `deactivate()` com `requireActive()` (409) | `PatientEntity`, `PlanEntity` |
| VO de um valor | `record` + `AttributeConverter` `autoApply`; DTO continua `BigDecimal`, service faz `X.of(...)`; rejeita mais de 2 casas, nunca arredonda | `Money` (pricing), `Percentage` (provider) |
| Unicidade | `unique = true` na coluna + `existsBy...` no service (409) | `PatientRepository.existsByCpf` |
| Erros | `GlobalExceptionHandler`: ProblemDetail; DTO → `errors` (campo→mensagem); exceptions → `detail`; JSON ilegível e parâmetro de URL inválido → 400 em português | — |

## Onde paramos (2026-10-15)

- **Branch:** `feature/consultation` (último commit `9754d47 feat: handoff`),
  working tree limpo. **Nenhum código escrito** nesta branch.
- Estamos **definindo as regras de negócio** do `Consultation` e, com isso, a
  **reestruturação do `Contract`** (contrato com itens). Tudo abaixo de "Decidido
  na conversa" **ainda NÃO está no `domain-model.md`** — documentar antes de
  qualquer código.
- Testes: serão feitos pela interface gráfica, depois do `Consultation`. Nada do
  backend foi executado desde a padronização.

### Já documentado no `domain-model.md` (commit `0696dd1`)

1. **Status: só 3** — `ATTENDED`, `NO_SHOW`, `CANCELED_LATE` (desmarcou com menos
   de 6h). Todo atendimento lançado é cobrado e gera repasse cheio. O resto não é
   lançado. Sem `countsTowardsBilling()`.
2. **Comissão pela origem do paciente:** `Patient.referringProviderId` (nullable,
   `null` = clínica); `Provider` com `commissionPercentage` (padrão 40%) e
   `referralCommissionPercentage` (padrão 45%), referral nunca menor. Vale o
   referral se o prestador que atendeu trouxe o **titular do contrato**.

### Decidido na conversa (falta documentar)

**Estrutura nova do contrato — opção "contrato com itens":**

- **`Contract` (raiz do agregado):** titular, beneficiário (nullable), vigência
  **única**, forma de pagamento **única**, **plano único** (ex: Evolução),
  `active`, `previousContract` (aditivo = nova versão do contrato inteiro). **Um
  contrato vigente por titular** (regra atual mantida).
- **`ContractItem` (um por grupo de preço, `unique (contrato, grupo)`):** linha de
  preço travada (o **exclusive arc sai do contrato e vai para o item**), técnica
  escolhida **só como informação** (aparece no contrato impresso, ex: "Liberação
  Miofascial e Quiropraxia"; precisa ser do grupo da linha; nova precisa estar
  ativa), frequência. O plano do item é o plano do contrato no grupo dele (mesmo
  plano nos grupos por duração; equivalente do de-para no Pilates em Grupo).
- **Saldo por item:** cada item de grupo por duração tem as `sessionCount` do
  plano (contrato Evolução com RPG + Quiro = 12 de RPG **e** 12 de Quiro), somando
  a cadeia de versões. Item do Pilates em Grupo não tem saldo de sessões.

**De-para de planos (duração ⇄ Pilates em Grupo), independente da frequência:**
Essencial ⇄ Mensal · Evolução ⇄ Trimestral · **Evolução Familiar → Trimestral** ·
Transformação ⇄ Semestral · Transformação Familiar ⇄ Semestral Familiar ·
Vitalidade ⇄ Anual · Vitalidade Familiar ⇄ Anual Familiar. Avulso **não tem
equivalente no Grupo** (Grupo exige contrato). Vou cadastrar todos os planos em
todos os grupos por duração (ex: Evolução Familiar no Especializado e na Dupla).

**Lançamento do atendimento (`Consultation`):**

- O prestador lança o **paciente que realmente compareceu**.
- **Contratos elegíveis:** vigentes em que o paciente é **titular ou
  beneficiário** e que têm item do grupo da técnica atendida. Nenhum → caso 2/3;
  um → automático; **mais de um → modal** para o prestador escolher. Estrutura
  proposta: `GET /api/consultations/eligible-contracts?patientId&techniqueId`
  (lista com titular, item e saldo) + o lançamento recebe `contractId`; backend
  revalida (precisa ser elegível; se existir elegível, `contractId` é obrigatório).
- **Caso 1 — grupo contratado:** debita o saldo do item. Grupo por duração: mesma
  duração da linha travada → **preço travado**; outra duração → **linha atual do
  mesmo plano**; sem linha → 400. Grupo por frequência → linha travada.
- **Caso 2 — paciente tem contrato, mas não do grupo atendido:** **de-para** a
  partir do plano do contrato → linha **atual** do grupo atendido (pela duração do
  atendimento; no Pilates em Grupo, **sempre 1x/semana** — aula experimental; se
  quiser continuar, o admin inclui o Grupo no contrato). **Não debita saldo**;
  valor vai para o saldo financeiro do paciente (módulo futuro). Sem linha → 400.
- **Caso 3 — paciente sem contrato:** grupo por duração → **Avulso** vigente na
  duração do atendimento; Pilates em Grupo → **400** (exige contrato).
- Saldo e comissão sempre do **titular do contrato escolhido** (sem contrato: o
  próprio paciente). `Consultation` referencia o **`Contract` (raiz)**, não o
  item — item = contrato + grupo da linha usada; `contractId` nulo = avulso.
- Sem modal de escolha de preço (o de-para substitui).

**Preço:** o contrato congela o preço **do grupo contratado** (qualquer técnica do
grupo, mesma duração) até o fim; o resto paga o preço atual. Mantido o modelo de
linhas individuais com `validFrom`/`validTo` (ideia de "tabela de preço anual"
foi **descartada**). O grupo do contrato não é salvo: vem da linha travada /
técnica (derivável).

**Frequência × vigência (em definição):**

- Novo enum (ex: `SessionFrequency`) para grupos por duração: `MONTHLY` (1x/mês),
  `BIWEEKLY` (quinzenal), `WEEKLY`, `TWICE_WEEKLY`, `THREE_TIMES_WEEKLY`
  (substitui o `weeklyFrequency` inteiro, que não representa quinzenal/mensal).
  No Pilates em Grupo, a frequência continua sendo o eixo do preço (1x/2x/3x).
- **Limite de vigência do item = sessões × intervalo da frequência, teto de 12
  meses** (cálculo como método do enum — princípio 6). Reproduz a tabela da
  clínica: Essencial 1/2/4 meses (semanal/quinzenal/mensal); Evolução 3/6/12;
  Transformação 6/12 (mensal ❌); Vitalidade 12 (quinzenal/mensal ❌). Frequências
  mais rápidas reduzem proporcionalmente (Vitalidade 2x/semana = 6 meses).
- **Vigência do contrato = a maior entre as dos itens** (itens mais rápidos só
  esgotam o saldo antes). Item do Pilates em Grupo fica fora do cálculo.
  → o `endDate` passaria a ser **calculado pelo sistema** (usuário informa início,
  plano e frequência de cada item).

### Perguntas em aberto (minha resposta pendente)

1. **`endDate` totalmente calculado?** E a **prorrogação** por aditivo (paciente
   doente, por exemplo) continua existindo, mesmo passando do calculado, ou o que
   sobra no fim vai sempre para o saldo do paciente?
2. **Contrato só com Pilates em Grupo:** vigência vem do prazo do plano do Grupo
   (Mensal 1 mês, Trimestral 3, Semestral 6, Anual 12)?
3. **Frequências oferecidas:** até 3x/semana, ou existe 4x+?
4. **Arredondamento em dias** nos ritmos rápidos (ex: 26 sessões a 3x/semana =
   8,67 semanas): arredondar para cima (61 dias)?

### Ainda a definir depois

- **Repasse (Parte 3):** cálculo, **modo de arredondamento** (ex: 35% de
  R$ 58,91 = R$ 20,6185), renomear `repasseValue` → `payoutValue` (sugestão),
  `Percentage.applyTo(Money)`.
- **Ciclo de vida do lançamento (Parte 6):** atendimento lançado pode ser
  alterado/excluído?
- **Quem lança (Parte 7)** (adiado): `providerId` explícito; `launchedByUserId`.
- Precisão: `session_value` `DECIMAL(10,2)` × `base_value` `DECIMAL(12,2)`.

### Módulo futuro: saldo do paciente (Parte 5 — decidido, fora do escopo agora)

- Lista os contratos ativos do paciente e o saldo.
- **Finalizar contrato** pergunta se o valor restante já foi pago ou vira **saldo
  do paciente**; o contrato fica inativo e o valor passa a existir só no saldo.
- Cada lançamento de saldo tem FK do contrato de origem. Saldo positivo pode ser
  reaproveitado na renovação ou congelado; débito (negativo) idem, e o admin marca
  como acertado quando quitado.
- Contrato ganhará um `finish(...)` (diferente do `deactivate`) quando o módulo
  existir.

### Implementação já planejada (aguardando aprovação; fazer depois da reestruturação)

- `ConsultationStatus` com os 3 valores.
- `Provider`: `referralCommissionPercentage`, constantes 40/45, invariante,
  `Percentage.isLessThan`; recomendação: no `PUT` os dois percentuais são
  **obrigatórios**.
- `Patient`: `referringProvider` (LAZY, nullable), `updateReferringProvider`,
  resposta com id e nome.
- Impacto da reestruturação: `contract` **refeito** (cabeçalho + itens, DTOs com
  lista de itens, aditivo por item, exclusive arc no item, enum de frequência,
  vigência calculada); `consultation` do zero; `pricing` ganha o de-para
  (sugestão: `Plan.equivalentPlanId` se for 1:1); docs (`domain-model.md`,
  `architecture.md` princípio 4).

## Ambiente — avisos

- **Arquivos voltando sozinhos para versões antigas** (`TechniqueController`,
  `domain-model.md` — já restaurados). Suspeitas: OneDrive na Área de Trabalho ou
  aba antiga do VS Code. **Conferir o diff antes de commitar.**
- **Erros falsos do Lombok na IDE** ("Can't initialize javac processor...",
  `getX()` não encontrado). O Maven compila normalmente.
- **Maven sem acesso ao Central** (erro de certificado PKIX): só o cache local
  funciona (plugins novos não baixam).
- Spring Boot **4.1.1**, Java 25, **Jackson 3** (`tools.jackson`).
- Docker Desktop estava fechado na última checagem.

## Dados da clínica (referência para cadastros/telas)

**Grupos:** Individual Padrão, Individual Especializado, Em Dupla
(`DURATION_BASED`); Em Grupo (`FREQUENCY_BASED`).

**Técnicas:** Liberação Miofascial, RPG, Massoterapia, Pilates Individual →
Individual Padrão · Quiropraxia, Reabilitação Vestibular → Individual
Especializado · Pilates em Dupla → Em Dupla · Pilates em Grupo → Em Grupo.

**Planos (`sessionCount`):** Avulso 1, Essencial 4, Evolução 12, Evolução
Familiar 12, Transformação 26, Transformação Familiar 26, Vitalidade 52,
Vitalidade Familiar 52 · Mensal, Trimestral, Semestral, Semestral Familiar,
Anual, Anual Familiar → `null`.

**Valores por sessão (60 min / 30 min):**

| Plano | Individual Padrão | Individual Especializado | Em Dupla |
|---|---|---|---|
| Avulso | 220,00 / 132,00 | 275,00 / 176,00 | 265,00 / 158,00 |
| Essencial | 180,00 / 113,00 | 242,00 / 140,80 | 238,00 / 143,00 |
| Evolução | 167,00 / 100,00 | 231,00 / 132,00 | 225,00 / 136,00 |
| Evolução Familiar | 156,00 / 93,00 | — | — |
| Transformação | 143,00 / 90,00 | 186,00 / 117,00 | 215,00 / 129,00 |
| Transformação Familiar | 137,00 / 85,00 | 178,00 / 112,00 | 205,00 / 126,00 |
| Vitalidade | 134,00 / 80,00 | 175,00 / 105,00 | 200,00 / 124,00 |
| Vitalidade Familiar | 129,00 / 77,00 | 170,00 / 100,00 | 195,00 / 118,00 |

**Pilates em Grupo** (valor por aula; 1x / 2x / 3x por semana):

| Plano | 1x | 2x | 3x |
|---|---|---|---|
| Mensal | 82,50 | 73,63 | 58,91 |
| Trimestral | 73,36 | 65,81 | 52,50 |
| Semestral | 70,06 | 62,57 | 51,06 |
| Semestral Familiar | 67,27 | 60,41 | 49,98 |
| Anual | 63,46 | 55,85 | 48,91 |
| Anual Familiar | 58,38 | 54,58 | 47,83 |

## Migrations (Flyway — depois de fechar o Consultation)
- `CHECK` do exclusive arc em `contract_item` (antes `contract`) e `consultation`.
- Unique de "uma linha de preço vigente" via coluna gerada `current_flag` (também
  resolve concorrência no cadastro de preço; por isso o `saveAndFlush` no reajuste).
- Trocar `ddl-auto` para `validate`.

## Futuro (anotado, sem prioridade)
- Módulo de **saldo do paciente** (ver acima) e **agendamento** integrado (hoje a
  clínica usa outro software).
- Categorias de prestador (Padrão, Estagiário...) com par de percentuais.
- `PUT` de grupo/técnica/plano (renomear), `activate()` onde fizer sentido.
- Todos os planos em todos os grupos por duração (cadastro).
- Reajuste agendado (`validFrom` futuro).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
