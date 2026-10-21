# 0012 — Comissão pela origem do paciente

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

O repasse ao prestador depende de quem trouxe o paciente: quem trouxe recebe um
percentual maior. O padrão da clínica é 40% (pacientes da clínica ou de outros
prestadores) e 45% (pacientes que o próprio prestador trouxe), com exceções caso a
caso (ex: estagiário com 30% e 35%).

## Decisão

- **O paciente guarda a origem:** `Patient.referringProviderId`, `null` = paciente
  da clínica. Pode ser alterada; ao atribuir um prestador novo, ele precisa estar
  ativo.
- **O prestador tem dois percentuais:** `commissionPercentage` e
  `referralCommissionPercentage`. Opcionais no cadastro; se não informados, valem
  as constantes da clínica no `ProviderEntity` (40% e 45%).
- **Invariante:** `referralCommissionPercentage >= commissionPercentage` (400).
- Mudança de percentual é `UPDATE` simples: o atendimento grava o percentual
  aplicado como snapshot.

## Alternativas descartadas

- **Percentual fixo único no código:** não permite exceção para estagiários.
- **Dois campos obrigatórios:** o admin digitaria 40/45 em todo cadastro.
- **Origem no contrato:** a origem é do paciente. No contrato, teria que ser
  preenchida de novo a cada renovação, e um esquecimento pagaria o percentual
  errado.
- **Um "id da clínica" no lugar do `null`:** exigiria um prestador fictício na
  tabela ou um número que não aponta para nada. O nulo diz "não foi trazido por
  ninguém", e a FK garante que, quando preenchido, o prestador existe.

## Consequências

- `patient` passa a depender de `provider` e deixa de ser módulo de base (sem
  ciclo; ver [ADR 0001](0001-monolito-modular.md)).
- Mudar o padrão da clínica exige alterar as constantes; prestadores já
  cadastrados mantêm o que foi combinado com eles.
- Se surgirem muitas faixas, a evolução natural são categorias de prestador
  (Padrão, Estagiário...) com o par de percentuais.
- Nenhum dos percentuais varia por técnica.
