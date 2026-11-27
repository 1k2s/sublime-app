# 0049 — O status do contrato terminado pelo saldo, e o crédito transferido ao contrato novo

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

Com a conta do paciente pelo consumo ([ADR 0042](0042-conta-do-paciente-pelo-consumo.md)), um
contrato que terminou pode ficar com saldo: devendo (usou mais do que pagou) ou com crédito (pagou
mais do que usou). O status "Inativo" não dizia isso: o contrato que terminou quitado e o que
terminou com pendência pareciam iguais, e o inativado à mão também era "Inativo". E o crédito
ficava parado no contrato antigo, sem um jeito de usá-lo no contrato seguinte.

Dois problemas de módulo: o status depende do saldo, que é do `billing`, mas é do `contract` (a
lista e o detalhe de contratos o mostram); e o cadastro do contrato, no `contract`, precisa saber o
crédito disponível e registrar o uso dele, também do `billing`.

## Decisão

- **O status do contrato é calculado pelo backend**, na leitura: **Vigente** (ativo, na vigência);
  **Encerrado** (passou do fim com o saldo zerado); **Vencido** (passou do fim com pendência: dívida
  ou crédito); **Cancelado** (inativado à mão: o antigo "Inativo"); **Substituída** (a versão que um
  aditivo substituiu). Nada é gravado: o status muda sozinho quando um pagamento ou uma
  transferência zera o saldo.
- **O crédito passa para o contrato novo como uma transferência gravada** (`BalanceTransfer`, tabela
  `balance_transfer`): no cadastro, com a caixa marcada, o crédito dos contratos não vigentes do
  titular entra **inteiro**, do mais antigo para o mais novo, até o total do novo. Cada
  transferência sai do saldo do antigo e entra no novo como pago (não é faturamento: não entrou
  dinheiro). O que não cabe continua no antigo. Imutável, como o lançamento.
- **O `contract` declara o `ContractBalanceProvider`** (os saldos para o status, o crédito disponível
  e a transferência), e o **`billing` a implementa**: o segundo caso da inversão de dependência do
  [ADR 0048](0048-paid-amount-provider.md).
- As parcelas em aberto de um contrato que terminou ficam **"não cobradas"**: a conta dele vale só
  pelo saldo (sem isso, quem terminou com crédito por ter usado pouco ficaria "Atrasado").

## Alternativas descartadas

- **Só reduzir o valor parcelado do contrato novo**, sem gravar a transferência: o contrato antigo
  continuaria com o crédito na conta (seria oferecido de novo), e o histórico de para onde o
  crédito foi se perderia.
- **Um evento do Spring no lugar da interface** (o `contract` publica "contrato criado com
  crédito" e o `billing` grava): o cadastro precisa do valor do crédito antes de gravar (o valor
  parcelado depende dele) e da transferência na mesma transação; um evento não devolve valor, e um
  erro no ouvinte ficaria longe de quem cadastrou.
- **O status gravado no contrato** (atualizado a cada pagamento, lançamento ou transferência):
  cada mudança no `billing` ou no `consultation` teria de lembrar de atualizar o `contract`; um
  esquecimento deixaria o status errado, sem como perceber.

## Consequências

- O status sai com a lista e o detalhe de contratos; os contratos terminados pedem os saldos ao
  `billing` de uma vez, para a lista não ter N+1 (o teste do número de consultas confere).
- O crédito transferido aparece nas duas contas ("Crédito aplicado" no histórico de pagamentos) e
  não desfaz as parcelas pagas do contrato antigo.
- Cancelar o contrato novo não devolve o crédito ao antigo: ele fica no novo como saldo credor e
  volta a estar disponível para o próximo contrato.
- O avulso fica de fora do crédito: a clínica resolve com um pagamento de "Outro valor".
