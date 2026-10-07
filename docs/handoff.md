# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code. Leia também
> `docs/architecture.md` e `docs/domain-model.md` antes de começar.
> Atualizado em 2026-10-06.
>
> ⚠️ Apesar de "uso pessoal", este arquivo **está versionado** (entrou no commit
> `a7976b6`). Se for para ficar fora do repositório: `.gitignore` + `git rm --cached`.

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
- **Dados de teste passam pelas classes, não por SQL direto.** INSERT no banco
  ignora as regras do domínio; prefiro criar pela API para testar o fluxo
  DTO → service → entidade. Scripts de apoio ficam em `scripts/` (raiz), nunca em
  `docs/` nem em `src/main/resources/`.
- Conversa e documentação em português; código em inglês.
- Compile (`./mvnw -q compile`) depois das mudanças e diga claramente o que foi
  ou não testado.
- **Onde cada coisa roda:** desenvolvimento, execução e testes acontecem no meu
  computador (Claude Code na extensão do VS Code, com Docker/MySQL). A sessão no
  Claude Code web é só para conversar/planejar de outro computador — não tente
  subir a aplicação ou o banco por lá.

## Padrão de construção (seguir o módulo `pricing`, o mais recente)

| Camada | Padrão | Precedente |
|---|---|---|
| Controller | construtor explícito (sem `@RequiredArgsConstructor`), `@Tag` + `@Operation` + `@ApiResponses` em português, POST → 201, DELETE (soft) → 204 | `PlanController`, `TechniqueController` |
| DTO | `record`; request com mensagens de validação em português; response com `fromEntity` estático | `PlanRequestDTO`, `PlanResponseDTO` |
| Service | um por agregado, construtor explícito, `@Transactional` (`readOnly` em leitura), 404 `ResourceNotFoundException`, 409 `ConflictException` | `PlanService`, `TechniqueService` |
| Entidade | construtor de negócio + `private static validate...`, `active = true` no campo, `deactivate()` | `PlanEntity`, `TechniqueEntity` |
| Unicidade | `unique = true` na coluna (garantia real) + `existsBy...` no service (mensagem clara, 409). Collation do MySQL ignora maiúsculas/acentos | `PatientRepository.existsByCpf`, `PricingGroupRepository.existsByName` |

`patient`, `provider` e `user` ainda estão no padrão antigo (DTO `class` com
`@Data`, sem Swagger, `@RequiredArgsConstructor`).

## Onde paramos (2026-10-06)

**Objetivo da rodada:** deixar todos os domínios prontos para teste, seguindo o
padrão do `pricing`. Ao tentar subir a aplicação, decidimos criar os dados de
teste pela API em vez de SQL — e para isso faltavam endpoints de `pricing_group`
e `technique`.

**Branch:** `feat/finalizacaoConsultation`. **Nada desta sessão foi commitado.**

### Decisões tomadas nesta sessão

1. Endpoints só os necessários para teste (sem `PUT` por enquanto).
2. Técnica **não muda de grupo** (reclassificar = técnica nova + inativar a antiga).
3. `pricingModel` do grupo **nunca muda** depois da criação.
4. **Nome duplicado → 409** em grupo, técnica e plano.
5. `findActiveTechniques` saiu do `PricingService` para o `TechniqueService`;
   `findCurrentPriceTable` continua no `PricingService` (é consulta de preço).

### Plano da rodada

| # | Módulo | Status |
|---|---|---|
| A | `pricing_group`: construtor, `existsByName`, DTOs, `PricingGroupService`, `PricingGroupController` (`POST` + `GET /api/pricing-groups`) | ✅ compila |
| B | `technique`: construtor + `deactivate()`, `existsByName`, `TechniqueRequestDTO`, `TechniqueService` (create/findActive/deactivate), `POST` + `DELETE /api/techniques` | ✅ compila |
| C | `plan`: `unique` no nome + `existsByName` → 409 no `PlanService.create` | ⏳ **próximo** |
| D | `patient`: DTOs → `record` + `fromEntity` (incl. `AddressDTO`), Swagger no controller, `@Transactional` no service. Entidade já está no padrão | pendente |
| E | `provider`: idem D + tirar o `if (user == null)` direto do construtor (`ProviderEntity.java:52`) para um `validateUser` | pendente |
| F | docs: `domain-model.md` (unicidade de nome em grupo/técnica/plano; `pricingModel` e grupo da técnica imutáveis) + este handoff | pendente |

Compilar e parar para revisão ao fim de cada módulo.

### Perguntas em aberto

- **Módulo `user`** entra nesta rodada? Está no padrão antigo, faz **delete
  físico** (`UserService.java:53`) e não checa e-mail duplicado.
- Inativar algo já inativo devolve 204 de novo (técnica, paciente, prestador,
  contrato). Manter ou virar erro? (regra de negócio)
- Como popular os dados de teste: tudo à mão pelo Swagger, ou um script em
  `scripts/` (ex: PowerShell com `Invoke-RestMethod`) chamando a API? Recomendação
  do Claude: script para o volume (4 grupos, 8 técnicas, 14 planos, 62 preços);
  Swagger para os casos de erro.

### Melhorias listadas, aguardando minha decisão

- `pricingModel` inválido no JSON (ex: `"XYZ"`) dá 400 no formato padrão do
  Spring, não no mapa `errors` do `GlobalExceptionHandler` (falta tratar
  `HttpMessageNotReadableException`).
- Diagrama ER do `domain-model.md` ainda mostra `BigDecimal sessionValue`; trocar
  para `Money`?

### Ambiente

- Última tentativa de subir a aplicação falhou com "Unable to determine Dialect
  without JDBC metadata" — causa: **Docker Desktop estava fechado** (MySQL fora do
  ar), não é problema de código.
- Antes de testar: abrir o Docker Desktop → `docker compose down -v` (zera o
  banco; as entidades mudaram e o `ddl-auto=update` não remove colunas/constraints
  antigas) → `docker compose up -d` → subir a aplicação (Hibernate cria as tabelas).
- `ddl-auto=update` continua até as migrations do Flyway.

## Roteiro de testes (depois de D/E/F)

**Cadastros pela API** (ids nascem na ordem de criação):
1. 4 grupos, 8 técnicas (+ inativar uma criada para teste), 14 planos, 62 preços
   (tabelas de valores abaixo).
2. Usuários (ADMIN e PROVIDER), prestadores (um com cada role), ~5 pacientes (um
   inativado via DELETE).

**Pela API o `validFrom` é sempre hoje** — o reajuste de teste fecha uma linha
criada no mesmo dia (intervalo vazio `[hoje, hoje)`, comportamento válido). Se
quiser histórico antigo, um `UPDATE` pontual de `valid_from` depois.

**Casos:** baseado no "Como testar" do PR do contract — caminho feliz (duração e
frequência), 400/404/409, contrato vencido não bloqueia, aditivos (manter linha
reajustada ok, trocar para linha fechada 400, prorrogar vencido, alterar versão
inativa 409). Somar:
- grupo: nome duplicado 409, `pricingModel` ausente/inválido 400;
- técnica: nome duplicado 409, grupo inexistente 404, inativar 204/404, inativa
  some do `GET`, contrato com técnica inativa 400;
- plano: com/sem `sessionCount`, `sessionCount = 0` → 400, nome duplicado 409;
- preço: novo, reajuste fechando a linha antiga, grupo com `pricingModel` errado
  → 400, plano sem `sessionCount` em preço por duração → 400, valor com 3 casas →
  400, grupo/plano inexistente → 404.

### Dados da clínica

**Grupos:** Individual Padrão (`DURATION_BASED`), Individual Especializado
(`DURATION_BASED`), Em Dupla (`DURATION_BASED`), Em Grupo (`FREQUENCY_BASED`).

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

## Value Object `Money` (já implementado — referência)

- `pricing/domain/Money.java`: `record Money(BigDecimal amount)`. Obrigatório, não
  negativo, máx. 2 casas (mais casas → `BusinessRuleException`/400, **nunca
  arredonda**), escala normalizada em 2. Zero é válido. `Money.of(...)` e
  `isPositive()`.
- `pricing/domain/MoneyConverter.java`: `AttributeConverter<Money, BigDecimal>`
  com `autoApply = true`. DTOs continuam `BigDecimal`; service faz `Money.of`.
- Formatação `R$ 1.234,56` fica no frontend (`Intl.NumberFormat`).
- Cadastro de preço usa **`saveAndFlush`** ao fechar a linha vigente: o Hibernate
  executa INSERTs antes de UPDATEs no flush, e com o unique de `current_flag`
  (migrations) a linha nova colidiria com a antiga.

## Pendências do Consultation (depois dos testes — combinado discutir só então)

- `ConsultationEntity`: falta a FK de `contract`, o snapshot do repasse,
  construtor e métodos de estado. Desenhar do zero seguindo o princípio 7.
  `baseValue`/repasse passam a ser `Money`.
- **Modo de arredondamento do repasse** (decisão de negócio): `HALF_UP`,
  `HALF_EVEN` ou sempre para baixo? Ex: 35% de R$ 58,91 = R$ 20,6185.
- **Status:** definir a diferença entre `CANCELED` e `UNSCHEDULED_WITH_NOTICE` e
  quais status contam para o repasse → `ConsultationStatus.countsTowardsBilling()`
  e alinhar os nomes no `domain-model.md` (o texto ainda cita
  `NO_SHOW`/`CANCELLED_EARLY`/`CANCELLED_WITH_CHARGE`, que não existem no enum).
- **`repasseValue` está em português** (viola a convenção). Sugestão:
  `payoutValue` (recomendado) ou `commissionValue`.
- Precisão inconsistente: `session_value` `DECIMAL(10,2)` × `base_value`
  `DECIMAL(12,2)` — alinhar ao redesenhar.
- **Saldo de sessões** (só `DURATION_BASED`): `sessionCount` − atendimentos que
  contam, somando **toda a cadeia de versões** do contrato. Cálculo no módulo
  `consultation`. Pilates em Grupo: sem saldo de sessões.
- **Encerramento do contrato (N2):** com sessões restantes, avisar; admin decide
  prorrogar (aditivo) ou levar as sessões para o contrato seguinte; decisão salva;
  DTO de saldo para o frontend.
- **Busca do contrato vigente** de quem foi atendido: titular **ou** beneficiário.

## Migrations (Flyway — depois de fechar o Consultation)
- `CHECK ((session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL))`
  em `contract` e `consultation`.
- Unique de "uma linha de preço vigente": coluna gerada
  `current_flag = IF(valid_to IS NULL, 1, NULL) STORED` + `UNIQUE (pricing_group_id,
  plan_id, duration_minutes|weekly_frequency, current_flag)`.
- Trocar `ddl-auto` para `validate` e zerar o banco local.
- Dados reais do pricing (grupos, técnicas, planos, preços) podem virar migration
  em `src/main/resources/db/migration/`; dados fictícios ficam em `scripts/`.

## Futuro (anotado, sem prioridade)
- `PUT` de grupo (renomear) e técnica (renomear), `activate()` onde fizer sentido.
- Evolução Familiar nos grupos Especializado, Em Dupla e Em Grupo.
- Reajuste agendado (`validFrom` futuro).
- VO `Percentage` para comissão (hoje `BigDecimal` com validação 0–100 no Provider).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`) e padronizar o
  idioma das mensagens (DTO em português, entidades em inglês).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
