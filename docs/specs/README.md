# Specs (features em andamento)

Uma spec por feature em andamento: `docs/specs/<feature>.md`. Ela é o "contrato" da
feature enquanto ela não está pronta.

**Em andamento:**

| Spec | Feature |
|---|---|
| [`tela-de-usuarios.md`](tela-de-usuarios.md) | tela do `ADMIN` para criar, alterar, inativar e reativar usuários (falta o teste manual) |

As próximas estão nos "Próximos passos" do [`roadmap.md`](../roadmap.md).

## Modelo

Toda spec segue esta estrutura (seção que não se aplica fica com "não se aplica", para
ninguém achar que foi esquecida). O desenho é apresentado e aprovado antes de escrever o
código.

```markdown
# Spec — Nome da feature

Uma frase: o que a feature entrega.

## Índice <!-- omit from toc -->

## 1. Status
O que já existe no código, a branch e a base do PR.

## 2. Objetivo e escopo
O que a feature entrega e de quais módulos depende. **Entra:** ... **Fica para
depois:** ... (com as decisões já tomadas sobre o que fica para depois).

## 3. Regras
Decididas e ainda não implementadas, com o motivo de cada uma, as mensagens ao usuário
e as alternativas descartadas. Pendências de negócio marcadas como *em aberto*.

## 4. Desenho do backend
Onde mora cada regra (DTO, entidade, service), consultas, endpoints e banco: uma tabela
por módulo (arquivo | mudança).

## 5. Desenho do frontend
Uma tabela (arquivo | mudança). Só o formato; a regra é a da API.

## 6. Tarefas
Checklist marcada durante a implementação (backend, frontend, testes, conclusão).

## 7. Casos de teste
Pré-requisitos e uma tabela por bloco (# | caso | esperado), pela API e pela interface.

## 8. Pendências e pontos abertos
O que surgiu durante a feature: o que falta decidir e o que fica fora dela (vai para o
roadmap na conclusão).

## 9. Documentação ao concluir
O que muda em cada documento de referência e no roadmap.

## 10. ADRs a registrar
Cada decisão com as alternativas descartadas e o motivo.
```

## Ao concluir a feature

- As regras vão para o [`domain-model.md`](../domain-model.md) (ou o
  [`architecture.md`](../architecture.md), se forem padrão de construção).
- As decisões viram ADRs em [`adr/`](../adr/README.md).
- O que a spec deixou para depois (seções 2 e 8) vai para o [`roadmap.md`](../roadmap.md),
  que também é atualizado com o estado da feature.
- A spec é removida (o histórico fica no git). Antes, procure quem a cita: comentários
  do código e documentos passam a citar o ADR ou a seção do documento de referência
  (`node scripts/check-docs.mjs` acusa o que sobrar).
