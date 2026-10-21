# 0013 — Prestador sempre com usuário

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Autenticação (`User`) e dado de negócio do prestador (`Provider`, com os
percentuais de repasse) são módulos separados. A principal função do prestador no
sistema é lançar os próprios atendimentos, o que exige login.

## Decisão

- **Todo prestador tem exatamente um usuário** (1:1, `userId` `unique`, fixo após a
  criação). O `PUT` do prestador nem recebe `userId`.
- O usuário precisa estar **ativo** para criar o prestador. O `role` pode ser
  `PROVIDER` ou `ADMIN` (admin que também atende é prestador válido).
- **Inativar o usuário de um prestador ativo é permitido:** é assim que se remove o
  acesso dele ao sistema. Prestador e atendimentos continuam no banco.
- Prestador que não está atendendo é inativado, sem desvincular o usuário.

## Alternativas descartadas

- **Usuário opcional, com vínculo posterior:** um prestador sem usuário não teria
  como lançar, e o administrador não lança por ele.
- **`User` e `Provider` numa entidade só:** acoplaria o módulo de login a regras
  de negócio da clínica que não são dele.

## Consequências

- Se o admin passar a lançar atendimentos por um prestador, esta decisão precisa
  ser revista.
- O usuário também tem soft delete: removê-lo quebraria o histórico do prestador
  (ou seria barrado pela FK).
