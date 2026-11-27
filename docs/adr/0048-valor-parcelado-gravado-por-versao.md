# 0048 — O valor parcelado gravado por versão

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

O contrato gravava só o número de parcelas; o valor delas saía do total, calculado a cada leitura
pelas linhas travadas ([ADR 0032](0032-valor-e-parcelas-do-contrato.md)). Com os pagamentos, o
aditivo passou a dividir o que falta pagar, e não o total
([ADR 0049](0049-parcelas-do-aditivo-sobre-o-que-falta.md)). Se esse valor fosse recalculado na
leitura pelo pago, as parcelas assinadas mudariam sozinhas: um pagamento com data retroativa, ou um
pagamento inativado depois do aditivo, alteraria o valor de parcelas já combinadas.

## Decisão

- **O valor parcelado é gravado em cada versão** (`installmentBase`): o total, no cadastro; o total
  − o pago até então, no aditivo (no mínimo 0). Depois, não muda. As parcelas dividem esse valor,
  com os centavos na primeira.
- **O pago que vale para as parcelas de uma versão** é o pago da cadeia de versões − o pago antes
  dela (o total da versão − o `installmentBase`), **limitado entre 0 e o `installmentBase`**,
  distribuído pela ordem de vencimento. Um pagamento retroativo ou inativado nunca deixa uma parcela
  negativa nem acima de paga: o que passar fica no saldo da conta.

## Alternativas descartadas

- **Recalcular as parcelas na leitura pelo pago:** o valor das parcelas assinadas mudaria a cada
  pagamento registrado ou inativado.

## Consequências

- Substitui em parte o [ADR 0032](0032-valor-e-parcelas-do-contrato.md): só a linha "só o número de
  parcelas é gravado".
- Coluna nova em `contract`: `installment_base` (`NOT NULL`, `CHECK >= 0`).
- **Inativar um pagamento de antes do aditivo faz a dívida reaparecer na versão atual**
  (comportamento esperado): o `installmentBase` não muda, e o pago dela diminui. Ex: o aditivo
  dividiu R$ 2.338,00 (3.006 − 668 pagos); se o pagamento de R$ 668,00 é inativado, as parcelas
  continuam somando R$ 2.338,00, e os R$ 668,00 voltam a faltar no saldo da conta.
- O pago antes do aditivo (o total − o `installmentBase`) é mostrado em cada versão, na aba
  Parcelas do Financeiro.
