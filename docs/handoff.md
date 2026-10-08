# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code. Leia também
> `docs/architecture.md` e `docs/domain-model.md` antes de começar.
> Atualizado em 2026-10-08.
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

## Onde paramos (2026-10-08)

- **PR aberto** da branch `feature/testeFluxoCrud`: 409 ao inativar registro já
  inativo (código) + definições do `Consultation` (só docs).
- **Testes manuais pelo Swagger: abandonados.** Os testes serão feitos pela
  **interface gráfica**, que será criada depois de finalizar o `Consultation`.
  Nada do backend foi executado ainda desde a padronização (inclusive o 409).
- **Branch atual:** `feature/consultation` (último commit `0696dd1`), working tree
  limpo. Nenhum código do `Consultation` escrito ainda.

### Definição do `Consultation` — o que já está decidido (no `domain-model.md`)

1. **Status: só 3** — `ATTENDED`, `NO_SHOW`, `CANCELED_LATE` (desmarcou com menos
   de 6h). O prestador só lança o que gera repasse → **todo atendimento lançado é
   cobrado (consome sessão) e gera repasse cheio**. Desmarcação dentro do prazo e
   cancelamento pela clínica não são lançados (ficam no software de agendamento
   da clínica). Sem `countsTowardsBilling()`. Regra das 6h aplicada por quem lança.
2. **Comissão pela origem do paciente:**
   - `Patient.referringProviderId` (FK nullable): prestador que trouxe o paciente;
     `null` = paciente da clínica. Pode ser alterada; ao **atribuir** um prestador,
     ele precisa existir (404) e estar ativo (400); manter o mesmo é permitido.
     No `PUT`, nulo torna o paciente "da clínica".
   - `Provider`: `commissionPercentage` (padrão **40%**) e
     `referralCommissionPercentage` (padrão **45%**), opcionais no cadastro (vazio
     = padrão, constantes no `ProviderEntity`), ajustáveis por prestador (ex:
     estagiário). Invariante: referral **nunca menor** que o padrão (400).
   - No lançamento: se o prestador que atendeu trouxe o **titular do contrato** →
     referral; senão → padrão. Vai para o snapshot `commissionPercentageApplied`.
   - Mapa de módulos mudou: `patient` passa a referenciar `provider` (sem ciclo).

### Próximo passo: implementação do que já foi decidido (aguardando minha aprovação)

Plano proposto pelo Claude (eu ainda não aprovei):

| Passo | O quê |
|---|---|
| 1 | `ConsultationStatus` com os 3 valores + comentários em português |
| 2 | `Provider`: campo `referralCommissionPercentage`; constantes 40/45; padrão aplicado na criação quando vier nulo; `validateReferralNotBelowCommission` (400); `Percentage.isLessThan(other)`; DTO de cadastro com os dois opcionais; resposta com os dois |
| 3 | `Patient`: `referringProvider` (`@ManyToOne` LAZY, nullable); `updateReferringProvider(provider)` na entidade com a regra "ativo ao atribuir, salvo se o mesmo de antes"; service busca o prestador (404) via `ProviderRepository`; request com `referringProviderId` opcional; resposta com id **e nome** do prestador (precedente: `TechniqueResponseDTO.pricingGroupName`) |

**Decisão técnica pendente (recomendação do Claude):** no `PUT` do prestador, os
dois percentuais são **obrigatórios** — o padrão só vale na criação; senão um
estagiário 30/35 voltaria para 40/45 por um campo esquecido.

Depois: `docker compose down -v` (coluna nova obrigatória em `provider`).

### Definição do `Consultation` — o que falta (retomar antes de escrever a entidade)

- **Parte 2 — resolução de preço** (adiada por mim). Já respondido, **ainda não
  documentado**:
  - o atendimento usa o **preço do contrato** (linha travada), não o vigente no
    dia; reajuste só vale para contratos novos/renovados;
  - técnica de outro grupo → usa o **mesmo plano** no grupo da técnica atendida
    (ex: Essencial do Individual Padrão → Essencial do Individual Especializado);
    substitui o "modal de escolha livre" do `domain-model.md` (corrigir também o
    passo 3, que diz "busca o preço vigente").
  - **Perguntas abertas:** (a) de qual data é a linha do outro grupo — proposta:
    a vigente no `startDate` da versão do contrato; (b) duração diferente no
    mesmo grupo (60 → 30 min) segue a mesma lógica?; (c) plano inexistente no
    outro grupo (Evolução Familiar só no Padrão; Pilates em Grupo × grupos por
    duração têm planos e eixos diferentes) — bloquear, usar Avulso, ou outra regra?
- **Parte 3 — repasse:** cálculo (`baseValue` × percentual), **modo de
  arredondamento** (ex: 35% de R$ 58,91 = R$ 20,6185), renomear `repasseValue` →
  `payoutValue` (sugestão), `Percentage.applyTo(Money)`.
- **Parte 4 — contrato do atendimento:** achar o contrato vigente de quem foi
  atendido (titular **ou** beneficiário); contrato vencido ou inexistente.
- **Parte 5 — saldo:** `sessionCount` − atendimentos lançados na cadeia de versões
  do contrato (só `DURATION_BASED`); encerramento com sessões restantes (N2).
- **Parte 6 — ciclo de vida do lançamento:** atendimento lançado pode ser
  alterado/excluído? (gera repasse na hora, sem aprovação).
- **Parte 7 — quem lança** (deixei para depois): desenhar com `providerId`
  explícito no request; avaliar `launchedByUserId`. Se o admin passar a lançar,
  rever o "usuário obrigatório" do prestador.
- Precisão: `session_value` `DECIMAL(10,2)` × `base_value` `DECIMAL(12,2)`.

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
- `CHECK` do exclusive arc em `contract` e `consultation`.
- Unique de "uma linha de preço vigente" via coluna gerada `current_flag` (também
  resolve concorrência no cadastro de preço; por isso o `saveAndFlush` no reajuste).
- Trocar `ddl-auto` para `validate`.

## Futuro (anotado, sem prioridade)
- Módulo de **agendamento** integrado (hoje a clínica usa outro software).
- Categorias de prestador (Padrão, Estagiário...) com par de percentuais.
- `PUT` de grupo/técnica/plano (renomear), `activate()` onde fizer sentido.
- Evolução Familiar nos grupos Especializado, Em Dupla e Em Grupo.
- Reajuste agendado (`validFrom` futuro).
- Autenticação: senha em texto puro; `SecurityConfig` com `permitAll`.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Sem testes automatizados.
