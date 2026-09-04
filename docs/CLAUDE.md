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
patient   pricing   user       ← módulos de base, sem dependência entre si
   \         /        |
    contract       provider    ← dependem só da camada de base
        \            /
         consultation          ← depende de todos os anteriores
```

- `patient`: cadastro de pacientes (`Patient`).
- `pricing`: técnicas, planos e catálogo de preços (`Technique`, `Plan`,
  `PricingGroup`, `GroupPlanPrice`, `GroupPlanFrequencyPrice`). Ficam juntos porque
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

## Princípios ordem modelagem já fixados (não reabrir sem justificativa forte)

1. **Value Objects embutidos, não entidades**, para dados sem identidade própria
   (ex: `Address` dentro de `Patient`, via `@Embeddable`/`@Embedded`).
2. **Nunca persistir dado derivável que serve só para exibição** (ex: percentual de
   desconto). Sempre persistir dado que já representa uma obrigação financeira
   consumada (ex: valor de um atendimento já lançado).
3. **Historização por `validFrom`/`validTo`, nunca `UPDATE` direto**, em qualquer
   tabela de preço. Reajuste sempre fecha a linha antiga e abre uma nova.
4. **"Exclusive arc"**: quando uma entidade pode referenciar uma de duas tabelas
   diferentes (nunca as duas), usar duas FKs nullable + `CHECK` constraint
   garantindo exatamente uma preenchida. Usado em `Contract` e `Consultation` para
   `groupPlanPriceId` / `groupPlanFrequencyPriceId`.
5. **Snapshot de valores financeiros no momento do lançamento.** `Consultation`
   grava `baseValue`, `commissionPercentageApplied` e `repasseValue` mesmo sendo
   tecnicamente deriváveis, porque são obrigação de pagamento já consumada — nunca
   devem mudar retroativamente se o catálogo de preço ou a comissão do prestador
   mudar depois.
6. **Regra fixa por valor de enum vira método no enum**, não coluna extra no banco
   (ex: `AppointmentStatus/ConsultationStatus.countsTowardsBilling()`).

## Estado atual (Fase 1 em andamento)

Modelagem de domínio concluída para: `Patient`, `Technique`, `Plan`,
`PricingGroup`, `GroupPlanPrice`, `GroupPlanFrequencyPrice`, `Contract`, `User`,
`Provider`, `Consultation`. Ver `docs/domain-model.md` para atributos completos.

Pendente, ainda não modelado: domínio de `Payment`/saldo do paciente (Fase 2, visão
da clínica cruzando pagamentos com atendimentos).

## Ordem de implementação recomendada

1. `patient`, `pricing`, `user` — entidades e repositórios, pode popular via
   fixtures/`data.sql` em vez de tela de cadastro completa.
2. `provider` (depende de `user`) e `contract` (depende de `patient` + `pricing`).
3. `consultation` — aqui sim com atenção total, é o módulo com a lógica de negócio
   mais densa e o alvo do frontend de teste desta fase.
