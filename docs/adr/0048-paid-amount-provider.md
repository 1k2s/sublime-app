# 0048 — O `contract` declara o `PaidAmountProvider`, o `billing` implementa

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

O aditivo precisa do que já foi pago para dividir o que falta
([ADR 0047](0047-parcelas-do-aditivo-sobre-o-que-falta.md)). O aditivo é do `contract`; os
pagamentos são do `billing`, que fica **acima** do `contract` no mapa de módulos (o `billing` lê o
contrato). O `contract` chamar o `billing` faria a dependência apontar para cima e fecharia um
ciclo entre os dois.

## Decisão

- **O `contract` declara uma interface**, `PaidAmountProvider` (`paidOn(contractChainIds)`: o bruto
  dos pagamentos ativos da cadeia de versões), e o **`billing` a implementa**
  (`BillingPaidAmountProvider`). O Spring injeta a implementação no `ContractService`.
- O aditivo e a prévia dele continuam no `contract`; a dependência continua apontando para cima.
- **Primeiro caso de inversão de dependência no projeto:** o padrão para quando um módulo de baixo
  precisa de um dado de um módulo de cima.

## Alternativas descartadas

- **O aditivo no `billing`:** o fluxo do contrato sairia do dono do contrato, e o `billing`
  repetiria as regras do aditivo.
- **O pago vindo do front:** o backend é a fonte da verdade; o pago mandado pela tela poderia ser
  diferente do gravado.

## Consequências

- O `contract` compila sem o `billing`, mas não sobe sem uma implementação da interface (o Spring
  não teria o que injetar): as duas partes entram juntas.
- Nos testes de unidade do contrato, o pago é passado direto (`Money`), sem o provider.
