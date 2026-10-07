# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code. **Não versionado** (está no
> `.git/info/exclude`). Leia também `docs/architecture.md` e `docs/domain-model.md`
> antes de começar.
> Atualizado em 2026-10-07.

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
  código em inglês. Está na "Convenção de idioma" do `architecture.md`.
- **Um service por agregado.**
- **Dados de teste passam pelas classes (API), nunca por SQL direto.**
- **Handoff só é atualizado quando eu pedir** (uso para trocar de computador).
- Conversa e documentação em português; código em inglês.
- Compile (`./mvnw -q compile`) depois das mudanças e diga claramente o que foi
  ou não testado.
- **Onde cada coisa roda:** desenvolvimento, execução e testes no meu computador
  (VS Code + Docker/MySQL). A sessão no Claude Code web é só para conversar/planejar.

## Padrão de construção (todos os módulos já estão nele)

| Camada | Padrão | Precedente |
|---|---|---|
| Controller | construtor explícito, `@Tag` + `@Operation` + `@ApiResponses` em português, POST → 201, DELETE (soft) → 204 (409 se já inativo) | `PatientController`, `TechniqueController` |
| DTO | `record`; request com `message` em português; response com `fromEntity` estático; DTO de alteração separado quando um campo não pode mudar | `PlanRequestDTO`, `ProviderUpdateRequestDTO` |
| Service | um por agregado, construtor explícito, `@Transactional` (`readOnly` em leitura), `getXOrThrow` (404), `existsBy...` (409), sem `save()` após alterar (dirty checking), soft delete = `deactivate` | `PatientService`, `ProviderService` |
| Entidade | construtor de negócio + `private static validate...`, `active = true`, `deactivate()` com `requireActive()` (409) | `PatientEntity`, `PlanEntity` |
| VO de um valor | `record` + `AttributeConverter` `autoApply`; DTO continua `BigDecimal`, service faz `X.of(...)`; rejeita mais de 2 casas, nunca arredonda | `Money` (pricing), `Percentage` (provider) |
| Unicidade | `unique = true` na coluna + `existsBy...` no service (409). Collation do MySQL ignora maiúsculas/acentos | `PatientRepository.existsByCpf` |
| Erros | `GlobalExceptionHandler`: ProblemDetail; DTO → `errors` (mapa campo→mensagem); exceptions → `detail`; JSON ilegível e parâmetro de URL inválido → 400 com mensagem em português | — |

## Onde paramos (2026-10-07)

**Branch:** `feature/testeFluxoCrud` (criada a partir do PR da padronização,
commit `f985a28 feat: pendências finalizadas`).

**Não commitado nesta branch:** 409 ao inativar registro já inativo (`requireActive()`
em Patient, Technique, Provider, User e Contract + `409` no Swagger dos 5 `DELETE`
+ seção "Soft delete" no `domain-model.md`). Compila; não testado.

### Decisões da rodada de padronização (já no código e nos docs)

- Todos os módulos no padrão (patient, pricing, user, provider, contract).
- `Address`: `street`, `numberHouse`, `city`, `cep` obrigatórios quando o endereço é
  informado (só `complement` opcional); CEP com 8 dígitos sem hífen. Endereço como
  um todo é opcional. `PUT` do paciente: `address` nulo **mantém** o atual;
  `phone`/`email` nulos apagam. Não há como remover endereço (aceito).
- `Percentage` (VO, módulo `provider`) para a comissão: 0–100, máx. 2 casas.
- `PUT /api/providers/{id}` não recebe `userId` (`ProviderUpdateRequestDTO`).
- Prestador exige usuário **existente e ativo** na criação (400 se inativo).
  Inativar o usuário de um prestador ativo é permitido (é como o admin remove o
  acesso). Prestador sem usuário foi discutido e **descartado** (motivo no
  `domain-model.md`).
- `user`: soft delete (`active`), 409 para e-mail duplicado.
- Inativar registro já inativo → **409**.
- Contrato: `delete` → `deactivate` (mesma URL).
- Todas as mensagens de erro em português.

## Próximo passo: testes manuais pelo Swagger

Decidido: **testar tudo à mão pelo Swagger** (sem script de dados), um módulo por
vez, seguindo as dependências. O Claude entrega o roteiro de um módulo, eu testo e
reporto o que divergiu (número do caso + resposta), e só então segue o próximo.

| Ordem | Módulo | O que testar | Status |
|---|---|---|---|
| 1 | patient | criar, consultar, alterar, inativar | ⏳ roteiro entregue (abaixo), **não testado** |
| 2 | user | criar, consultar, alterar, inativar | roteiro a criar |
| 3 | provider | criar, consultar, alterar, inativar | roteiro a criar |
| 4 | pricing | grupos → técnicas → planos → preços (sem `PUT`; "alterar" preço = reajuste), inativar técnica, tabela vigente | roteiro a criar |
| 5 | contract | criar, consultar, aditivo, inativar | roteiro a criar |

### Preparação (uma vez)
1. Abrir o **Docker Desktop** (estava fechado na última checagem).
2. `docker compose down -v` e `docker compose up -d` (obrigatório: colunas e
   constraints novas — `user.active`, `unique` em nome de plano, `session_count`
   nullable).
3. Subir a aplicação. **Subir sem erro já é o primeiro teste** (consultas derivadas
   do Spring Data só são validadas na subida).
4. Swagger: http://localhost:8080/swagger-ui.html. Banco zerado → ids em sequência.

### Roteiro 1 — Patient (`/api/patients`)

Criação (POST):
- **P1** Maria com endereço → 201, id 1:
  `{"name":"Maria Silva","cpf":"11111111111","birthDate":"1985-03-15","phone":"19999990001","email":"maria@email.com","address":{"street":"Rua das Flores","numberHouse":"100","city":"Campinas","complement":"Apto 12","cep":"13010000"}}`
- **P2** João sem endereço → 201, id 2, `address: null`:
  `{"name":"João Souza","cpf":"22222222222","birthDate":"1990-07-20"}`
- **P3** Carla com endereço sem complemento → 201, id 3:
  `{"name":"Carla Lima","cpf":"33333333333","birthDate":"1978-11-02","address":{"street":"Av. Brasil","numberHouse":"S/N","city":"Valinhos","cep":"13270000"}}`

Erros na criação:
- **P4** P2 de novo → 409 "Já existe um paciente cadastrado com o CPF: 22222222222"
- **P5** `{}` → 400, `errors` com `name`, `cpf`, `birthDate`
- **P6** CPF `"222.222.222-22"` → 400 `errors.cpf`
- **P7** `birthDate` `"2090-01-01"` → 400 `errors.birthDate`
- **P8** `birthDate` `"10/05/1990"` → 400 `detail` "Data inválida no campo 'birthDate'. Use o formato AAAA-MM-DD." (sem `errors`)
- **P9** `email` `"maria-sem-arroba"` → 400 `errors.email`
- **P10** endereço sem `city` e com `cep` `"13010-000"` → 400 `errors["address.city"]` e `errors["address.cep"]`
- **P11** JSON quebrado → 400 "O corpo da requisição está ausente ou não é um JSON válido."

Consulta:
- **P12** GET lista → 3 pacientes · **P13** GET /1 → Maria · **P14** GET /999 → 404
  "Paciente não encontrado com id: 999" · **P15** GET /abc → 400 "Valor inválido
  para o parâmetro 'id': abc."

Alteração (PUT):
- **P16** PUT /1 sem `address`, com phone/email novos → 200, endereço preservado:
  `{"name":"Maria Silva","cpf":"11111111111","birthDate":"1985-03-15","phone":"19988887777","email":"maria.nova@email.com"}`
- **P17** PUT /2 com endereço → 200, João passa a ter endereço
- **P18** PUT /1 com CPF do João → 409
- **P19** PUT /1 com o próprio CPF → 200 (sem falso 409)
- **P20** PUT /1 sem phone/email → 200, ficam `null`
- **P21** PUT /999 → 404

Soft delete:
- **P22** DELETE /3 → 204 · **P23** GET /3 → `active: false` · **P24** lista ainda
  tem os 3 · **P25** DELETE /3 de novo → 409 "O paciente já está inativo." ·
  **P26** DELETE /999 → 404

Estado final esperado: Maria (1) e João (2) ativos; **Carla (3) inativa** — usada
no contrato para testar "paciente inativo não pode ser titular". Não reativar.

### Casos já anotados para os próximos roteiros
- **user:** e-mail duplicado 409 (criação e PUT, sem falso 409 no próprio e-mail),
  `role` inválido (`"XYZ"`) → 400 com valores aceitos, senha nunca aparece na
  resposta, DELETE → `active: false`, DELETE de novo → 409.
- **provider:** usuário inexistente 404, usuário já com prestador 409, **usuário
  inativo 400**, comissão `33.333` → 400, `101` → 400, `PUT` sem `userId` → 200,
  inativar o usuário de um prestador ativo → 204 (prestador segue ativo).
- **pricing:** nome duplicado 409 (grupo, técnica, plano), `pricingModel` inválido
  400, técnica com grupo inexistente 404, inativar técnica 204/409 e ela some do
  `GET`, plano com/sem `sessionCount`, `sessionCount = 0` → 400, preço novo,
  **reajuste** fechando a linha antiga (`validTo = hoje`, intervalo vazio
  `[hoje, hoje)` — válido), grupo com `pricingModel` errado → 400, plano sem
  `sessionCount` em preço por duração → 400, valor com 3 casas → 400,
  grupo/plano inexistente → 404, `GET /api/techniques/{id}/prices` só com vigentes.
- **contract:** "Como testar" do PR do contract — caminho feliz (duração e
  frequência, `planId`/`weeklyFrequency` copiados da linha), 400/404/409, contrato
  vencido não bloqueia, aditivos (manter linha reajustada ok, trocar para linha
  fechada 400, prorrogar vencido, alterar versão inativa 409), titular inativo
  (Carla) 400, inativar 204/409.

### Dados da clínica (para o roteiro de pricing)

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

Para o teste não precisa cadastrar tudo — um subconjunto que cubra os dois
modelos de preço basta (ex: Individual Padrão + Individual Especializado + Em
Grupo; RPG, Liberação Miofascial, Quiropraxia, Pilates em Grupo; Avulso,
Essencial, Evolução, Mensal, Trimestral).

## Ambiente — avisos

- **Arquivos voltando sozinhos para versões antigas** (`TechniqueController` no
  working tree; `domain-model.md` chegou a entrar no commit `96b2659` numa versão de
  setembro, já restaurada). Suspeitas: OneDrive sincronizando a Área de Trabalho,
  ou aba antiga do VS Code salva por cima. **Conferir o diff antes de commitar.**
- **Erros falsos do Lombok na IDE** ("Can't initialize javac processor...",
  `getX()` não encontrado). O Maven compila normalmente; só atrapalha o editor.
- **Maven sem acesso ao Central** (erro de certificado PKIX): plugins novos (ex:
  `dependency:tree`) não baixam. Build normal funciona com o cache local.
- Spring Boot **4.1.1**, Java 25, **Jackson 3** (`tools.jackson`, não
  `com.fasterxml`).
- PowerShell 5.1: script com acentos precisa de UTF-8 **com BOM** e corpo enviado
  em bytes UTF-8 (se algum dia criarmos scripts em `scripts/`).

## Pendências do Consultation (depois dos testes)

- `ConsultationEntity`: falta FK de `contract`, snapshot do repasse, construtor e
  métodos de estado. Desenhar do zero seguindo o princípio 7. `baseValue`/repasse
  → `Money`; `commissionPercentageApplied` → `Percentage`.
- **Status:** diferença entre `CANCELED` e `UNSCHEDULED_WITH_NOTICE` e quais contam
  para o repasse → `ConsultationStatus.countsTowardsBilling()`; o `domain-model.md`
  ainda cita `NO_SHOW`/`CANCELLED_EARLY`/`CANCELLED_WITH_CHARGE` (não existem).
- **`repasseValue` em português** → sugestão `payoutValue`.
- **Modo de arredondamento do repasse** (decisão de negócio) → entra com
  `Percentage.applyTo(Money)`.
- **Quem lança o atendimento:** desenhar com `providerId` explícito no request (não
  inferido do login) para permitir no futuro o admin lançar por um prestador;
  avaliar `launchedByUserId` para rastreabilidade. Se o admin passar a lançar,
  rever o "usuário obrigatório" do prestador (documentado no `domain-model.md`).
- Precisão: `session_value` `DECIMAL(10,2)` × `base_value` `DECIMAL(12,2)`.
- **Saldo de sessões** (só `DURATION_BASED`) somando a cadeia de versões do
  contrato; Pilates em Grupo sem saldo de sessões ("em dia" depende de `Payment`).
- **Encerramento do contrato (N2)** com sessões restantes.
- **Busca do contrato vigente** de quem foi atendido: titular **ou** beneficiário.

## Migrations (Flyway — depois de fechar o Consultation)
- `CHECK` do exclusive arc em `contract` e `consultation`.
- Unique de "uma linha de preço vigente" via coluna gerada `current_flag` (também
  resolve concorrência no cadastro de preço; por isso o `saveAndFlush` no reajuste).
- Trocar `ddl-auto` para `validate`.

## Futuro (anotado, sem prioridade)
- `PUT` de grupo/técnica/plano (renomear), `activate()` onde fizer sentido.
- Evolução Familiar nos grupos Especializado, Em Dupla e Em Grupo.
- Reajuste agendado (`validFrom` futuro).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
