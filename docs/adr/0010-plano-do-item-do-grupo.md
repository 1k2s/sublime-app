# 0010 — Opções do plano do item do Grupo

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Num contrato misto, o plano do item do Grupo é sugerido pelo de-para (ver
[ADR 0007](0007-de-para-de-planos.md)), mas a vigência do contrato pode passar do
prazo do plano sugerido. Ex: Evolução quinzenal = 24 semanas, de-para Trimestral =
13. Sem troca, o paciente faria o Grupo por 24 semanas pagando a aula do
Trimestral (R$ 73,36 em 1x/semana) em vez da do Semestral (R$ 70,06). Por outro
lado, quanto mais longo o plano do Grupo, mais barata a aula.

## Decisão

- O usuário pode escolher **qualquer plano de mensalidade ativo até o primeiro
  cujo prazo cubra a vigência do contrato**, incluindo os de mesmo prazo (vigência
  24 → Mensal, Trimestral, Semestral **e** Semestral Familiar).
- **Sem limite para baixo:** plano mais curto é mais caro e favorece a clínica.
- Se nenhum cobrir a vigência (contrato prorrogado além de 52 semanas), todos
  valem. Contrato só com Grupo: a única opção é o próprio plano do contrato.
- **Sem restrição de família:** Familiar ou não.
- A regra fica no backend (valida na criação e no aditivo) e é exposta nos
  endpoints de apoio (`term-preview` e `group-plan-options`).

## Alternativas descartadas

- **Só o plano do de-para, sem troca:** o caso do Evolução quinzenal acima.
- **Sem limite para cima:** um Essencial semanal (4 semanas) com Grupo Anual
  pagaria o preço de quem se compromete por um ano.
- **Limitar ao maior plano que cabe na vigência:** zeraria o desconto excessivo,
  mas o Evolução quinzenal (24) perderia o Semestral (26).
- **Familiar só com Familiar:** exigiria um campo "familiar" no `Plan` (ou
  reconhecer pelo nome, já descartado no [ADR 0005](0005-tipo-do-plano.md)), sem
  ganho que justifique. A sugestão do de-para já leva ao Familiar certo.

## Consequências

- **Risco residual aceito:** desconto pequeno quando o plano escolhido cobre bem
  mais que a vigência (ex: Essencial mensal, 16 semanas, com Semestral, 26). É
  escolha deliberada do admin.
- No aditivo, as opções usam a vigência atual (início → fim, com prorrogação), e o
  plano do Grupo só é revalidado se o item do Grupo for novo ou trocar de linha.
