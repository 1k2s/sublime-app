# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code (não versionado). Leia também
> `docs/architecture.md` e `docs/domain-model.md` antes de começar.
> Atualizado em 2026-10-02.

## Como eu gosto de trabalhar

- **Explique antes de alterar.** Para cada mudança, diga o que muda e por quê
  (em 1–3 frases) antes de aplicar. Passos pequenos; nada de várias alterações em
  lote sem explicação.
- **Ao apontar um problema, mostre o código.** Explicação + trecho exato
  (arquivo:linha) + um exemplo concreto do que dá errado.
- **Análises longas em partes.** Apresente uma parte, espere meu retorno e só
  então siga para a próxima.
- **Correção ≠ melhoria ≠ feature.** Numa rodada de correções, faça só a mudança
  mínima. Melhorias (refatorar, extrair método, validação extra) você lista para
  eu decidir — não aplica junto.
- **Decisões de negócio são minhas.** Quando houver dúvida de regra, pergunte; não
  presuma. Quando houver escolha técnica, dê uma recomendação, não só as opções.
- **Consulte o projeto antes de sugerir.** Toda recomendação técnica parte de como
  os módulos existentes já resolvem o mesmo caso (citar o precedente em
  arquivo:linha) e dos princípios de monolito modular e DDD do `architecture.md`.
  Se for caso novo, sem precedente, diga isso.
- **Seguir o princípio 7 do `architecture.md`** (entidade rica: DTO = formato,
  entidade = regras dela, service = regras que dependem do banco + orquestração).
- **Sem `if` direto no construtor da entidade.** Validações em método privado com
  nome que diz o que valida (ex: `validateNameAndSessionCount`), como
  `PatientEntity.validatePersonalInfo`. Métodos de estado idem (`requireCurrent()`).
- **Um service por agregado** (ex: `PlanService` separado do `PricingService`).
- Conversa e documentação em português; código em inglês.
- Compile (`./mvnw -q compile`) depois das mudanças e diga claramente o que foi
  ou não testado.
- **Onde cada coisa roda:** desenvolvimento, execução e testes acontecem no meu
  computador (Claude Code na extensão do VS Code, com Docker/MySQL). A sessão no
  Claude Code web é só para conversar/planejar de outro computador — não tente
  subir a aplicação ou o banco por lá.

## Onde paramos (2026-10-02)

Antes de testar pricing/contract, decidimos criar o **cadastro de planos e de
preços** (não existia nenhum endpoint de escrita no pricing) e **renomear os
planos**. Trabalho em 5 passos:

1. ✅ **Documentação** (`domain-model.md`) — feito.
2. ✅ **Entidades do pricing** — feito, compila. Nada foi executado ainda.
3. ⏳ **Service, DTOs e controllers** — próximo passo (detalhe abaixo).
4. Compilar.
5. Script SQL + testes pelo Swagger.

**Alterações não commitadas** (branch `dev`): `docs/domain-model.md`,
`PlanEntity`, `SessionDurationPriceEntity`, `SessionFrequencyPriceEntity`.

### Decisões tomadas nesta sessão (já no `domain-model.md`)

- **Planos novos** (14 no total):
  - Grupos por duração: Avulso (1), Essencial (4), Evolução (12), Evolução
    Familiar (12, por enquanto só no Individual Padrão), Transformação /
    Transformação Familiar (26), Vitalidade / Vitalidade Familiar (52).
  - Pilates em Grupo mantém os nomes antigos: Mensal, Trimestral, Semestral,
    Semestral Familiar, Anual, Anual Familiar — com `sessionCount = null`.
- **`Plan.sessionCount` nullable:** no Grupo o plano é mensalidade (plano ×
  frequência), não pacote de sessões. Preço por duração exige plano com
  `sessionCount` (validado na linha de preço).
- **Saldo do Pilates em Grupo:** não usa saldo de sessões; é "em dia" se a parcela
  do mês foi paga → depende de `Payment` (Fase 2). Na Fase 1 o Grupo não tem saldo.
  Repasse ao prestador vale igual para todos.
- **Cadastro de valor = fluxo único (novo preço ou reajuste):** se há linha
  vigente da combinação, fecha com `validTo = hoje` e cria a nova com
  `validFrom = hoje`; senão só cria. `validFrom` sempre hoje (não vem do request).
- **`validTo` é exclusivo** (`[validFrom, validTo)`); **`endDate` do contrato é
  inclusivo**. Reajuste no mesmo dia gera intervalo vazio — permitido, sem bloqueio.
  Consulta por data usa `validTo > X`.

### O que foi feito no código (passo 2)

- `PlanEntity`: `Integer sessionCount` nullable, nasce `active = true`, construtor
  com `validateNameAndSessionCount`.
- `SessionDurationPriceEntity` / `SessionFrequencyPriceEntity`: construtor público
  (sem fábrica — só há uma forma de criar) com `validatePricingGroupAndPlan`
  (pricingModel do grupo, plano ativo; na duração também `sessionCount`) e
  `validateDurationAndSessionValue` / `validateFrequencyAndSessionValue`;
  `close(validTo)` com `requireCurrent()` + `validateValidTo()` (aceita
  `validTo == validFrom`).

## Próximo passo: passo 3 (service, DTOs, controllers)

Combinado:
- **Repositórios:** busca da linha vigente da combinação, retornando `Optional`
  (ex: `findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull`, e o
  equivalente com `WeeklyFrequency`).
- **DTOs:** `PlanRequestDTO` (`@NotBlank name`, `sessionCount` opcional `@Min(1)`),
  `PlanResponseDTO`, `DurationPriceRequestDTO` / `FrequencyPriceRequestDTO`
  (`pricingGroupId`, `planId`, eixo `@Min(1)`, `sessionValue` > 0). Resposta de
  preço reaproveita o `SessionPriceResponseDTO`.
- **`PlanService` + `PlanController`:** `POST /api/plans`, `GET /api/plans`.
- **`PricingService` (existente) + `PriceController`:** `POST /api/prices/duration`
  e `POST /api/prices/frequency` com o fluxo único (busca vigente →
  `ifPresent(close(hoje))` → cria a nova). O `PricingService` usa o
  `PlanRepository` direto (mesmo módulo; precedente: `ContractService` usa
  `PatientRepository`).

**Aguardando minha confirmação** (recomendação do Claude entre parênteses):
- `GET /api/pricing-groups` — incluir agora ou depois? (depois: no teste os ids
  vêm do SQL; o grupo não tem service próprio e o lugar dele não é óbvio).
- Concorrência no cadastro de preço (duas requisições simultâneas podem criar duas
  linhas vigentes) — só anotar? (sim: proteção real é o unique com `current_flag`
  das migrations; só o admin cadastra preço).

## Depois: passo 5 (testar pricing e contract)

- **Recriar o banco** (`docker compose down -v`), subir a aplicação uma vez para o
  Hibernate criar as tabelas.
- **Script SQL** (sugestão: `docs/test-data/seed-pricing.sql`), rodado à mão uma
  vez: `docker exec -i mysql-dev mysql -uroot -proot sublime_db < <arquivo>`.
  Não usar `data.sql` (com `ddl-auto=update` duplicaria a cada boot).
  - 4 grupos + 8 técnicas + 1 técnica inativa fictícia (não há endpoint para
    inativar técnica).
  - Planos e preços podem ir no SQL (são 62 linhas) — o endpoint de preço é
    testado criando a **linha histórica via reajuste** (substitui o `UPDATE` manual
    que estava previsto).
- **Cadastros pelo Swagger:** usuários (ADMIN e PROVIDER), prestadores (um com
  cada role), ~5 pacientes (um inativado via DELETE).
- **Roteiro:** baseado no "Como testar" do PR do contract — caminho feliz (duração
  e frequência), 400/404/409, contrato vencido não bloqueia, aditivos (manter linha
  reajustada ok, trocar para linha fechada 400, prorrogar vencido, alterar versão
  inativa 409).

### Valores (valor por sessão; 60 min / 30 min)

| Plano | Individual Padrão | Individual Especializado | Em Dupla |
|---|---|---|---|
| Avulso | 220,00 / 132,00 | 275,00 / 176,00 | 265,00 / 158,00 |
| Essencial | 180,00 / 113,00 | 242,00 / 140,80 | 238,00 / **143,00** |
| Evolução | 167,00 / 100,00 | 231,00 / 132,00 | 225,00 / 136,00 |
| Evolução Familiar | 156,00 / 93,00 | — | — |
| Transformação | 143,00 / 90,00 | 186,00 / 117,00 | 215,00 / 129,00 |
| Transformação Familiar | 137,00 / 85,00 | 178,00 / 112,00 | 205,00 / 126,00 |
| Vitalidade | 134,00 / 80,00 | 175,00 / 105,00 | 200,00 / 124,00 |
| Vitalidade Familiar | 129,00 / 77,00 | 170,00 / 100,00 | 195,00 / 118,00 |

- Individual Especializado: Essencial = antigo "6 sessões" (mesmo valor, agora 4
  sessões); Evolução = antigo "12 sessões".
- Em Dupla 30 min Essencial: valor corrigido para 143,00 (a planilha tinha 130).
- Evolução Familiar nos demais grupos: adicionar depois.

**Pilates em Grupo** (valor por aula; 1x / 2x / 3x por semana):

| Plano | 1x | 2x | 3x |
|---|---|---|---|
| Mensal | 82,50 | 73,63 | 58,91 |
| Trimestral | 73,36 | 65,81 | 52,50 |
| Semestral | 70,06 | 62,57 | 51,06 |
| Semestral Familiar | 67,27 | 60,41 | 49,98 |
| Anual | 63,46 | 55,85 | 48,91 |
| Anual Familiar | 58,38 | 54,58 | 47,83 |

## Pendências do Consultation (depois dos testes)

- `ConsultationEntity` incompleta: FK de `contract` comentada, falta o snapshot
  do valor de repasse, código comentado usa status que não existem
  (`SCHEDULED`, `COMPLETED`...). Deve ser desenhada do zero seguindo o princípio 7.
- **Status:** definir a diferença entre `CANCELED` e `UNSCHEDULED_WITH_NOTICE` e
  quais status contam para o repasse → implementar `ConsultationStatus.countsTowardsBilling()`
  e alinhar os nomes de status no `domain-model.md` (o texto ainda cita
  `NO_SHOW`/`CANCELLED_EARLY`/`CANCELLED_WITH_CHARGE`, que não existem no enum).
- **`repasseValue` está em português** (viola a convenção de idioma). Sugestões:
  `payoutValue` (recomendado) ou `commissionValue`. Renomear no `domain-model.md`
  antes de implementar.
- **Saldo de sessões** (só grupos `DURATION_BASED`): `sessionCount` do plano −
  atendimentos que contam para o repasse, somando **toda a cadeia de versões** do
  contrato (`previousContract`). Cálculo no módulo `consultation` (contract não
  pode depender dele). Pilates em Grupo: sem saldo de sessões (ver decisões acima).
- **Encerramento do contrato (N2):** ao finalizar com sessões restantes, avisar; o
  administrador decide prorrogar (via aditivo) ou levar as sessões para o contrato
  seguinte; essa decisão fica salva; DTO de saldo para o frontend.
- **Busca do contrato vigente** de quem foi atendido: titular **ou** beneficiário
  (no Pilates em Dupla o atendimento pode ser lançado para o acompanhante).

## Migrations (Flyway — depois de fechar o Consultation)
- `CHECK ((session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL))`
  em `contract` e `consultation`.
- Unique de "uma linha de preço vigente": coluna gerada
  `current_flag = IF(valid_to IS NULL, 1, NULL) STORED` + `UNIQUE (pricing_group_id,
  plan_id, duration_minutes|weekly_frequency, current_flag)`. Também resolve a
  concorrência no cadastro de preço.
- Trocar `ddl-auto` para `validate` e zerar o banco local.

## Futuro (anotado, sem prioridade)
- `GET /api/pricing-groups` (se não entrar no passo 3).
- Evolução Familiar nos grupos Especializado, Em Dupla e Em Grupo.
- Reajuste agendado (`validFrom` futuro).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`) e padronizar o
  idioma das mensagens (DTO em português, entidades em inglês).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
