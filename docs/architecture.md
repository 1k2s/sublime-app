# Sublime Fisioterapia — Arquitetura

## Sobre o projeto

Sistema de controle de atendimento e repasse de honorários para a clínica Sublime
Fisioterapia. Serve dois propósitos: entregar um sistema de negócio funcional e
funcionar como exercício prático de arquitetura de software e inglês técnico.

**Stack:** Java + Spring Boot (monolito modular), MySQL, ReactJS. Versões, setup e
ambiente: [`development.md`](development.md). Fases, estado atual e próximos
passos: [`roadmap.md`](roadmap.md).

## Convenção de idioma (importante, sempre seguir)

Toda nomenclatura de código — classes, atributos, métodos, pacotes — em **inglês**.
Comentários no código e toda documentação/discussão em **português**. Não misturar:
nunca criar uma classe ou atributo com nome em português.

**Mensagens ao usuário em português.** Todo texto que chega na resposta da API é
em português: mensagens das exceptions (`BusinessRuleException`,
`ConflictException`, `ResourceNotFoundException`), mensagens de validação dos DTOs
(`message = "..."`) e os textos do `GlobalExceptionHandler`. Nomes de campo citados
na mensagem aparecem como no JSON (ex: "`frequency` é obrigatória"), porque são o
que o frontend envia.

**Exceção deliberada:** conceitos específicos do contexto brasileiro sem
equivalente correto em inglês mantêm o nome em português — ex: `cep` em
`Address` (não é o mesmo conceito de um "zip code" americano, então traduzir
seria impreciso, não só uma questão de idioma). Isso é exceção pontual, não
abertura geral: qualquer novo caso deve ter a mesma justificativa (conceito de
domínio sem tradução fiel), não conveniência.

## Arquitetura: monolito modular

Um único deploy, separado em módulos com fronteiras de domínio
([ADR 0001](adr/0001-monolito-modular.md)).

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

- `pricing`: técnicas, planos e catálogo de preços (`Technique`, `Plan`,
  `PricingGroup`, `SessionDurationPrice`, `SessionFrequencyPrice`). Ficam juntos porque
  mudam sempre em conjunto e nenhum outro módulo deve conhecer a fiação interna
  entre eles.
- `user`: autenticação genérica (`User`).
- `provider`: dado de negócio do prestador (`Provider`), referencia `user`.
- `patient`: cadastro de pacientes (`Patient`), referencia `provider` — a origem
  do paciente (prestador que o trouxe; `null` = clínica) define o percentual de
  repasse ([ADR 0012](adr/0012-comissao-pela-origem-do-paciente.md)).
- `contract`: contrato do paciente (`Contract` + itens `ContractItem`, um por grupo
  de preço contratado), referencia `patient` + `pricing`.
- `consultation`: lançamento de atendimento (`Consultation`), referencia todos os
  anteriores. É o módulo mais "no topo" da cadeia de dependência; por isso o saldo
  de sessões do contrato é calculado aqui, não no `contract`.

Entidades, atributos e regras de negócio: [`domain-model.md`](domain-model.md) —
consulte esse arquivo sempre que for mexer em qualquer entidade de domínio, antes
de propor mudanças de schema.

## Princípios de modelagem já fixados (não reabrir sem justificativa forte)

1. **Value Objects, não entidades**, para dados sem identidade própria. Duas
   estratégias de persistência, escolhidas pela quantidade de campos do VO
   ([ADR 0002](adr/0002-value-object-de-um-valor.md)):
   - **Vários campos → `@Embeddable` + `@Embedded`, como `class`** (ex: `Address`
     dentro de `Patient`). Cada campo vira uma coluna. Precisa ser `class` porque o
     Hibernate instancia o VO sozinho: construtor sem argumentos `protected` e
     campos não-`final`, preenchidos por reflexão.
   - **Um valor só → `record` + `AttributeConverter` com `autoApply = true`** (ex:
     `Money` + `MoneyConverter`, no módulo `pricing`; `Percentage` +
     `PercentageConverter`, no módulo `provider`). O VO fica no módulo dono do
     conceito, sem criar dependência nova entre módulos. Quem cria o VO é o
     converter (via `Money.of`), nunca o Hibernate, então o VO fica livre de JPA,
     imutável e com o construtor validado como única porta de entrada. A coluna
     mantém o nome e a precisão do `@Column` de cada campo, sem
     `@AttributeOverride`.
   - Os DTOs continuam com os tipos do JSON (ex: `BigDecimal`); o service monta o
     VO e o passa à entidade (ex: `PatientService.toAddress`, `Money.of` no
     `PricingService`).
   - Formatação de exibição (ex: `R$ 1.234,56`) não é regra de negócio: fica no
     frontend, nunca no VO. A API devolve número.
2. **Nunca persistir dado derivável que serve só para exibição** (ex: percentual de
   desconto). Sempre persistir dado que já representa uma obrigação financeira
   consumada (ex: valor de um atendimento já lançado).
3. **Historização por `validFrom`/`validTo`, nunca `UPDATE` direto**, em qualquer
   tabela de preço. Reajuste sempre fecha a linha antiga e abre uma nova
   ([ADR 0003](adr/0003-preco-historizado.md)).
4. **"Exclusive arc"**: quando uma entidade pode referenciar uma de duas tabelas
   diferentes (nunca as duas), usar duas FKs nullable garantindo exatamente uma
   preenchida. Usado em `ContractItem` e `Consultation` para
   `sessionDurationPriceId` / `sessionFrequencyPriceId`. Garantido no código
   (`ContractItemDraft` e `@AssertTrue` no DTO; validação no construtor do
   `ConsultationEntity`) e no banco, por um `CHECK` (migration V2; ver "Banco e
   schema" no [`development.md`](development.md)).
5. **Snapshot de valores financeiros no momento em que viram obrigação.** Valores
   tecnicamente deriváveis (valor da sessão, percentual aplicado, repasse) são
   gravados quando representam obrigação de pagamento já consumada — nunca devem
   mudar retroativamente se o catálogo de preço ou a comissão do prestador mudar
   depois. Ex: `baseValue`, `commissionPercentageApplied` e `payoutValue` do
   `Consultation`.
6. **Regra fixa por valor de enum vira método no enum**, não coluna extra no banco
   nem `if` espalhado pelo código (ex: `PlanType.isContractable()`,
   `SessionFrequency.weeksToComplete(...)`).
7. **Entidade rica, não anêmica: cada validação mora na camada certa.** Critério:
   *"para verificar essa regra, preciso consultar outros registros no banco?"*
   - **DTO — formato do request.** Campos obrigatórios, tamanho, e-mail, e
     combinação de campos do request via `@AssertTrue` (ex: exclusive arc e
     frequência em `ContractItemRequestDTO`). Devolve todos os erros de uma vez.
   - **Entidade — regras dela, que não precisam do banco.** Ficam nos
     construtores/fábricas e nos métodos de mudança de estado, para valer em
     qualquer caminho que crie ou altere a entidade (ex: fábricas
     `ContractItemDraft.withDurationPrice/withFrequencyPrice`, endereço completo em
     `Address`). Regra de um valor isolado mora no VO dele (ex: comissão 0–100% no
     `Percentage`, valor não negativo no `Money`). Pergunta sobre si mesma é
     respondida pela própria entidade (ex: `SessionDurationPrice.isCurrent()`).
     Lançam `BusinessRuleException` (400).
   - **Validação em método privado com nome descritivo**, nunca `if` direto no
     corpo do construtor/fábrica: o construtor só chama os validadores e atribui
     os campos (ex: `PatientEntity.validatePersonalInfo`,
     `PlanEntity.validateAmountsForType`). Métodos de mudança de estado seguem o
     mesmo padrão (ex: `requireCurrent()`, `requireAmendable()`).
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
   Quando o agregado tem **entidade interna** (ex: `Contract` + `ContractItem`,
   [ADR 0008](adr/0008-contrato-com-itens.md)):
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
| Consulta com relação | `@EntityGraph` quando a resposta usa a relação (evita N+1) | `TechniqueRepository.findByActiveTrue`, `PlanRepository.findAll`, `ConsultationRepository.search` |
| Consulta que o nome do método não expressa | `@Query` (JPQL) com `@Param`, só quando o método derivado não dá conta (ex: "ou" com `left join`, filtros opcionais `(:x is null or ...)`); senão, método derivado | `ContractRepository.findActiveCoveringDateByHolderOrBeneficiary`, `ConsultationRepository.search` |
| Endpoint de apoio à tela | leitura que expõe uma regra do backend para o frontend não repetir | `GET /api/techniques/{id}/prices`, `GET /api/contracts/term-preview` |

## Formato dos erros

`GlobalExceptionHandler`, sempre `ProblemDetail` (RFC 7807), mensagens em
português:

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
