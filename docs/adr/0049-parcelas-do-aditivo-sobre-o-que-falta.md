# 0049 — As parcelas do aditivo sobre o que falta pagar, até os meses que faltam

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

No aditivo, as parcelas dividiam o total da nova versão, de 1 até os meses da nova vigência
([ADR 0037](0037-parcelas-escolhidas-no-aditivo.md)), porque o sistema ainda não sabia o que tinha
sido pago. Com os pagamentos, as duas regras passaram a dar errado:
- dividir o total faria o paciente pagar de novo o que já pagou;
- num contrato de 12 meses com aditivo no mês 10, o máximo seriam 12 parcelas, vencendo até o mês
  22, muito depois do fim.

## Decisão

- **As parcelas do aditivo dividem o que falta pagar:** o total da nova versão − o pago da cadeia
  até o aditivo (o `installmentBase`, gravado, [ADR 0048](0048-valor-parcelado-gravado-por-versao.md)).
- **O pago que cobre o total:** o `installmentBase` tem mínimo 0; com 0, a versão grava 1 parcela
  de R$ 0,00 (já paga), e o que passar do total fica como saldo credor na conta.
- **O máximo de parcelas são os meses que faltam:** do novo 1º vencimento até o fim do contrato, no
  mínimo 1, pela mesma conta de meses da criação.
- **O que conta como alteração nas parcelas** (o aditivo sem alteração continua recusado, 400
  "Nenhuma alteração no contrato."): mudar o dia do vencimento, ou escolher um número de parcelas
  diferente das que ainda faltam vencer na versão anterior (contadas pela data, sem depender dos
  pagamentos). Ex: 3 parcelas no dia 10; um aditivo depois da 1ª, mantendo o dia 10 e as 2 que
  faltam, não é alteração.
- O pago chega ao aditivo pelo `PaidAmountProvider` ([ADR 0050](0050-paid-amount-provider.md)). A
  prévia do aditivo mostra o pago até agora, o valor parcelado, o máximo e o 1º vencimento sugerido.

## Alternativas descartadas

- **Dividir o total da nova versão** (a regra anterior): cobra de novo o que já foi pago.
- **O máximo pelos meses da vigência inteira:** as parcelas passariam do fim do contrato.
- **Comparar as datas de vencimento para o "sem alteração":** todo aditivo tem um 1º vencimento a
  partir da data dele, sempre diferente do antigo; todo aditivo contaria como alteração.

## Consequências

- Substitui em parte o [ADR 0037](0037-parcelas-escolhidas-no-aditivo.md): a linha "as parcelas
  dividem o total da nova versão" e o máximo "até os meses da nova vigência". As parcelas continuam
  escolhidas em todo aditivo.
- Os testes do aditivo passaram a calcular o máximo pelos meses que faltam.
