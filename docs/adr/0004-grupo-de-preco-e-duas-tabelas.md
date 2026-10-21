# 0004 — Preço no grupo, com duas tabelas pelo `pricingModel`

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Técnicas de mesmo grau de complexidade COFFITO (ex: Liberação Miofascial, RPG,
Massoterapia, Pilates Individual) custam exatamente o mesmo. A maioria é cobrada
por **duração da sessão × plano**; o Pilates em Grupo é cobrado por **frequência
semanal × plano**, com uma tabela de desconto por volume publicada à parte (que
embute o cálculo de feriados), e não por multiplicação do valor unitário.

## Decisão

- **O preço mora no grupo (`PricingGroup`), não na técnica.** A técnica só aponta
  para o grupo.
- **Discriminador `pricingModel`** no grupo: `DURATION_BASED` ou `FREQUENCY_BASED`.
- **Duas tabelas de preço:** `SessionDurationPrice` (eixo `durationMinutes`) e
  `SessionFrequencyPrice` (eixo `weeklyFrequency`).
- `pricingModel` do grupo e grupo da técnica são **fixos** após a criação.

## Alternativas descartadas

- **Preço na técnica:** o mesmo valor duplicado em cada técnica do grupo, replicado
  a cada reajuste, com risco de divergência silenciosa.
- **Uma tabela de preço só:** colunas nulas na maioria das linhas (duração em
  quem é por frequência e vice-versa).
- **Permitir trocar o `pricingModel` ou o grupo da técnica:** as linhas de preço,
  os contratos e os atendimentos já foram fechados com base no grupo de então.
  Trocar deixaria linhas na tabela errada ou mudaria o preço de registros antigos.

## Consequências

- Reajustar o grupo atualiza todas as técnicas dele de uma vez.
- Reclassificar uma técnica = cadastrar uma nova no grupo certo e inativar a
  antiga. Grupo com outro modelo de preço = grupo novo.
- Quem referencia preço (item do contrato, atendimento) usa duas FKs nullable em
  exclusive arc (princípio 4 do [`architecture.md`](../architecture.md)).
