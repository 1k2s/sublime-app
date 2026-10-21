# 0008 — Contrato como agregado com um item por grupo

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

O contrato tinha um único preço travado no cabeçalho. Na prática, o paciente
contrata mais de um grupo de preço no mesmo acordo (ex: Evolução com RPG, do
Individual Padrão, e Quiropraxia, do Individual Especializado), cada um com o seu
preço e o seu saldo de sessões, mas com uma vigência, uma forma de pagamento e um
plano só.

## Decisão

- **Agregado `Contract` + `ContractItem`:** o cabeçalho tem o que vale para o
  contrato inteiro (titular, beneficiário, plano, início, fim, forma de
  pagamento); cada item trava a linha de preço de **um grupo**.
- **Um item por grupo** no mesmo contrato. O grupo não é coluna do item (vem da
  linha travada), então a regra é do agregado, não um `unique` no banco.
- **Plano único no contrato.** O item por duração usa uma linha do plano do
  contrato; o item do Grupo usa um plano de mensalidade escolhido pelas regras do
  [ADR 0010](0010-plano-do-item-do-grupo.md).
- **A linha de preço é a fonte da verdade:** grupo e, no Grupo, frequência são
  copiados da linha, nunca recebidos do request.
- **Técnica do item só informativa:** aparece no contrato impresso, mas o
  paciente pode ser atendido em qualquer técnica do grupo do item.
- `ContractItem` é **entidade interna** (princípio 8 do
  [`architecture.md`](../architecture.md)): sem repositório nem service, construtor
  do pacote, criado pela raiz a partir de rascunhos (`ContractItemDraft`) montados
  pelo service. Outros agregados referenciam só o `Contract`.

## Alternativas descartadas

- **Vários contratos simultâneos, um por grupo:** o acordo com o paciente é um só
  (uma assinatura, uma vigência, um pagamento). Separar em contratos permitiria
  vigências e planos diferentes para o mesmo acordo e quebraria a regra de um
  contrato vigente por titular.
- **Receber grupo e frequência do request junto com a linha:** o item poderia
  nascer incoerente com o preço travado.

## Consequências

- Saldo de sessões por item: cada item por duração tem as `sessionCount` do plano
  (Evolução com RPG + Quiropraxia = 12 sessões de cada grupo).
- Preço travado vale para qualquer técnica do grupo do item, na mesma duração da
  linha; o resto paga o preço atual (ver [ADR 0003](0003-preco-historizado.md)).
- Contrato de pacote precisa de pelo menos um item por duração (senão não teria
  como calcular o fim); contrato só com Grupo usa plano de mensalidade.
