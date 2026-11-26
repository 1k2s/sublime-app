# 0041 — Aulas do Pilates em Grupo pela vigência do contrato

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

O valor do item do Pilates em Grupo era as **semanas do plano do Grupo** × vezes por semana × o
valor da aula ([ADR 0032](0032-valor-e-parcelas-do-contrato.md)). Mas o Grupo **acompanha a
vigência do contrato e termina junto** ([ADR 0009](0009-vigencia-calculada.md)): ele não entra
no cálculo do fim. No contrato misto, as duas regras se contradiziam.

Ex: Evolução, RPG 2x/semana = 12 sessões em **6 semanas**; o Grupo sugerido pelo de-para é o
Trimestral ([ADR 0010](0010-plano-do-item-do-grupo.md)). Com o Trimestral 2x, o item valia
13 × 2 = **26 aulas** (R$ 1.711,06), num contrato de 6 semanas, em que cabem só 12: o paciente
pagava 14 aulas que não podia fazer.

Para a clínica, **o valor da aula é o que manda**: o plano é só a condição (a frequência e o
preço da aula), e o paciente paga o mesmo por aula, qualquer que seja o prazo. O controle de
aulas e as mensalidades do futuro módulo de pagamentos (billing) dependem dessa conta.

## Decisão

- **As aulas do item do Grupo são as semanas da vigência do contrato × vezes por semana**, ao
  valor da aula da linha travada (o preço do plano escolhido). No `ContractItemValue`, que passa a
  receber as semanas do contrato.
- **Contrato só de mensalidade:** não muda (a vigência já é o prazo do plano).
- **Mensalidade com item por duração mais longo** (ex: Trimestral + RPG quinzenal = 24 semanas):
  24 × a frequência, ao preço do Trimestral.
- **Aditivo:** a nova versão calcula pela nova vigência. Prorrogar aumenta as aulas do Grupo; o
  preço da aula continua o travado.

## Alternativas descartadas

- **Manter as semanas do plano:** o contrato misto teria de durar pelo menos o prazo do plano do
  Grupo, o que muda a regra da vigência (o Grupo acompanha o contrato) e obriga o paciente a ficar
  no Grupo depois de terminar os itens por duração.
- **Gravar a quantidade de aulas no item:** a quantidade sai da vigência, que já é calculada; uma
  coluna poderia ficar diferente dela.

## Consequências

- Substitui em parte o [ADR 0032](0032-valor-e-parcelas-do-contrato.md): só a linha do valor do
  item do Grupo ("semanas do plano do Grupo × vezes por semana").
- O valor dos contratos mistos muda, inclusive o dos já gravados: o valor é calculado a cada
  leitura, nunca gravado. Ex: Evolução 2x/semana + Trimestral 2x passa de R$ 1.711,06 para
  R$ 789,72 no item do Grupo.
- Num contrato misto, um plano do Grupo mais longo que a vigência não cobra mais aulas: só muda o
  preço da aula (o limite das opções, no ADR 0010, continua valendo).
