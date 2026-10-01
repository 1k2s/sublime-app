# Modelo de domínio — referência completa

> Consulte este arquivo antes de propor qualquer mudança de schema ou entidade.
> Ele documenta não só a estrutura final, mas o raciocínio por trás de cada
> decisão — várias delas já foram tentadas de outra forma e revertidas por um
> motivo específico, registrado abaixo.

## Diagrama entidade-relacionamento

```mermaid
erDiagram
  PRICING_GROUP ||--o{ TECHNIQUE : classifica
  PRICING_GROUP ||--o{ SESSION_DURATION_PRICE : precifica
  PRICING_GROUP ||--o{ SESSION_FREQUENCY_PRICE : precifica
  PLAN ||--o{ SESSION_DURATION_PRICE : precifica
  PLAN ||--o{ SESSION_FREQUENCY_PRICE : precifica
  CONTRACT |o--o| CONTRACT : versao_anterior
  PATIENT ||--o{ CONTRACT : titular
  PATIENT |o--o{ CONTRACT : beneficiario
  TECHNIQUE ||--o{ CONTRACT : ancora
  PLAN ||--o{ CONTRACT : contratado
  SESSION_DURATION_PRICE |o--o{ CONTRACT : trava
  SESSION_FREQUENCY_PRICE |o--o{ CONTRACT : trava
  USER ||--o| PROVIDER : autentica
  PATIENT ||--o{ CONSULTATION : atendido
  CONTRACT ||--o{ CONSULTATION : consome_saldo
  PROVIDER ||--o{ CONSULTATION : realiza
  TECHNIQUE ||--o{ CONSULTATION : executada
  SESSION_DURATION_PRICE |o--o{ CONSULTATION : aplicado
  SESSION_FREQUENCY_PRICE |o--o{ CONSULTATION : aplicado

  PATIENT {
    bigint id PK
    string name
    string cpf
    date birthDate
    string phone
    string email
    string addressStreet
    string addressNumberHouse
    string addressCity
    string addressComplement
    string addressCep
    boolean active
  }

  TECHNIQUE {
    bigint id PK
    string name
    bigint pricingGroupId FK
    boolean active
  }

  PLAN {
    bigint id PK
    string name
    int sessionCount
    boolean active
  }

  PRICING_GROUP {
    bigint id PK
    string name
    string pricingModel
  }

  SESSION_DURATION_PRICE {
    bigint id PK
    bigint pricingGroupId FK
    bigint planId FK
    int durationMinutes
    BigDecimal sessionValue
    date validFrom
    date validTo
  }

  SESSION_FREQUENCY_PRICE {
    bigint id PK
    bigint pricingGroupId FK
    bigint planId FK
    int weeklyFrequency
    BigDecimal sessionValue
    date validFrom
    date validTo
  }

  CONTRACT {
    bigint id PK
    bigint patientId FK
    bigint beneficiaryId FK
    bigint techniqueId FK
    bigint planId FK
    int weeklyFrequency
    date startDate
    date endDate
    string paymentMethod
    bigint sessionDurationPriceId FK
    bigint sessionFrequencyPriceId FK
    boolean active
    bigint previousContractId FK
  }

  USER {
    bigint id PK
    string email
    string password
    string role
  }

  PROVIDER {
    bigint id PK
    bigint userId FK
    string name
    BigDecimal commissionPercentage
    boolean active
  }

  CONSULTATION {
    bigint id PK
    bigint patientId FK
    bigint contractId FK
    bigint providerId FK
    bigint techniqueId FK
    int durationMinutes
    date occurredAt
    string status "ENUM"
    bigint sessionDurationPriceId FK
    bigint sessionFrequencyPriceId FK
    BigDecimal commissionPercentageApplied
    BigDecimal baseValue
    BigDecimal repasseValue
  }
```

Legenda: `addressStreet`/`addressNumberHouse`/`addressCity`/`addressComplement`/
`addressCep` em `Patient` são um Value Object embutido (`@Embeddable`), não uma
tabela própria (`cep`, não `zipCode` — é um conceito específico do endereçamento
brasileiro, não uma tradução literal de "zip code"). `pricingModel`, `role`,
`paymentMethod` e `status` são ENUMs. `sessionDurationPriceId`/`sessionFrequencyPriceId`
(em `Contract` e `Consultation`) são nullable, em padrão "exclusive arc": sempre um
preenchido, nunca os dois.

---

## `Patient`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `name` | string | |
| `cpf` | string | |
| `birthDate` | date | |
| `phone` | string | |
| `email` | string | |
| `address` | VO embutido (street, numberHouse, city, complement, cep) | `@Embeddable`/`@Embedded`, imutável (sem setter) |
| `active` | boolean | soft delete |

**Por que endereço é Value Object, não entidade própria:** não existe caso de uso
em que precisamos rastrear a identidade de um endereço ao longo do tempo — trocar
de endereço é substituição de valor, não edição de um registro com vida própria.

**Por que `Patient` não referencia `Contract`:** o relacionamento é sempre
unidirecional `Contract → Patient`. Se `Patient` tivesse uma coleção de contratos,
o módulo `patient` passaria a depender do módulo `contract`, criando ciclo (já que
o inverso é verdadeiro por natureza).

---

## `Technique`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `name` | string | |
| `pricingGroupId` | FK → PricingGroup | |
| `active` | boolean | |

**Por que sem preço nem duração:** técnicas do mesmo grupo de precificação (ex:
Miofascial, RPG, Massoterapia, Pilates Individual — mesmo grau de complexidade
COFFITO) compartilham exatamente o mesmo valor. Guardar preço na técnica geraria
duplicação replicada em cada reajuste, com risco de divergência silenciosa.

---

## `Plan`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `name` | string | |
| `sessionCount` | int | quantidade de sessões do pacote |
| `active` | boolean | |

**Por que sem frequência semanal, vigência ou desconto:** um paciente pode
contratar um plano Anual (52 sessões) com vigência de 6 meses a 2x/semana — a
mesma quantidade de sessões pode ser consumida em ritmos diferentes. Frequência e
vigência são decisão de negociação, vivem em `Contract`. Desconto é sempre
calculado em tempo de leitura (ver seção de preço abaixo), nunca armazenado.

---

## `PricingGroup`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `name` | string | |
| `pricingModel` | ENUM: `DURATION_BASED` \| `FREQUENCY_BASED` | discriminador |

**Por que existe:** técnicas de mesmo grau de complexidade COFFITO compartilham
uma única tabela de preço. O grupo é quem carrega o preço; a técnica só aponta
para o grupo. Reajustar o valor do grupo atualiza automaticamente todas as
técnicas associadas, sem precisar editar cada uma.

**Por que o discriminador `pricingModel`:** a maioria dos grupos precifica por
`duração da sessão × plano` (`DURATION_BASED`). O Grupo Pilates precifica por
`frequência semanal × plano` (`FREQUENCY_BASED`) — o preço por sessão não é uma
multiplicação simples do valor unitário, é uma tabela de desconto por volume
publicada à parte (embute cálculo de feriados). As duas estratégias não cabem na
mesma tabela sem colunas nulas para a maioria das linhas, por isso duas tabelas
de preço separadas (abaixo).

---

## `SessionDurationPrice` (grupos `DURATION_BASED`)

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `pricingGroupId` | FK → PricingGroup | |
| `planId` | FK → Plan | |
| `durationMinutes` | int | eixo de duração |
| `sessionValue` | BigDecimal | |
| `validFrom` | date | |
| `validTo` | date, nullable | `null` = vigente |

## `SessionFrequencyPrice` (grupos `FREQUENCY_BASED`)

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `pricingGroupId` | FK → PricingGroup | |
| `planId` | FK → Plan | |
| `weeklyFrequency` | int | eixo de frequência |
| `sessionValue` | BigDecimal | |
| `validFrom` | date | |
| `validTo` | date, nullable | `null` = vigente |

**Regra de unicidade:** só pode existir uma linha com `validTo IS NULL` por
combinação de `(pricingGroupId, durationMinutes, planId)` — ou
`(pricingGroupId, weeklyFrequency, planId)` na tabela de frequência.

**Reajuste nunca é `UPDATE`.** Sempre: fecha a linha vigente (`validTo` = ontem) e
insere uma nova com `validFrom` = hoje. Isso preserva o histórico completo de
preços — requisito de rastreabilidade do escopo original do projeto — e garante
que contratos antigos continuem apontando para o valor que estava vigente na
assinatura.

**Percentual de desconto nunca é persistido.** É sempre calculado em tempo de
leitura: `1 − (valorDoPlano / valorDeReferência)`, onde a referência é o Avulso da
mesma técnica/grupo quando existir, ou o Mensal quando não existir Avulso (caso do
Grupo Pilates). Guardar os dois campos (valor absoluto e percentual) gera risco de
dessincronia a cada reajuste.

---

## `Contract`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `patientId` | FK → Patient | titular, obrigatório |
| `beneficiaryId` | FK → Patient, nullable | Pilates em Dupla / plano Familiar |
| `techniqueId` | FK → Technique | técnica-âncora (define com base em qual valor o contrato foi fechado) |
| `planId` | FK → Plan | copiado da linha de preço travada, nunca recebido do request |
| `weeklyFrequency` | int | preço por duração: negociação, informada no request; preço por frequência: copiada da linha de preço |
| `startDate` / `endDate` | date | vigência negociada |
| `paymentMethod` | ENUM | |
| `sessionDurationPriceId` | FK → SessionDurationPrice, nullable | exclusive arc |
| `sessionFrequencyPriceId` | FK → SessionFrequencyPrice, nullable | exclusive arc |
| `active` | boolean | |
| `previousContractId` | FK → Contract, nullable, unique | versão anterior (aditivo); null na primeira versão |

**Sobre `beneficiaryId`:** cobre tanto o Pilates em Dupla (titular + acompanhante
compartilhando o mesmo saldo) quanto os planos Familiares (dois contratos
distintos, cada um com seu próprio titular e beneficiário — o grau de parentesco
entre os titulares é checado manualmente pela clínica na venda, o sistema não
guarda nem valida isso).

**Sobre `techniqueId` ser só "âncora":** o paciente não fica restrito a essa
técnica. `techniqueId` formaliza com base em qual valor o contrato foi
inicialmente fechado; o paciente pode usar qualquer técnica do seu plano
livremente (ver lógica de resolução de preço em `Consultation`, abaixo). Um
paciente tem apenas um contrato vigente em seu nome.

**"Ativo" × "vigente":** `active` é o soft delete; vigente é `active = true` **e**
`endDate >= hoje` (o último dia ainda conta). Só um contrato vigente bloqueia a
criação de outro. Um contrato ativo porém vencido não bloqueia: ele aguarda a
decisão do administrador (prorrogar via aditivo, ou encerrar e levar as sessões
restantes para o contrato seguinte — regra a detalhar junto com o saldo, em
`Consultation`). Por isso um paciente pode ter, temporariamente, mais de um
contrato ativo.

**Cadastros inativos:** paciente titular, beneficiário e técnica precisam estar
ativos para um contrato novo. A vigência exige `endDate >= startDate`.

**Versionamento (aditivos):** o contrato nunca é editado no lugar. Cada
alteração gera uma **nova versão** — novo registro com `previousContractId`
apontando para a versão atual, que é inativada (`POST /api/contracts/{id}/amendments`).
Motivo: o contrato é um compromisso assinado; cada versão será impressa para nova
assinatura, e o histórico completo precisa ser preservado (mesma lógica da
historização de preços). Regras:
- O request traz o **estado completo** da nova versão (o frontend abre o
  formulário preenchido com a versão atual). Nova versão passa pelas mesmas
  regras da criação.
- O **titular nunca muda** (é copiado): trocar o titular é um contrato novo.
- **O que for igual à versão anterior é aceito como está; o que for novo precisa
  ser válido.** Manter a mesma linha de preço é permitido mesmo após um reajuste
  (o contrato mantém o valor combinado); trocar de linha exige uma linha vigente.
  O mesmo vale para técnica e beneficiário inativos.
- Só a versão **ativa** pode ser alterada. Ativa porém vencida pode — é assim
  que se prorroga (novo `endDate`).
- `previousContractId` é `unique`: cada versão tem no máximo uma sucessora
  (impede duas alterações simultâneas da mesma versão).
- O número da versão (1, 2, 3…) não é persistido: é derivável pela cadeia.

*Impacto no `Consultation`:* atendimentos de um mesmo acordo ficam distribuídos
entre as versões da cadeia; o saldo de sessões precisa considerar a cadeia
inteira, não só a versão ativa.

**Sobre as duas FKs de preço:** o contrato trava, no momento da assinatura, a
linha de preço vigente naquele instante — mesmo que o catálogo seja reajustado
depois, o contrato mantém o valor combinado até o fim da vigência. Constraint de
banco (`CHECK`) garante que exatamente uma das duas FKs esteja preenchida
(*pendente: entra junto com as migrations do Flyway; até lá, garantida no código*).

**A linha de preço é a fonte da verdade.** Na criação, a clínica escolhe a
técnica-âncora e uma linha de preço vigente do grupo dela
(`GET /api/techniques/{id}/prices`). Tudo que a linha já contém é copiado dela,
nunca recebido do request: o `planId` sempre, e a `weeklyFrequency` quando o
preço é por frequência. Receber esses dados separados criaria redundância — o
request poderia mandar um plano ou frequência diferente do preço travado, e o
contrato nasceria incoerente. Pelo mesmo motivo, o sistema valida que a técnica
pertence ao grupo da linha de preço e que o grupo usa a tabela de onde a linha
veio (`pricingModel`).

---

## `User`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `email` | string | |
| `password` | string | |
| `role` | ENUM: `ADMIN` \| `PROVIDER` | |

**Por que separado de `Provider`:** autenticação é preocupação genérica; dado de
negócio (percentual de repasse) é específico da clínica. Misturar os dois
acoplaria o módulo de login a regras que não são dele.

---

## `Provider`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `userId` | FK → User | |
| `name` | string | |
| `commissionPercentage` | BigDecimal | uniforme, não varia por técnica |
| `active` | boolean | |

**Sobre `commissionPercentage`:** o percentual de repasse é o mesmo para
qualquer atendimento do prestador, independente da técnica. Pode mudar ao longo
do tempo com `UPDATE` simples — não precisa de historização própria, porque cada
`Consultation` já grava o valor aplicado no momento do lançamento
(`commissionPercentageApplied`).

---

## `Consultation`

| Atributo | Tipo | Observação |
|---|---|---|
| `id` | Long | auto-increment |
| `patientId` | FK → Patient | quem foi atendido (pode divergir do titular do contrato) |
| `contractId` | FK → Contract | de quem sai o saldo |
| `providerId` | FK → Provider | quem atendeu |
| `techniqueId` | FK → Technique | técnica clinicamente executada |
| `durationMinutes` | int | dado de agenda; só participa do cálculo se o grupo for `DURATION_BASED` |
| `occurredAt` | date | (não usar `date` como nome de campo — ambíguo com o tipo em alguns parsers) |
| `status` | ENUM: `ATTENDED` \| `CANCELED` \| `UNSCHEDULED_WITH_NOTICE` \| `UNSCHEDULED_WITH_CHARGE` \| `MISSED` | |
| `sessionDurationPriceId` | FK → SessionDurationPrice, nullable | exclusive arc |
| `sessionFrequencyPriceId` | FK → SessionFrequencyPrice, nullable | exclusive arc |
| `commissionPercentageApplied` | BigDecimal | snapshot |
| `baseValue` | BigDecimal | snapshot |
| `repasseValue` | BigDecimal | snapshot |

**Por que `patientId` e `contractId` são dois campos separados:** no Pilates em
Dupla, quando o titular não comparece, o prestador pode lançar o atendimento no
nome do acompanhante — quem foi atendido (`patientId`) e de quem sai o saldo
(`contractId`, e por ele, o titular) são pessoas diferentes.

**Por que `techniqueId` é campo próprio, mesmo já existindo a FK de preço:** a
linha de preço aponta para o *grupo*, não para a técnica específica. Sem esse
campo, perderíamos a granularidade necessária para relatórios como "atendimentos
de Quiropraxia este mês".

**Por que os três valores (`baseValue`, `commissionPercentageApplied`,
`repasseValue`) são sempre gravados, nunca só derivados:** eles representam uma
obrigação financeira já consumada, que entra direto nos honorários do prestador
assim que lançada (sem fluxo de aprovação). Um reajuste futuro no catálogo de
preço, ou uma mudança na comissão do prestador, nunca deve alterar
retroativamente o valor de um atendimento já lançado.

**Por que `status` não tem uma coluna extra de "conta para o repasse":** é uma
regra fixa por valor (`ATTENDED`, `NO_SHOW` e `CANCELLED_WITH_CHARGE` contam;
`CANCELLED_EARLY` não conta), então vive como método no enum
(`status.countsTowardsBilling()`), não como dado replicado no banco.

### Lógica de resolução de preço no lançamento

1. Prestador seleciona paciente, duração, técnica e status.
2. Sistema compara `technique.pricingGroupId` do atendimento com o do contrato
   do paciente.
   - **Mesmo grupo:** usa o `planId` do próprio `Contract` automaticamente.
   - **Grupo diferente:** abre um modal para o prestador escolher manualmente o
     plano equivalente no grupo da nova técnica (ex: Avulso, 6 sessões, 12
     sessões, para Quiropraxia). *Decisão atual: escolha livre do prestador, sem
     validação da clínica — trava/validação é melhoria futura.*
3. Busca o preço vigente na tabela correta (`SessionDurationPrice` se
   `DURATION_BASED`, `SessionFrequencyPrice` se `FREQUENCY_BASED`).
4. Grava o snapshot: `baseValue`, `commissionPercentageApplied`, `repasseValue`.

**Nota:** o preço travado em `Contract` é uma referência de cobrança, não
necessariamente o valor de toda sessão futura — se a duração real ou a técnica
divergirem do que o contrato ancora, o valor daquele atendimento específico é
resolvido de novo, seguindo o fluxo acima.

---

## Não modelado ainda (fora do escopo da Fase 1)

- **`Payment`** — controle financeiro/saldo do paciente (pagamentos realizados x
  valor consumido nos atendimentos `countsTowardsBilling`). Alimenta a visão da
  clínica na Fase 2.
- App de consulta de saldo do paciente (Fase 3) — camada de consumo somente
  leitura sobre `consultation` e `payment`, sem entidades novas.
