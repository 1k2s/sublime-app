# 0019 — Repasse arredondado com `HALF_UP`

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

O repasse é o valor da sessão × percentual do prestador, e o resultado pode ter mais
de 2 casas (35% de R$ 58,91 = R$ 20,6185). O `Money` rejeita mais de 2 casas e nunca
arredonda valor digitado ([ADR 0002](0002-value-object-de-um-valor.md)). O
`Percentage` mora no `provider` e o `Money` no `pricing`.

## Decisão

- **Repasse = valor × percentual ÷ 100, arredondado para 2 casas com `HALF_UP`**
  (5 ou mais sobe): R$ 20,6185 → R$ 20,62.
- `Percentage.applyTo(BigDecimal)` faz a conta com o arredondamento explícito; o
  `Consultation` embrulha o resultado em `Money`.

## Alternativas descartadas

- **`HALF_EVEN`** (arredondamento bancário): difícil de explicar ao prestador.
- **Sempre para baixo:** favorece a clínica em centavos e vira reclamação.
- **`Percentage.applyTo(Money)`:** o `provider` passaria a depender do `pricing`,
  uma seta nova no mapa de módulos por causa de um método.

## Consequências

- É o arredondamento de planilha e calculadora: o prestador consegue conferir o
  próprio repasse.
- Não conflita com "o `Money` nunca arredonda": aquela regra vale para valor
  digitado; aqui é uma conta.
