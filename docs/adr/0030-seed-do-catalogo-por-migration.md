# 0030 — Seed do catálogo de preços por migration

- **Status:** aceito
- **Data:** 2026-11-11 (registro retroativo; a migration `V4` é de 2026-11-06)

## Contexto

O [ADR 0021](0021-schema-versionado-flyway.md) decidiu que o catálogo da clínica não entraria
nas migrations: os dados seriam cadastrados pela API, passando pelas regras do domínio. Assim,
cada banco novo começava vazio e precisava do catálogo inteiro cadastrado à mão (4 grupos, 8
técnicas, 14 planos, o de-para e 62 preços) antes de qualquer contrato ou lançamento. A
migration `V4__seed_pricing_catalog.sql` passou a carregar esse catálogo, sem que a decisão
fosse registrada.

## Decisão

- **O catálogo atual da clínica entra pela migration `V4`:** grupos, técnicas, planos, de-para
  e preços vigentes (a tabela "Catálogo atual da clínica" no
  [`development.md`](../development.md)).
- **Os dados respeitam as regras que a API aplicaria:** tipo do plano × `sessionCount` /
  `termWeeks`, de-para entre as duas famílias, tipo do plano × modelo do grupo e um preço
  vigente por combinação.
- **Idempotente:** só entra o que ainda não existe (nome do grupo, da técnica e do plano;
  de-para vazio; combinação sem linha vigente). Nada que a clínica já cadastrou é sobrescrito.
- **Depois do seed, o catálogo muda só pela aplicação** (reajuste pela tela de preço). A `V4`
  não é editada para refletir reajustes.
- Substitui, do ADR 0021, o ponto "o catálogo da clínica não entra nas migrations".

## Alternativas descartadas

- **Cadastrar o catálogo pela API a cada banco novo** (o que o ADR 0021 previa): dezenas de
  chamadas manuais, repetidas toda vez que o banco local é zerado.
- **`ApplicationRunner`, como o `AdminInitializer`:** o admin é um problema de bootstrap (sem
  ele ninguém faz login); o catálogo não é. Como migration, são dados de referência
  versionados junto com o schema, aplicados uma única vez e registrados no
  `flyway_schema_history`.

## Consequências

- As regras do domínio não passam pelo código no seed: quem mexer na `V4` confere à mão (o
  `validate` do Hibernate confere o schema, não os dados).
- O banco zerado volta com o catálogo sem passo manual. Com as migrations editáveis do
  [ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md), o time zera o banco local com
  frequência.
- O preço da Evolução Familiar no Individual Especializado e no Em Dupla, ainda não definido
  pela clínica, fica fora do seed e é cadastrado pela tela de preço.
