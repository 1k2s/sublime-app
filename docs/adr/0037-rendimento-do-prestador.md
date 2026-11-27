# 0037 — Rendimento do prestador: só os realizados, pela data do lançamento

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

O prestador só via o repasse do dia, na tela de Lançamento, e só dos atendimentos com a data de
hoje: um retroativo lançado hoje não aparecia em lugar nenhum. O admin não tinha uma visão do
período de nenhum prestador. O dashboard precisava de um número de "quanto rendeu" no dia, na
semana e no mês, e de uma regra para o que entra nele.

Os três status lançados (`ATTENDED`, `NO_SHOW` e `CANCELED_LATE`) são cobrados do paciente e geram
repasse com o mesmo cálculo ([ADR 0014](0014-tres-status-cobrados.md)). Um atendimento tem duas
datas: a do atendimento (`occurredAt`) e, desde esta decisão, a do lançamento (`launchedAt`).

## Decisão

- **Rendimento = soma do repasse dos atendimentos ativos com status `ATTENDED`**, cuja **data do
  lançamento** cai no período.
- **Faltas cobradas à parte:** o `NO_SHOW` e o `CANCELED_LATE` continuam com repasse, mas ficam
  fora do rendimento; cada período mostra a quantidade e o repasse delas e o repasse total (os
  dois somados), para o prestador ver o repasse inteiro sem somar de cabeça.
- **Data do lançamento gravada:** coluna `launched_at DATETIME NOT NULL`, passada pelo service
  (`now()` no fuso da clínica, [ADR 0034](0034-hoje-no-fuso-da-clinica.md)), com o índice
  `(provider_id, launched_at)`.
- **Períodos:** hoje, a semana de segunda a domingo e o mês do calendário, definidos pelo backend
  e devolvidos com o início e o fim.
- **O resumo do dia da tela de Lançamento** (`day-summary`) segue a mesma regra, para "hoje" ter
  um número só nas duas telas.

## Alternativas descartadas

- **Os três status no rendimento** (o rendimento seria o repasse devido inteiro): misturaria o
  atendimento feito com a falta cobrada, e o prestador não veria quanto rendeu atendendo.
- **Esconder as faltas cobradas:** o prestador não teria onde conferir o repasse delas.
- **Falta sem repasse:** mudaria o lançamento e o ADR 0014, fora do escopo.
- **Pela data do atendimento:** um retroativo lançado hoje mudaria um período já visto (a semana
  passada "ganharia" um atendimento), e o retroativo de hoje não apareceria no rendimento de hoje.
- **Últimos 7 e últimos 30 dias** no lugar da semana e do mês: mudam de início todo dia.
- **`DATE` no lugar de `DATETIME`:** perderia a ordem dos lançamentos do mesmo dia, útil para a
  lista e para uma auditoria futura.

## Consequências

- Ver "Rendimento" e "Leituras" em `Consultation`, no `domain-model.md`.
- O rendimento de um dia passado só muda quando o admin desfaz um lançamento dele (o `PROVIDER`
  só desfaz os de hoje).
- A coluna entrou na V1 (migrations editáveis até a versão estável,
  [ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md)): quem puxou zerou o banco local.
- O dashboard mostra quanto rendeu, não quanto já foi pago: o fechamento do repasse continua em
  "Não modelado ainda", no roadmap.
