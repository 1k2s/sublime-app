# 0006 — Prazo dos planos do Grupo em semanas (`termWeeks`)

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Um contrato só com Pilates em Grupo não tem sessões para calcular a vigência
(mensalidade não tem `sessionCount`). Num contrato misto, o prazo do plano do Grupo
precisa ser comparado com a vigência dos itens por duração, que é calculada em
semanas (ver [ADR 0009](0009-vigencia-calculada.md)).

## Decisão

O plano `MONTHLY_FEE` guarda o prazo em semanas, em `termWeeks`: Mensal 4,
Trimestral 13, Semestral 26, Anual 52 (Familiares iguais ao plano base). Nos demais
tipos fica `null`.

## Alternativas descartadas

- **Prazo em meses:** comparar meses com as semanas dos itens gera diferença de
  dias (52 semanas = 364 dias, contra 365 de 12 meses).
- **Derivar o prazo pelo de-para** (Trimestral = prazo do Evolução semanal): o
  prazo do Grupo viraria consequência das sessões de outro plano. Se o Evolução
  mudasse de sessões, o Trimestral mudaria junto sem ninguém mexer nele.

## Consequências

- Trimestral = 13 (não 12): mantém a proporção interna do Grupo (13 × 2 = 26,
  13 × 4 = 52). Mensal = 4 bate com "Mensal 1x/semana = 4 aulas".
- Os prazos ficam um pouco menores que os "meses" da tabela comercial.
- O `termWeeks` também limita as opções do plano do item do Grupo (ver
  [ADR 0010](0010-plano-do-item-do-grupo.md)).
