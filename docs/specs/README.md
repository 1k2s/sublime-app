# Specs (features em andamento)

Uma spec por feature em andamento: `docs/specs/<feature>.md`. Ela é o "contrato" da
feature enquanto ela não está pronta.

**Nenhuma feature em andamento no momento.** O próximo passo está no
[`roadmap.md`](../roadmap.md).

## O que uma spec tem

1. **Status:** o que já existe no código e a branch.
2. **Objetivo:** o que a feature entrega e de quais módulos depende.
3. **Regras:** decididas e ainda não implementadas, com o motivo de cada uma e as
   alternativas descartadas. Pendências de negócio marcadas como *em aberto*.
4. **Desenho:** onde mora cada regra (DTO, entidade, service), consultas, DTOs e
   endpoints. Apresentado e aprovado antes de escrever o código.
5. **Tarefas:** checklist marcada durante a implementação.
6. **ADRs a registrar:** decisões com alternativas descartadas.

## Ao concluir a feature

- As regras vão para o [`domain-model.md`](../domain-model.md) (ou o
  [`architecture.md`](../architecture.md), se forem padrão de construção).
- As decisões viram ADRs em [`adr/`](../adr/README.md).
- A spec é removida (o histórico fica no git).
- O [`roadmap.md`](../roadmap.md) é atualizado.
