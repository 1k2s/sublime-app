# 0014 — Só três status de atendimento, todos cobrados

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

O esqueleto do `Consultation` previa cinco status, incluindo desmarcações sem
cobrança (dentro do prazo de 6h) e cancelamentos pela clínica. O sistema existe para
controlar o repasse ao prestador e o saldo do paciente, e a clínica já usa outro
software para a agenda.

## Decisão

`ConsultationStatus` tem só o que é cobrado e gera repasse: `ATTENDED` (aconteceu),
`NO_SHOW` (faltou sem aviso) e `CANCELED_LATE` (desmarcou com menos de 6h). Os três
são cobrados do paciente (num pacote, consomem uma sessão) e geram repasse com o
mesmo cálculo. O que não é cobrado **não é lançado**.

## Alternativas descartadas

- **Manter `CANCELED_ON_TIME` e `CANCELED_BY_CLINIC`:** nunca seriam lançados, já
  que o prestador só lança o que vai receber.
- **Nomes anteriores** (`MISSED`, `UNSCHEDULED_WITH_NOTICE/CHARGE`, `CANCELED`):
  *unscheduled* em inglês significa "não agendado" (encaixe), o oposto de
  "desmarcado". `NO_SHOW` e *late cancellation* são os termos das políticas de
  cancelamento de clínicas.

## Consequências

- Não existe método "conta para o repasse" no enum (seria sempre `true`): o status
  diz **por que** a sessão foi cobrada, útil para relatórios.
- A regra das 6h é aplicada por quem lança; o sistema não guarda a hora do
  atendimento nem a do aviso.
- Taxa de desmarcação e cancelamentos sem cobrança ficam no software de agendamento.
