# 0034 — "Hoje" no fuso da clínica, fixado na inicialização

- **Status:** aceito
- **Data:** 2026-11-13

## Contexto

Várias regras dependem de "hoje": a vigência do contrato (`current`), os preços vigentes, a data
do atendimento (que não pode ser futura e, para o prestador, tem de ser hoje:
[ADR 0033](0033-lancamento-retroativo-com-liberacao.md)), a liberação de retroativo (vale até
23:59) e o total do dia. O backend usava o `LocalDate.now()` com o fuso da máquina: num servidor
em UTC, depois das 21h de Brasília o "hoje" já seria o dia seguinte, e o prestador não
conseguiria lançar o atendimento do próprio dia.

## Decisão

O fuso padrão da aplicação é fixado em `America/Sao_Paulo` no `SublimeAppApplication.main`, antes
do Spring subir (`TimeZone.setDefault`). Todo `LocalDate.now()`, inclusive o de um código novo, e
a validação `@PastOrPresent` passam a usar o dia da clínica. O fuso fica no código, não numa
variável de ambiente: a clínica fica num lugar só.

## Alternativas descartadas

- **O fuso da máquina** (como era): o "hoje" muda com o servidor.
- **Um `Clock` injetado:** exigiria trocar cada `now()` (9 pontos em 5 módulos), passar "hoje"
  pelos construtores de duas entidades e configurar o relógio do validador; e um `now()` novo,
  escrito sem o `Clock`, voltaria a usar o fuso da máquina sem aviso.
- **O `-Duser.timezone` no comando de inicialização:** fácil de esquecer num ambiente novo.

## Consequências

- Ver a linha "Hoje" em "Padrão de construção", no `architecture.md`.
- Testes que precisem de um "hoje" falso não têm como trocá-lo; se surgirem, a decisão é revista
  (um `Clock` para esses pontos).
- As datas do banco são `DATE` (sem fuso) e o token usa `Instant`: nenhum dos dois muda.
