# 0018 — Saldo esgotado aceita o lançamento

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

Um item de contrato pode ter as sessões esgotadas e o paciente continuar sendo
atendido (ex: o admin ainda não fez o aditivo). O lançamento é o registro do repasse
do prestador.

## Decisão

O lançamento é **aceito** e o saldo do item fica **negativo** (ex: −1). A resposta
traz o saldo depois do lançamento (`remainingSessions`), e o modal dos contratos e o
saldo do contrato mostram o valor negativo para o frontend destacar.

## Alternativas descartadas

- **Bloquear o lançamento:** o atendimento aconteceu; o prestador perderia o registro
  do repasse ou lançaria por fora.
- **Virar Avulso automaticamente:** mudaria o preço sem ninguém decidir.

## Consequências

- O excesso é regularizado no aditivo (prorrogar, mais sessões) ou no encerramento
  do contrato, em que o débito vira saldo do paciente (módulo futuro).
