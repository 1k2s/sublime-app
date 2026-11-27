# 0045 — Pilates em Grupo com duas contas: o dinheiro e as aulas

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

O Pilates em Grupo é mensalidade: o paciente paga o mês, vá a quantas aulas for. Pela regra da
conta pelo consumo ([ADR 0044](0044-conta-do-paciente-pelo-consumo.md)), as aulas lançadas
entrariam no consumido, e faltar a uma aula baixaria a dívida, o que a mensalidade não faz. Ao
mesmo tempo, a clínica quer saber se o paciente está desmarcando muito ou indo além das aulas
previstas. Até aqui, o item do Grupo não tinha saldo de aulas.

## Decisão

- **Duas contas, que não se afetam:**
  - **o dinheiro:** a dívida são as **mensalidades vencidas**: o valor do item do Grupo ÷ os meses
    do contrato (`Installments.maxFor`), com os centavos na primeira, cada uma num vencimento do
    contrato (o 1º vencimento + N meses), **mesmo com menos parcelas que meses** (quem pagou à
    vista fica com saldo credor, abatido mês a mês). As aulas lançadas **não** entram no consumido;
  - **as aulas (controle):** as contratadas (as semanas da vigência × vezes por semana, a conta do
    valor do item, [ADR 0041](0041-aulas-do-grupo-pela-vigencia.md)) − as lançadas, no saldo de
    sessões do `consultation`.
- **Com aditivo, cada versão conta as próprias mensalidades:** o valor do item do Grupo dela ÷ os
  meses dela, nos vencimentos dela, só as que vencem enquanto ela valeu (do `createdAt` ao
  `deactivatedOn`); o total da cadeia fica nos meses do contrato. Depois da inativação do contrato,
  nada mais vence.
- **No contrato misto**, a dívida é o consumo dos itens por duração + as mensalidades vencidas.
- **Sem o aviso de saldo na tela de Lançamento** para o Grupo (o de saldo esgotado dos itens por
  duração): daria a entender que a aula será cobrada à parte. O saldo de aulas aparece na conta e
  no aditivo.

## Alternativas descartadas

- **Sem saldo de aulas** (a regra anterior): a clínica não veria o paciente que desmarca muito ou
  vai além.
- **As aulas lançadas no consumido:** faltar baixaria a dívida, e ir mais a aumentaria, o que a
  mensalidade não faz.
- **A quantidade de aulas pelas sessões do pacote equivalente** (de-para): o contrato e o billing
  passariam a contar as aulas de jeitos diferentes; a conta pela vigência é a mesma do valor.

## Consequências

- O `findSessionBalance` passou a incluir o item do Grupo, marcado (`groupItem`), para a tela
  dizer "aulas".
- A aula acima das previstas aparece na aba Sessões como "além das aulas previstas", sem valor a
  mais na conta.
- A Produção do dashboard soma as mensalidades pelo vencimento, não as aulas.
