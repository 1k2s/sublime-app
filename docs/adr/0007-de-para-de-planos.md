# 0007 — De-para de planos nos dois lados

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Os grupos por duração usam pacotes (Essencial, Evolução...) e o Pilates em Grupo
usa mensalidades (Mensal, Trimestral...). O sistema precisa saber qual plano de
uma família corresponde ao da outra:
- no contrato misto, para sugerir o plano do item do Grupo;
- no lançamento de atendimento fora do grupo contratado (spec do `consultation`),
  nos dois sentidos: paciente de pacote numa aula do Grupo (Evolução → Trimestral)
  e paciente de contrato só com Grupo numa sessão por duração (Trimestral →
  Evolução). Esse uso substitui um modal de escolha livre do plano.

A correspondência não é simétrica: o Evolução **e** o Evolução Familiar
correspondem ao Trimestral, mas o Trimestral volta só para o Evolução.

## Decisão

- `Plan.equivalentPlanId`, preenchido **nos dois lados**: cada plano aponta para o
  seu equivalente na outra família, independente da frequência.
- O equivalente precisa ser da **outra família** (`PACKAGE` ⇄ `MONTHLY_FEE`), estar
  ativo e não ser o próprio plano (400). `SINGLE_SESSION` não tem equivalente.
- Definido depois da criação, em endpoint próprio
  (`PUT /api/plans/{id}/equivalent`), porque os dois planos precisam existir antes.

## Alternativas descartadas

- **FK só de um lado** (só nos pacotes): "qual é o equivalente do Trimestral?"
  teria duas respostas (Evolução e Evolução Familiar).
- **Escolha livre do plano equivalente pelo usuário:** o preço do atendimento
  dependeria de quem lança; com o de-para, o sistema resolve sozinho.

## Consequências

- O cadastro do catálogo precisa de um passo a mais: definir o de-para dos dois
  lados depois de criar os planos.
- No contrato misto, o de-para é só **sugestão**: as opções válidas seguem o
  [ADR 0010](0010-plano-do-item-do-grupo.md).
