# 0042 — A conta do paciente pelo consumo, com as parcelas como referência

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

A clínica controlava à mão o que cada paciente usou e pagou. O contrato já tinha o total e as
parcelas ([ADR 0032](0032-valor-e-parcelas-do-contrato.md)), e cada atendimento ativo grava o
valor da sessão (`baseValue`) no lançamento. Faltava saber o que o paciente pagou e comparar.

O paciente não usa o contrato no ritmo das parcelas: quem vai mais que o contratado termina
devendo, quem desmarca com antecedência termina com crédito. Ex: RPG 60 min, 12 × R$ 167,00 =
R$ 2.004,00 em 3 parcelas de R$ 668,00; com 6 sessões no 2º mês, o consumido chega a R$ 1.670,00
com R$ 1.336,00 pagos: deve R$ 334,00 já no 2º mês, não só no fim.

## Decisão

- **A dívida é o consumo:** os atendimentos ativos × `baseValue`, nos itens por duração e no
  de-para (o atendimento pela técnica fora do contrato, que grava o contrato de origem), comparados
  com os pagamentos. **Saldo = pago − devido** (positivo = saldo credor; negativo = devedor).
- **O pagamento é de valor livre**, ligado ao contrato (a qualquer versão da cadeia) ou, no
  avulso, sem contrato. **As parcelas são a referência** do que se espera receber: têm vencimento e
  situação (paga, atrasada, próxima), mas não são a dívida.
- **A conta é calculada na leitura** (`ContractAccount`, `SingleSessionAccount`,
  `PatientAccountSummary`, no `billing`), nunca gravada: muda sozinha com cada lançamento,
  inativação e pagamento.
- **Contrato e avulso são contas separadas:** o crédito de uma não passa para a outra. Na lista do
  Financeiro, o saldo é a soma; a situação é avaliada conta a conta (atrasado → devendo → com
  crédito → em dia).

## Alternativas descartadas

- **As parcelas como a dívida** ("parcela 2 de 3 paga"): esconde o consumo. O paciente que foi
  mais apareceria em dia até a última parcela; o que desmarcou, devendo.
- **O pagamento ligado a uma parcela:** o pagamento real não acompanha as parcelas (paga parte,
  adianta, junta duas); ligar a uma parcela obrigaria a dividir ou a recusar o valor.
- **Gravar o saldo:** teria de ser atualizado em cada lançamento, inativação e pagamento, em três
  módulos; um saldo gravado poderia ficar diferente da soma que ele resume.

## Consequências

- Módulo novo, `billing`, acima do `consultation` no mapa de módulos: lê o `contract` e o
  `consultation`, e nenhum dos dois o conhece.
- A lista do Financeiro calcula todas as contas numa leitura: as somas são em Java, sobre poucas
  consultas agrupadas (no máximo 4, qualquer que seja o número de pacientes), sem N+1.
- O dashboard (a Produção, numa branch própria) segue a mesma conta: o consumo pela data do
  atendimento, para a conta e o dashboard nunca darem números diferentes.
- O destino do saldo no fim do contrato (devolver, renovar, cobrar) fica com o encerramento, fora
  desta decisão.
