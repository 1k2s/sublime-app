# Sublime Fisioterapia — Sistema de Controle de Atendimento

## Sobre o projeto

Sistema de controle de atendimento e repasse de honorários para a clínica Sublime
Fisioterapia. Serve dois propósitos: entregar um sistema de negócio funcional e
funcionar como exercício prático de arquitetura de software e inglês técnico.

**Stack:** Java + Spring Boot (monolito modular), MySQL, ReactJS.

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
- `contract`: contrato do paciente (`Contract`), referencia `patient` + `pricing`.
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
   garantindo exatamente uma preenchida. Usado em `Contract` e `Consultation` para
   `sessionDurationPriceId` / `sessionFrequencyPriceId`.
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

## Estado atual (Fase 1 em andamento)

Modelagem de domínio concluída para: `Patient`, `Technique`, `Plan`,
`PricingGroup`, `SessionDurationPrice`, `SessionFrequencyPrice`, `Contract`, `User`,
`Provider`, `Consultation`. Ver `docs/domain-model.md` para atributos completos.

Pendente, ainda não modelado: domínio de `Payment`/saldo do paciente (Fase 2, visão
da clínica cruzando pagamentos com atendimentos).

## Ordem de implementação recomendada

1. `patient`, `pricing`, `user` — entidades e repositórios. Dados de teste são
   criados **pela API** (não por `INSERT`/`data.sql`), para passar pelas regras
   do domínio; scripts de apoio ficam em `scripts/`.
2. `provider` (depende de `user`) e `contract` (depende de `patient` + `pricing`).
3. `consultation` — aqui sim com atenção total, é o módulo com a lógica de negócio
   mais densa e o alvo do frontend de teste desta fase.
