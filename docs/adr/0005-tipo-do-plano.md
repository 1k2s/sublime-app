# 0005 — Tipo do plano (`PlanType`)

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Existem três tipos de plano com regras diferentes: o Avulso (sessão única, só para
quem é atendido sem contrato), os pacotes de sessões (Essencial, Evolução...) e as
mensalidades do Pilates em Grupo (Mensal, Trimestral...). O plano tinha
`sessionCount` e `termWeeks` como dois campos nullable sem regra entre eles, e o
Avulso precisava ser reconhecido para ser recusado no contrato.

## Decisão

Enum `PlanType`, fixo após a criação: `SINGLE_SESSION`, `PACKAGE`, `MONTHLY_FEE`.

- **Coerência é invariante do plano** (400): `PACKAGE` exige `sessionCount`,
  `MONTHLY_FEE` exige `termWeeks`, `SINGLE_SESSION` tem exatamente 1 sessão, e
  nenhum tipo aceita o campo do outro.
- **As regras perguntam o tipo, nunca o nome**, por métodos do enum (princípio 6):
  linha por duração exige `SINGLE_SESSION` ou `PACKAGE`; por frequência,
  `MONTHLY_FEE`; contrato recusa `SINGLE_SESSION`.

## Alternativas descartadas

- **Reconhecer o Avulso pelo nome:** frágil, porque renomear o plano quebraria a
  regra.
- **Booleano "pode entrar em contrato":** resolveria só o Avulso. O tipo também
  organiza `sessionCount` × `termWeeks`.

## Consequências

- O tipo também decide a família do de-para (`PACKAGE` ⇄ `MONTHLY_FEE`; ver
  [ADR 0007](0007-de-para-de-planos.md)).
- O plano não sabe em qual grupo é usado (a ligação é a linha de preço), então a
  regra "tipo do plano × tabela" é validada na criação da linha de preço.
