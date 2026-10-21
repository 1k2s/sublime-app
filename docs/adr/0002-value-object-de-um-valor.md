# 0002 — Value object de um valor: `record` + `AttributeConverter`, sem arredondar

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Valores em dinheiro e percentuais apareciam como `BigDecimal` solto, com as regras
(não negativo, no máximo 2 casas, 0–100%) espalhadas pelos services e entidades. O
projeto já usava `@Embeddable` para o `Address`, um VO com vários campos.

## Decisão

- **VO de um valor só é `record` + `AttributeConverter` com `autoApply = true`:**
  `Money` + `MoneyConverter` (módulo `pricing`) e `Percentage` +
  `PercentageConverter` (módulo `provider`).
- O converter traduz o VO para a coluna e de volta, e cria o VO pela fábrica (ex:
  `Money.of`). O Hibernate nunca instancia o VO, então ele fica livre de JPA,
  imutável e com o construtor validado como única porta de entrada.
- **Mais de 2 casas decimais é rejeitado (400), nunca arredondado.** A escala é
  sempre normalizada em 2 (`220` → `220.00`).
- O VO fica no módulo dono do conceito (comissão é do prestador; dinheiro, da
  precificação), sem criar dependência nova entre módulos.
- DTOs continuam com `BigDecimal` (formato do JSON); o service monta o VO.

## Alternativas descartadas

- **`@Embeddable` como `class`:** exige construtor sem argumentos e campos não
  `final`, preenchidos por reflexão. Fica para VO com vários campos (`Address`),
  onde cada campo vira uma coluna.
- **`BigDecimal` solto:** a regra do valor teria que ser repetida em cada lugar
  que o usa.
- **Arredondar o valor digitado:** esconderia um valor digitado errado (ex:
  `220.555`).

## Consequências

- A coluna mantém nome e precisão do `@Column` de cada campo, sem
  `@AttributeOverride`.
- Formatação de exibição (`R$ 1.234,56`) fica no frontend; a API devolve número.
- Uma **conta** que precise arredondar (ex: aplicar um percentual) faz isso
  explicitamente no método da conta. A regra "nunca arredonda" vale para valor
  **digitado**.
