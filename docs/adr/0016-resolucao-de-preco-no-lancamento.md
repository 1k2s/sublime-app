# 0016 — Resolução de preço no lançamento pelo sistema

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

O valor de um atendimento vira obrigação de repasse na hora, sem aprovação. O
paciente pode ser atendido numa técnica do grupo contratado (em outra duração, até),
de um grupo não contratado, ou não ter contrato. O contrato trava o preço do que foi
contratado ([ADR 0003](0003-preco-historizado.md), [ADR 0008](0008-contrato-com-itens.md)).

## Decisão

O sistema resolve a linha de preço; **não há escolha manual de plano ou preço**.

1. **O contrato cobre o grupo:** linha travada do item na mesma duração; em outra
   duração, a linha **atual** do plano do contrato. Debita o item.
2. **O contrato não cobre o grupo:** **de-para** ([ADR 0007](0007-de-para-de-planos.md))
   a partir do plano do contrato, linha **atual**. No Pilates em Grupo, sempre o
   preço de 1x por semana (aula experimental). Não debita.
3. **Sem contrato:** linha **atual** do Avulso, o único plano `SINGLE_SESSION` ativo
   (409 no `pricing` ao criar um segundo). O Pilates em Grupo não tem avulso: 400.

"Atual" = vigente hoje, mesmo em lançamento retroativo. Sem linha correspondente →
400, nunca um preço aproximado.

## Alternativas descartadas

- **Modal de escolha livre do plano equivalente:** o preço dependeria de quem lança.
- **Preço travado para qualquer técnica e duração:** o contrato combinou um preço
  para o que foi contratado; o resto paga o preço atual.
- **Tabela de preço por ano para achar o preço "da época" do contrato:** ver
  [ADR 0003](0003-preco-historizado.md).
- **Mais de um plano avulso:** a busca do caso 3 ficaria ambígua.

## Consequências

- O de-para precisa estar cadastrado nos dois lados; plano sem equivalente → 400.
- Cada grupo por duração precisa de linha do Avulso em cada duração atendida.
- A regra do de-para mora no plano (`PlanEntity.planFor(PricingModel)`).
