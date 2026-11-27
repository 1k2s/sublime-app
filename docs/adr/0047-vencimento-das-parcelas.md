# 0047 — O vencimento das parcelas e a data da inativação

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

As parcelas do contrato não tinham vencimento ([ADR 0032](0032-valor-e-parcelas-do-contrato.md)):
sem ele, a conta do paciente ([ADR 0044](0044-conta-do-paciente-pelo-consumo.md)) não tem como
dizer o que está atrasado. A clínica combina com o paciente a melhor data de pagamento. E um
contrato inativado no meio (o paciente desistiu) continuaria com parcelas vencendo e ficando
atrasadas.

## Decisão

- **O 1º vencimento é gravado no contrato** (`firstDueDate`), por versão. **Opcional no pedido:**
  sem ele, o início do contrato (no cadastro) ou o sugerido (no aditivo).
  - **No cadastro:** do início do contrato até 1 mês depois.
  - **No aditivo:** a partir da data do aditivo (nenhuma parcela nasce vencida). O sugerido: o dia
    de vencimento no mês do aditivo; se a parcela desse mês está paga, ou se o dia já passou, o mesmo
    dia no mês seguinte.
- **Cada vencimento sai do 1º:** o 1º vencimento + N meses, e não o anterior + 1 mês. Nos dias 29 a
  31, no mês mais curto a parcela vence no último dia (como os bancos), e o dia não "escorrega":
  31/01 → 28/02 → 31/03.
- **A inativação tem data** (`deactivatedOn`): o dia da inativação ou, na versão substituída por
  aditivo, o `createdAt` da sucessora. Depois dela nada vence: as parcelas ficam "canceladas", não
  atrasadas, e o Grupo para de somar mensalidades. A conta do contrato passa a valer só pelo saldo.

## Alternativas descartadas

- **Cada vencimento a partir do anterior:** depois de um fevereiro, o dia 31 viraria 28 para
  sempre.
- **`firstDueDate` obrigatório:** o backend entra na `dev` antes das telas, e o cadastro pela tela
  antiga daria 400 até o PR do front. Com o padrão, o pedido antigo continua válido.
- **Inativar sem data:** um contrato de 12 meses inativado no mês 4 ficaria com as parcelas de 5 a
  12 vencendo e marcando "Atrasado".

## Consequências

- Duas colunas novas em `contract`: `first_due_date` (`NOT NULL`) e `deactivated_on`.
- A resposta do contrato traz o `firstDueDate`, o `deactivatedOn` e o cronograma
  (`installmentSchedule`: o número, o vencimento e o valor de cada parcela, sem a situação, que é do
  `billing`).
