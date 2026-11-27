# 0046 — O pagamento: bruto e líquido, e o cartão parcelado como um pagamento só

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

No cartão, a clínica recebe menos do que o paciente paga (a taxa da maquininha), e no crédito
parcelado recebe aos poucos, mês a mês. O paciente, por sua vez, quitou o valor inteiro na hora:
para ele, as parcelas do contrato estão pagas. A conta do paciente e o faturamento da clínica
precisam de valores diferentes do mesmo pagamento.

## Decisão

- **O pagamento grava o bruto e o líquido:** o bruto (`amount`, o que o paciente pagou, maior que
  zero) e o líquido (`netAmount`, o que a clínica recebe, de 0 ao bruto). **A conta do paciente usa
  o bruto** (a taxa é custo da clínica, não dívida do paciente); o líquido serve ao faturamento.
  O admin digita o líquido, que vem preenchido com o bruto; a taxa (valor e %) é calculada.
- **O cartão parcelado é um pagamento só**, com o número de parcelas do cartão
  (`cardInstallments`, de 1 a 12, maior que 1 só no crédito). Uma passada na maquininha é uma
  transação: um registro, inativado de uma vez. Para o paciente, o bruto inteiro conta na data do
  pagamento.
- **A previsão de recebimento é calculada, não gravada** (`Receivables`): Pix, dinheiro e débito
  na data do pagamento; o crédito em N vezes, N partes do líquido, na data + 1, + 2… meses, com os
  centavos na primeira.
- **O pagamento nunca é editado:** para corrigir, inativar e registrar de novo, como o lançamento
  ([ADR 0020](0020-lancamento-imutavel.md)). Inativar é corrigir um registro errado, não estornar.
- **As formas aceitas:** Pix, cartão de crédito, cartão de débito e dinheiro. A forma que conta é
  a do pagamento; a do contrato é só a referência do combinado.

## Alternativas descartadas

- **A taxa cadastrada por forma de pagamento:** a real muda com a bandeira, as parcelas e a
  maquininha; pode entrar depois só como sugestão do líquido.
- **N pagamentos para o crédito em N vezes:** corrigir exigiria inativar todos, e o histórico
  mostraria N pagamentos de uma transação.
- **Só o bruto:** o faturamento da clínica sairia errado no cartão.

## Consequências

- O boleto (`BANK_SLIP`, que o enum já tem) é recusado no pagamento e entra com um gateway.
- O estorno (a clínica devolver o que recebeu) é outro movimento, do encerramento: feito por
  inativação, o faturamento daquele mês mudaria.
- A antecipação de recebíveis do cartão fica fora.
