# Decisões de arquitetura e de domínio (ADRs)

Cada arquivo registra **uma decisão**: o problema, o que foi decidido, as
alternativas descartadas e as consequências aceitas. Os documentos de referência
([`architecture.md`](../architecture.md) e [`domain-model.md`](../domain-model.md))
dizem a regra com um "por quê" de uma linha e apontam para o ADR.

## Regras

- **Quando escrever:** a decisão tinha alternativas reais, é difícil de reverter
  (schema, dependência entre módulos, formato de dados) ou alguém provavelmente vai
  perguntar "por que não fizeram X?". Decisão pequena fica só com uma linha na
  referência.
- **ADR aceito não é editado.** Se a decisão mudar, cria-se um ADR novo que o
  substitui, e o antigo recebe só o status "Substituído por NNNN". Correção de
  digitação ou de link é permitida.
- **Numeração sequencial**, nunca reaproveitada: `NNNN-titulo-em-kebab-case.md`.
- Decisões de uma feature em andamento ficam na spec dela ([`../specs/`](../specs/))
  e viram ADR quando a feature é concluída.

## Modelo

```markdown
# NNNN — Título da decisão

- **Status:** aceito | substituído por NNNN
- **Data:** AAAA-MM-DD

## Contexto
O problema e as restrições que levaram à decisão.

## Decisão
O que foi decidido.

## Alternativas descartadas
- Alternativa: por que não.

## Consequências
O que muda, o que fica mais fácil e o que foi aceito como custo.
```

## Índice

Os ADRs 0001 a 0013 foram registrados retroativamente em 2026-10-20, a partir das
discussões que estavam no `domain-model.md` e no `architecture.md`.

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-monolito-modular.md) | Monolito modular com dependência unidirecional | aceito |
| [0002](0002-value-object-de-um-valor.md) | Value object de um valor: `record` + `AttributeConverter`, sem arredondar | aceito |
| [0003](0003-preco-historizado.md) | Preço historizado por `validFrom`/`validTo` exclusivo | aceito |
| [0004](0004-grupo-de-preco-e-duas-tabelas.md) | Preço no grupo, com duas tabelas pelo `pricingModel` | aceito |
| [0005](0005-tipo-do-plano.md) | Tipo do plano (`PlanType`) | aceito |
| [0006](0006-prazo-do-grupo-em-semanas.md) | Prazo dos planos do Grupo em semanas (`termWeeks`) | aceito |
| [0007](0007-de-para-de-planos.md) | De-para de planos nos dois lados | aceito |
| [0008](0008-contrato-com-itens.md) | Contrato como agregado com um item por grupo | aceito |
| [0009](0009-vigencia-calculada.md) | Vigência calculada e `SessionFrequency` | aceito |
| [0010](0010-plano-do-item-do-grupo.md) | Opções do plano do item do Grupo | aceito |
| [0011](0011-contrato-versionado-por-aditivos.md) | Contrato versionado por aditivos | aceito |
| [0012](0012-comissao-pela-origem-do-paciente.md) | Comissão pela origem do paciente | aceito |
| [0013](0013-prestador-com-usuario.md) | Prestador sempre com usuário | aceito |
| [0014](0014-tres-status-cobrados.md) | Só três status de atendimento, todos cobrados | aceito |
| [0015](0015-contrato-do-atendimento.md) | Contrato do atendimento: vínculo na data e escolha livre | aceito |
| [0016](0016-resolucao-de-preco-no-lancamento.md) | Resolução de preço no lançamento pelo sistema | aceito |
| [0017](0017-contrato-e-origem-do-preco-gravados.md) | Contrato e origem do preço gravados no atendimento | aceito |
| [0018](0018-saldo-esgotado-aceita-lancamento.md) | Saldo esgotado aceita o lançamento | aceito |
| [0019](0019-repasse-com-half-up.md) | Repasse arredondado com `HALF_UP` | aceito |
| [0020](0020-lancamento-imutavel.md) | Lançamento de atendimento imutável | aceito |
| [0021](0021-schema-versionado-flyway.md) | Schema versionado pelo Flyway, com `validate`, e enums como `varchar` | aceito |
