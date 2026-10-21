# 0009 — Vigência calculada e `SessionFrequency`

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

O `endDate` do contrato era digitado, e a frequência era um inteiro
(`weeklyFrequency`) que não representava quinzenal nem mensal. A tabela comercial
da clínica não vende pacote longo em ritmo lento (Transformação mensal, Vitalidade
quinzenal e mensal). O prazo é derivável: sessões do plano ÷ ritmo.

## Decisão

- **O `endDate` é sempre calculado:** `endDate = startDate + prazo − 1 dia`
  (inclusivo).
- **Enum `SessionFrequency`** com os ritmos vendidos: `MONTHLY`, `BIWEEKLY`,
  `ONCE_A_WEEK` … `FIVE_TIMES_A_WEEK`. O cálculo mora no enum (princípio 6).
- **Prazo do item por duração = sessões ÷ ritmo, em semanas, arredondado para
  cima**, com **teto de 52 semanas**.
- **Todas as combinações abaixo do teto são permitidas.**
- **Vigência do contrato = o maior prazo entre os itens por duração.** O item do
  Grupo não entra no cálculo: acompanha a vigência e termina junto.
- **Contrato só com Grupo:** a vigência é o `termWeeks` do plano (ver
  [ADR 0006](0006-prazo-do-grupo-em-semanas.md)).

## Alternativas descartadas

- **`endDate` digitado entre um mínimo (o cálculo) e um máximo (12 meses):**
  contradiz o cálculo e permitiria esticar pacote (ex: Essencial semanal com 12
  meses), o que a tabela da clínica proíbe.
- **Manter o inteiro `weeklyFrequency`:** não representa quinzenal nem mensal.
- **VO "N sessões por período":** aceitaria combinações que a clínica não vende
  (ex: 4x por mês). O enum é um cardápio fechado.
- **Prazo em meses:** duas unidades para comparar (itens e planos do Grupo).
- **Grupo dentro do cálculo** (vigência = maior entre todos os itens): o Grupo é
  mensalidade, sem controle de presença que justifique definir a vigência.
- **Bloquear combinações abaixo do teto:** nos grupos por duração a frequência não
  afeta o preço, então ritmo rápido não dá vantagem financeira, só esgota o saldo
  antes. O risco real (esticar o pacote) o teto já barra.

## Consequências

- Os prazos ficam um pouco menores que os "meses" da tabela comercial (Evolução
  quinzenal = 24 semanas, ~5,5 meses, não 6).
- Semana quebrada arredonda para cima: o paciente nunca fica com menos tempo que o
  necessário.
- Ritmo novo = valor novo no enum. Se a clínica quiser bloquear combinações, entra
  uma lista explícita de frequências por plano.
- No item do Grupo, a frequência fica nula: ela é o eixo do preço e já está na
  linha travada.
