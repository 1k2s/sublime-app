# 0020 — Lançamento de atendimento imutável

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

Um atendimento lançado entra direto nos honorários do prestador, sem aprovação, e
pode ter sido pago. Lançamentos errados acontecem (paciente, técnica ou data
trocados).

## Decisão

- **O lançamento nunca é editado.** Não existe `PUT`. Para corrigir: inativar (soft
  delete, `DELETE` → `active = false`; inativar de novo → 409) e lançar de novo.
- Atendimento inativo não conta no saldo de sessões nem no repasse.

## Alternativas descartadas

- **Editar o lançamento:** mudaria um repasse que pode já ter sido pago, sem deixar
  rastro do erro.

## Consequências

- É a mesma lógica de tudo que tem efeito financeiro no projeto: preço historizado
  ([ADR 0003](0003-preco-historizado.md)), contrato versionado
  ([ADR 0011](0011-contrato-versionado-por-aditivos.md)) e snapshots que nunca mudam.
- O erro e a correção ficam registrados.
- Com o pagamento do repasse (fechamento do período), entra "atendimento de período
  já pago não pode ser inativado".
