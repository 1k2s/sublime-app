# 0017 — Contrato e origem do preço gravados no atendimento

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

No de-para (caso 2 do [ADR 0016](0016-resolucao-de-preco-no-lancamento.md)), o preço
vem do plano de um contrato que não cobre o grupo atendido, e o usuário pode escolher
entre dois contratos. O saldo de sessões soma os atendimentos de toda a cadeia de
versões do contrato.

## Decisão

- O `contractId` é gravado nos casos 1 e 2 (nulo só no Avulso).
- O campo `pricingSource` (`CONTRACT_ITEM`, `EQUIVALENT_PLAN`, `SINGLE_SESSION`)
  registra o caso. É **calculado pela entidade** no lançamento, nunca vem do request.
- O saldo conta só `CONTRACT_ITEM` (`PricingSource.debitsContractItem()`).

## Alternativas descartadas

- **`contractId` nulo no de-para:** perde qual contrato determinou o preço
  (princípio 5) e o contrato de origem do valor que vai para o saldo do paciente.
- **Deduzir o caso em cada consulta** (atendimento → versão gravada → itens → grupo):
  é possível, porque a versão gravada nunca muda, mas cada consulta de saldo e de
  relatório repetiria a dedução. Uma consulta que contasse só pela cadeia e pelo
  grupo erraria: uma Quiropraxia pelo de-para gravada com V1 passaria a consumir uma
  sessão do item de Quiropraxia incluído depois na V2.

## Consequências

- É um dado derivável gravado, mas não só para exibição (princípio 2): decide se uma
  sessão contratada foi consumida. Concentra a lógica num lugar só e facilita ajustes.
- Relatórios por caso filtram pelo campo (ex: aulas experimentais do Grupo no mês).
