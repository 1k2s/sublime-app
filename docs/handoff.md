# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code (não versionado). Leia também
> `docs/architecture.md` e `docs/domain-model.md` antes de começar.
> Atualizado em 2026-10-02 (fim do dia).

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
  mínima. Melhorias (refatorar, extrair método, validação extra) você lista para
  eu decidir — não aplica junto.
- **Decisões de negócio são minhas.** Quando houver dúvida de regra, pergunte; não
  presuma. Quando houver escolha técnica, dê uma recomendação, não só as opções.
- **Consulte o projeto antes de sugerir — e antes de escrever.** Toda recomendação
  técnica parte de como os módulos existentes já resolvem o mesmo caso (citar o
  precedente em arquivo:linha) e dos princípios de monolito modular e DDD do
  `architecture.md`. Vale para detalhes pequenos também (ex: comparar `BigDecimal`
  com `compareTo`, não `signum()`). Se for caso novo, sem precedente, diga isso.
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

Cadastro de planos e de preços no pricing, antes de testar pricing/contract:

1. ✅ **Documentação** (`domain-model.md`).
2. ✅ **Entidades do pricing** (commit `13ac5a5`).
3. ✅ **Service, DTOs e controllers** — feito, compila.
4. ✅ **Compilar** — ok. **Nada foi executado ainda.**
5. ⏳ **Script SQL + testes pelo Swagger** — próximo passo (proposta abaixo,
   aguardando minha confirmação).

Extra feito nesta sessão: **Value Object `Money`** (ver abaixo).

**Branch:** `feat/finalizacaoConsultation`. **Tudo do passo 3 em diante está sem
commit:** `architecture.md`, `domain-model.md`, `ConsultationEntity` (só remoção
de código comentado), entidades/repositórios/service/DTO de preço alterados, e os
novos `PlanController`, `PriceController`, `PlanService`, `Money`,
`MoneyConverter`, `PlanRequestDTO`, `PlanResponseDTO`, `DurationPriceRequestDTO`,
`FrequencyPriceRequestDTO`.

### O que foi feito no passo 3

- **Repositórios:** `findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull`
  e `...AndWeeklyFrequencyAndValidToIsNull` (retornam `Optional`).
- **DTOs:** `PlanRequestDTO`/`PlanResponseDTO`, `DurationPriceRequestDTO`/
  `FrequencyPriceRequestDTO` (`sessionValue` com `@DecimalMin` exclusivo). Resposta
  de preço reaproveita o `SessionPriceResponseDTO`.
- **`PlanService` + `PlanController`:** `POST /api/plans`, `GET /api/plans` (todos,
  ativos e inativos).
- **`PricingService.createDurationPrice/createFrequencyPrice` + `PriceController`:**
  `POST /api/prices/duration` e `POST /api/prices/frequency`. Fluxo único: busca
  vigente → `close(hoje)` + **`saveAndFlush`** → cria a nova com `validFrom = hoje`.
  O `saveAndFlush` (método nativo do Spring Data, mantido) grava o fechamento antes
  do INSERT: o Hibernate executa INSERTs antes de UPDATEs no flush, e com o unique
  de `current_flag` (migrations) a linha nova colidiria com a antiga.
- `GET /api/pricing-groups`: fica para o futuro (no teste os ids vêm do SQL).
- Concorrência no cadastro de preço: tratar junto com as migrations.

### Value Object `Money` (decidido e implementado)

- `pricing/domain/Money.java`: `record Money(BigDecimal amount)`. Obrigatório, não
  negativo, máx. 2 casas (mais casas → `BusinessRuleException`/400, **nunca
  arredonda**), escala normalizada em 2. Zero é válido. `Money.of(...)` (mantido) e
  `isPositive()`.
- `pricing/domain/MoneyConverter.java`: `AttributeConverter<Money, BigDecimal>`
  com `autoApply = true`.
- Usado em `sessionValue` das duas entidades de preço. Service faz
  `Money.of(dto.sessionValue())` (precedente: `PatientService.toAddress`); DTOs
  continuam `BigDecimal`; resposta devolve `amount()`.
- Documentado no `architecture.md` (princípio 1: `@Embeddable`/class para VO de
  vários campos × `record` + converter para VO de um valor) e no `domain-model.md`.
- Formatação `R$ 1.234,56` fica no frontend (`Intl.NumberFormat`). Se o backend
  gerar documento (impressão do contrato), formatar nessa camada, não no `Money`.

### Melhorias listadas, aguardando minha decisão

- Bloquear nome de plano duplicado (409)? — regra de negócio.
- Diagrama ER do `domain-model.md` ainda mostra `BigDecimal sessionValue` (é o tipo
  da coluna); trocar para `Money`?

## Próximo passo: passo 5 (script SQL + testes)

**Proposta do script** (aguardando minha confirmação):
- Arquivo `docs/test-data/seed-pricing.sql`, rodado à mão uma vez:
  `docker exec -i mysql-dev mysql -uroot -proot sublime_db < docs/test-data/seed-pricing.sql`.
  Não usar `data.sql` (com `ddl-auto=update` duplicaria a cada boot). Com o Flyway,
  os dados reais (grupos, técnicas, planos, preços) podem virar migration.
- Conteúdo: 4 `pricing_group`, 9 `technique` (8 reais + 1 inativa fictícia, em
  bloco comentado no fim), 14 `plan`, 44 `session_duration_price`
  (Padrão 8×2, Especializado 7×2, Dupla 7×2), 18 `session_frequency_price` (6×3).
- Recomendações do Claude:
  1. Ids explícitos em grupos, técnicas e planos (os testes precisam saber os ids);
     auto-increment nas linhas de preço.
  2. `valid_from` fixo no passado (ex: `2026-01-01`), para o reajuste de hoje gerar
     histórico de verdade.
  3. Planos pelo SQL; o `POST /api/plans` é testado com 1–2 planos extras + erros.
  4. Um arquivo só por enquanto.

**Antes:** recriar o banco (`docker compose down -v`) e subir a aplicação uma vez
para o Hibernate criar as tabelas.

**Cadastros pelo Swagger:** usuários (ADMIN e PROVIDER), prestadores (um com cada
role), ~5 pacientes (um inativado via DELETE).

**Roteiro:** baseado no "Como testar" do PR do contract — caminho feliz (duração e
frequência), 400/404/409, contrato vencido não bloqueia, aditivos (manter linha
reajustada ok, trocar para linha fechada 400, prorrogar vencido, alterar versão
inativa 409). Somar: cadastro de plano (com/sem `sessionCount`, `sessionCount = 0`
→ 400), cadastro de preço (novo, reajuste fechando a linha antiga, grupo com
`pricingModel` errado → 400, plano sem `sessionCount` em preço por duração → 400,
valor com 3 casas → 400, grupo/plano inexistente → 404).

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

## Pendências do Consultation (depois dos testes — combinado discutir só então)

- `ConsultationEntity`: código comentado do dev anterior **foi removido**. Falta a
  FK de `contract`, o snapshot do repasse, construtor e métodos de estado. Deve ser
  desenhada do zero seguindo o princípio 7. `baseValue`/repasse passam a ser `Money`.
- **Modo de arredondamento do repasse** (decisão de negócio): `HALF_UP`,
  `HALF_EVEN` ou sempre para baixo? Ex: 35% de R$ 58,91 = R$ 20,6185. O cálculo de
  percentual entra no `Money` quando isso for decidido.
- **Status:** definir a diferença entre `CANCELED` e `UNSCHEDULED_WITH_NOTICE` e
  quais status contam para o repasse → implementar `ConsultationStatus.countsTowardsBilling()`
  e alinhar os nomes de status no `domain-model.md` (o texto ainda cita
  `NO_SHOW`/`CANCELLED_EARLY`/`CANCELLED_WITH_CHARGE`, que não existem no enum).
- **`repasseValue` está em português** (viola a convenção de idioma). Sugestões:
  `payoutValue` (recomendado) ou `commissionValue`. Renomear no `domain-model.md`
  antes de implementar.
- Precisão inconsistente: `session_value` é `DECIMAL(10,2)`, `base_value` é
  `DECIMAL(12,2)` — alinhar ao redesenhar.
- **Saldo de sessões** (só grupos `DURATION_BASED`): `sessionCount` do plano −
  atendimentos que contam para o repasse, somando **toda a cadeia de versões** do
  contrato (`previousContract`). Cálculo no módulo `consultation` (contract não
  pode depender dele). Pilates em Grupo: sem saldo de sessões.
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
  concorrência no cadastro de preço (o `saveAndFlush` já prepara a ordem certa).
- Trocar `ddl-auto` para `validate` e zerar o banco local.
- Seed dos dados reais do pricing pode virar migration.

## Futuro (anotado, sem prioridade)
- `GET /api/pricing-groups`.
- Evolução Familiar nos grupos Especializado, Em Dupla e Em Grupo.
- Reajuste agendado (`validFrom` futuro).
- VO `Percentage` para comissão (hoje `BigDecimal` com validação 0–100 no Provider).
- Formatação de dinheiro em documentos gerados pelo backend (se houver).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`) e padronizar o
  idioma das mensagens (DTO em português, entidades em inglês).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
