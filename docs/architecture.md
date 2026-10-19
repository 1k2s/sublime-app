# Sublime Fisioterapia — Sistema de Controle de Atendimento

## Sobre o projeto

Sistema de controle de atendimento e repasse de honorários para a clínica Sublime
Fisioterapia. Serve dois propósitos: entregar um sistema de negócio funcional e
funcionar como exercício prático de arquitetura de software e inglês técnico.

**Stack:** Java + Spring Boot (monolito modular), MySQL, ReactJS (versões e ambiente
em "Stack e ambiente de desenvolvimento").

**Escopo por fase:** Fase 1 — cadastros, contratos e lançamento de atendimentos
(repasse ao prestador e saldo de sessões); Fase 2 — `Payment` e saldo financeiro do
paciente (visão da clínica); Fase 3 — app de consulta de saldo para o paciente.

## Convenção de idioma (importante, sempre seguir)

Toda nomenclatura de código — classes, atributos, métodos, pacotes — em **inglês**.
Comentários no código e toda documentação/discussão em **português**. Não misturar:
nunca criar uma classe ou atributo com nome em português.

**Mensagens ao usuário em português.** Todo texto que chega na resposta da API é
em português: mensagens das exceptions (`BusinessRuleException`,
`ConflictException`, `ResourceNotFoundException`), mensagens de validação dos DTOs
(`message = "..."`) e os textos do `GlobalExceptionHandler`. Nomes de campo citados
na mensagem aparecem como no JSON (ex: "`weeklyFrequency` é obrigatória"), porque
são o que o frontend envia.

**Exceção deliberada:** conceitos específicos do contexto brasileiro sem
equivalente correto em inglês mantêm o nome em português — ex: `cep` em
`Address` (não é o mesmo conceito de um "zip code" americano, então traduzir
seria impreciso, não só uma questão de idioma). Isso é exceção pontual, não
abertura geral: qualquer novo caso deve ter a mesma justificativa (conceito de
domínio sem tradução fiel), não conveniência.

## Arquitetura: monolito modular

- Cada módulo é um pacote logo abaixo do pacote base, nomeado com o mesmo nome do
  agregado raiz que encapsula (ex: pacote `contract` contém a classe `Contract`).
- **Dependência entre módulos é sempre unidirecional.** Um módulo nunca deve
  referenciar de volta um módulo que depende dele — isso cria ciclo e quebra a
  verificação de monolito modular (se formos usar Spring Modulith).
- Relacionamentos JPA entre agregados de módulos diferentes são sempre
  unidirecionais: só o lado "dependente" guarda a FK/referência de objeto; o lado
  referenciado nunca tem coleção de volta.

### Mapa de módulos (Fase 1)

```
pricing          user          ← módulos de base, sem dependência
   |               |
   |           provider        ← referencia user
   |               |
   |           patient         ← referencia provider (origem do paciente)
    \             /
     contract                  ← referencia patient + pricing
         \
       consultation            ← depende de todos os anteriores
```

- `patient`: cadastro de pacientes (`Patient`), referencia `provider` — a origem
  do paciente (`referringProviderId`, prestador que o trouxe; `null` = clínica)
  define o percentual de repasse. Até a decisão da comissão pela origem, `patient`
  era módulo de base; a dependência nova é unidirecional (`provider` nunca
  referencia `patient`).
- `pricing`: técnicas, planos e catálogo de preços (`Technique`, `Plan`,
  `PricingGroup`, `SessionDurationPrice`, `SessionFrequencyPrice`). Ficam juntos porque
  mudam sempre em conjunto e nenhum outro módulo deve conhecer a fiação interna
  entre eles.
- `user`: autenticação genérica (`User`).
- `provider`: dado de negócio do prestador (`Provider`), referencia `user`.
- `contract`: contrato do paciente (`Contract` + itens `ContractItem`, um por grupo
  de preço contratado), referencia `patient` + `pricing`.
- `consultation`: lançamento de atendimento (`Consultation`), referencia todos os
  anteriores. É o módulo mais "no topo" da cadeia de dependência.

Referência completa de entidades, atributos e decisões de negócio:
`docs/domain-model.md` — consulte esse arquivo sempre que for mexer em qualquer
entidade de domínio, antes de propor mudanças de schema.

## Princípios de modelagem já fixados (não reabrir sem justificativa forte)

1. **Value Objects, não entidades**, para dados sem identidade própria. Duas
   estratégias de persistência, escolhidas pela quantidade de campos do VO:
   - **Vários campos → `@Embeddable` + `@Embedded`, como `class`** (ex: `Address`
     dentro de `Patient`). Cada campo vira uma coluna. Precisa ser `class` porque o
     Hibernate instancia o VO sozinho: construtor sem argumentos `protected` e
     campos não-`final`, preenchidos por reflexão.
   - **Um valor só → `record` + `AttributeConverter` com `autoApply = true`** (ex:
     `Money` + `MoneyConverter`, no módulo `pricing`; `Percentage` +
     `PercentageConverter`, no módulo `provider`). O VO fica no módulo dono do
     conceito, sem criar dependência nova entre módulos. O converter traduz o VO para
     a coluna e de volta; quem cria o VO é o converter (via `Money.of`), nunca o
     Hibernate. Por isso o VO pode ser `record`: fica livre de JPA, imutável e
     com o construtor validado como única porta de entrada. A coluna mantém o nome
     e a precisão do `@Column` de cada campo, sem `@AttributeOverride`.
   - Os DTOs continuam com os tipos do JSON (ex: `BigDecimal`); o service monta o
     VO e o passa à entidade (ex: `PatientService.toAddress`, `Money.of` no
     `PricingService`).
   - Formatação de exibição (ex: `R$ 1.234,56`) não é regra de negócio: fica no
     frontend, nunca no VO. A API devolve número.
2. **Nunca persistir dado derivável que serve só para exibição** (ex: percentual de
   desconto). Sempre persistir dado que já representa uma obrigação financeira
   consumada (ex: valor de um atendimento já lançado).
3. **Historização por `validFrom`/`validTo`, nunca `UPDATE` direto**, em qualquer
   tabela de preço. Reajuste sempre fecha a linha antiga e abre uma nova.
4. **"Exclusive arc"**: quando uma entidade pode referenciar uma de duas tabelas
   diferentes (nunca as duas), usar duas FKs nullable + `CHECK` constraint
   garantindo exatamente uma preenchida. Usado em `ContractItem` e `Consultation`
   para `sessionDurationPriceId` / `sessionFrequencyPriceId`.
5. **Snapshot de valores financeiros no momento do lançamento.** `Consultation`
   grava `baseValue`, `commissionPercentageApplied` e `repasseValue` mesmo sendo
   tecnicamente deriváveis, porque são obrigação de pagamento já consumada — nunca
   devem mudar retroativamente se o catálogo de preço ou a comissão do prestador
   mudar depois.
6. **Regra fixa por valor de enum vira método no enum**, não coluna extra no banco
   (ex: se um valor de `ConsultationStatus` passar a ter regra própria, como um
   repasse diferente, ela vira método do enum). Hoje não há caso no código: o
   `countsTowardsBilling()` previsto deixou de existir porque todo atendimento
   lançado é cobrado (ver `Consultation` no `domain-model.md`).
7. **Entidade rica, não anêmica: cada validação mora na camada certa.** Critério:
   *"para verificar essa regra, preciso consultar outros registros no banco?"*
   - **DTO — formato do request.** Campos obrigatórios, tamanho, e-mail, e
     combinação de campos do request via `@AssertTrue` (ex: exclusive arc e
     `weeklyFrequency` em `ContractRequestDTO`). Devolve todos os erros de uma vez.
   - **Entidade — regras dela, que não precisam do banco.** Ficam nos
     construtores/fábricas e nos métodos de mudança de estado, para valer em
     qualquer caminho que crie ou altere a entidade (ex: fábricas
     `ContractEntity.withDurationPrice/withFrequencyPrice`, endereço completo em
     `Address`). Regra de um valor isolado mora no VO dele (ex: comissão 0–100% no
     `Percentage`, valor não negativo no `Money`). Pergunta sobre si mesma é
     respondida pela própria entidade (ex: `SessionDurationPrice.isCurrent()`).
     Lançam `BusinessRuleException` (400).
   - **Validação em método privado com nome descritivo**, nunca `if` direto no
     corpo do construtor/fábrica: o construtor só chama os validadores e atribui
     os campos (ex: `PatientEntity.validatePersonalInfo`,
     `PlanEntity.validateNameAndSessionCount`). Métodos de mudança de estado
     seguem o mesmo padrão (ex: `requireCurrent()`, `requireAmendable()`).
   - **Service — regras que dependem do banco + orquestração.** Unicidade e
     consultas a outros registros (ex: CPF já cadastrado, paciente com contrato
     vigente), buscar entidades por id, chamar a entidade, salvar.
   - Construtores de negócio (e fábricas) são a **única porta de entrada**: nunca
     `@AllArgsConstructor` em entidade — ele gera um construtor público que ignora
     todas as regras. O construtor sem argumentos fica `protected` (uso do
     Hibernate). Value objects seguem a mesma ideia: o construtor com todos os
     campos é o próprio construtor de negócio e valida o VO (`Address` escrito à
     mão; `Money`/`Percentage` no construtor compacto do `record`).
   - **Sem `@Setter` em entidade** (nem `protected`, que também libera acesso ao
     pacote inteiro). Estado só muda por métodos de negócio (`update`,
     `deactivate`, `updateAddress`...), que aplicam as regras. O Hibernate não
     precisa de setters: as annotations ficam nos campos.
   - Regra de negócio pode aparecer no DTO **e** na entidade quando convém
     (ex: comissão): o DTO dá a mensagem junto com os demais erros de formato; a
     entidade garante a regra para qualquer chamador.
8. **Agregado: a raiz é a única porta de entrada.** Agregado é o conjunto de
   entidades e value objects que mudam juntos e precisam estar sempre coerentes
   como um todo; a **raiz** é a entidade por onde tudo passa. A maioria dos
   agregados do projeto é uma entidade com seus VOs (ex: `Patient` + `Address`).
   Quando o agregado tem **entidade interna** (ex: `Contract` + `ContractItem`):
   - a entidade interna não tem repositório nem service próprios, e o construtor
     dela não é público — só a raiz cria e altera (no `contract`, via rascunhos
     `ContractItemDraft` montados pelo service);
   - é salva junto com a raiz (`cascade`); a coleção da raiz é exposta como cópia
     somente leitura;
   - regras que olham o conjunto (ex: um item por grupo, fim calculado pelos
     itens) ficam na raiz;
   - outros agregados referenciam só a raiz (ex: `Consultation` → `Contract`, nunca
     → `ContractItem`).
   - A coleção raiz → entidade interna é permitida: a proibição de coleção de volta
     vale **entre módulos**, e os dois são o mesmo agregado e o mesmo módulo.

## Padrão de construção (todos os módulos implementados seguem)

| Camada | Padrão | Precedente |
|---|---|---|
| Controller | construtor explícito (sem `@RequiredArgsConstructor`); `@Tag` + `@Operation` + `@ApiResponses` + `@Parameter` em português; POST → 201, DELETE (soft delete) → 204, 409 se já inativo | `PatientController`, `TechniqueController` |
| DTO | `record`; request com `message` em português em toda anotação de validação; combinação de campos via `@AssertTrue`; response com `fromEntity` estático; **DTO de alteração separado** quando um campo não pode mudar | `PlanRequestDTO`, `ProviderUpdateRequestDTO`, `ContractItemRequestDTO` |
| Service | **um por agregado**; construtor explícito; `@Transactional` (`readOnly` em leitura); `getXOrThrow` (404); `existsBy...` (409); sem `save()` depois de alterar entidade carregada (dirty checking); soft delete = `deactivate`; monta VOs e rascunhos a partir do DTO | `PatientService`, `ProviderService`, `ContractService` |
| Entidade | construtor de negócio (ou fábrica) + `private static validate...` com nome descritivo; `active = true` no campo; `deactivate()` com `requireActive()` (409); sem `@Setter`/`@AllArgsConstructor`; construtor vazio `protected` | `PatientEntity`, `PlanEntity`, `ContractEntity` |
| Enum com regra | regra fixa por valor vira método do enum (princípio 6), com nome de pergunta (`is...`, `has...`) | `PlanType`, `SessionFrequency` |
| VO de um valor | `record` + `AttributeConverter` com `autoApply`; DTO continua `BigDecimal`, service faz `X.of(...)`; rejeita mais de 2 casas, nunca arredonda | `Money` (pricing), `Percentage` (provider) |
| Unicidade | `unique = true` na coluna (garantia real) + `existsBy...` no service (409 com mensagem clara). Collation do MySQL ignora maiúsculas/acentos | `PatientRepository.existsByCpf` |
| Consulta com relação | `@EntityGraph` quando a resposta usa a relação (evita N+1) | `TechniqueRepository.findByActiveTrue`, `PlanRepository.findAll` |
| Endpoint de apoio à tela | leitura que expõe uma regra do backend para o frontend não repetir | `GET /api/techniques/{id}/prices`, `GET /api/contracts/term-preview` |

**Formato dos erros** (`GlobalExceptionHandler`, sempre `ProblemDetail` — RFC 7807,
mensagens em português):

| Origem | Status | Corpo |
|---|---|---|
| Validação do DTO (`@Valid`) | 400 | `detail` genérico + mapa **`errors`** (campo → mensagem; campo aninhado como `address.cep`; `@AssertTrue` aparece com o nome do método sem `is`) |
| JSON ilegível (enum inexistente, tipo errado, data fora do formato, JSON malformado) | 400 | `detail` citando o campo (e, em enum, os valores aceitos; em data, o formato `AAAA-MM-DD`) |
| Parâmetro de URL com tipo errado (ex: `/api/patients/abc`) | 400 | `detail` citando o parâmetro |
| `BusinessRuleException` (regra da entidade/VO) | 400 | `detail` |
| `ResourceNotFoundException` | 404 | `detail` |
| `ConflictException` (duplicidade, estado inválido) | 409 | `detail` |
| `DataIntegrityViolationException` (constraint que escapou das checagens) | 409 | `detail` genérico |

Ordem em que os erros acontecem: leitura do JSON → DTO (todos os erros de uma
vez) → service (404/409, um por vez) → entidade (400, um por vez) → banco. O
frontend trata `errors` como erro de formulário (por campo) e `detail` como
mensagem geral.

## Stack e ambiente de desenvolvimento

- **Spring Boot 4.1.1**, **Java 25**, **Jackson 3** (pacote `tools.jackson`, não
  `com.fasterxml`), Lombok, springdoc (Swagger em
  `http://localhost:8080/swagger-ui.html`).
- **MySQL 8** via `docker-compose.yml` (porta 3307, banco `sublime_db`).
- **Schema:** `spring.jpa.hibernate.ddl-auto=update` até as migrations do Flyway.
  O `update` cria colunas e tabelas, mas **não remove colunas nem ajusta
  constraints antigas** — quando uma entidade perde campos ou muda obrigatoriedade,
  é preciso zerar o banco: `docker compose down -v` → `docker compose up -d` → subir
  a aplicação (o Hibernate recria as tabelas). As mudanças de `plan`, `contract`,
  `contract_item`, `provider` e `patient` de outubro exigem esse reset.
- **Dados de teste** são criados **pela API** (nunca por `INSERT`/`data.sql`), para
  passar pelas regras do domínio; scripts de apoio, se houver, ficam em `scripts/`.
- **Fluxo de git:** branch `feature/...` (ou `feat/...`) → PR para a `dev`.

**Problemas conhecidos do ambiente** (não são do código):
- **Erros falsos do Lombok no VS Code** ("Can't initialize javac processor...",
  `getX()` não encontrado): o processador do Lombok falha dentro da extensão Java.
  O Maven compila normalmente — vale o `./mvnw -q compile`.
- **Sem acesso ao Maven Central e ao GitHub por HTTPS** (erro de certificado
  PKIX/SSL, provável proxy da rede): só o cache local do Maven funciona (plugins
  novos não baixam) e o `git push` pode falhar. Tentar outra rede ou
  `git config --global http.sslBackend schannel`; nunca desligar a verificação SSL.
- **Arquivos que voltaram sozinhos para versões antigas** (já aconteceu com
  `TechniqueController` e `domain-model.md`): suspeita de OneDrive sincronizando a
  Área de Trabalho ou aba antiga do VS Code salva por cima. **Conferir o diff
  antes de commitar.**

## Estado atual (Fase 1 — atualizado em 2026-10-19)

| Módulo | Situação |
|---|---|
| `patient` | implementado — incl. origem do paciente (`referringProvider`) e soft delete com 409 |
| `user` | implementado — soft delete, e-mail único (409) |
| `provider` | implementado — dois percentuais (padrão 40/45), usuário ativo obrigatório |
| `pricing` | implementado — grupos, técnicas, planos (`PlanType`, `termWeeks`, de-para), preços com reajuste historizado |
| `contract` | implementado — cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio |
| `consultation` | **em definição** — status (`ConsultationStatus`) implementado; regras de lançamento decididas e documentadas; repasse, ciclo de vida e outras pendências abertas (ver `Consultation` no `domain-model.md`) |

- **Nada do backend foi executado** desde a padronização dos módulos (outubro): o
  código compila, mas não houve teste em execução.
- **Testes:** decidido testar pela **interface gráfica**, que será construída depois
  de finalizar o `consultation` (não por Swagger nem por testes automatizados por
  enquanto).

## Próximos passos

1. **Fechar as pendências do `Consultation`** (lista no fim da seção `Consultation`
   do `domain-model.md`): cálculo e arredondamento do repasse, nome `payoutValue`,
   precisão, ciclo de vida do lançamento, quem lança, vigência de referência e saldo
   esgotado.
2. **Implementar o `consultation`** (entidade do zero, lançamento com contratos
   elegíveis e resolução de preço, saldo por item, endpoint
   `eligible-contracts`).
3. **Interface gráfica** (ReactJS) e testes por ela.
4. **Migrations do Flyway** e o que depende delas (ver "Pendências técnicas").

## Pendências técnicas

- **Migrations (Flyway — depois de fechar o `Consultation`):**
  - `CHECK` do exclusive arc em `contract_item` e `consultation`
    (`(session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL)`);
    até lá, garantido no código;
  - unique de "uma linha de preço vigente": coluna gerada
    `current_flag = IF(valid_to IS NULL, 1, NULL) STORED` +
    `UNIQUE (pricing_group_id, plan_id, duration_minutes|weekly_frequency, current_flag)`.
    Resolve também a concorrência no cadastro de preço (duas requisições
    simultâneas criando duas linhas vigentes). Por causa dela, o reajuste fecha a
    linha antiga com `saveAndFlush` antes do INSERT da nova (o Hibernate executa
    INSERTs antes de UPDATEs no flush);
  - trocar `ddl-auto` para `validate` e zerar o banco local;
  - dados reais do catálogo (grupos, técnicas, planos, preços) podem virar
    migration em `src/main/resources/db/migration/`.

## Melhorias futuras (anotadas, sem prioridade)

- **Autenticação:** senha em texto puro e `SecurityConfig` com `permitAll` — entram
  hash de senha, login e perfis (`ADMIN`/`PROVIDER`) com visibilidade por perfil.
- `PUT` de grupo, técnica e plano (renomear); `activate()` onde fizer sentido.
- Reajuste de preço agendado (`validFrom` futuro).
- Categorias de prestador (Padrão, Estagiário...) com o par de percentuais.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Testes automatizados.
- Módulos ainda não modelados — `Payment`, saldo do paciente, agendamento, app do
  paciente: ver "Não modelado ainda" no `domain-model.md`.
