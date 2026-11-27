# 0037 — Parcelas escolhidas em todo aditivo

- **Status:** aceito (em parte substituído por [0047](0047-parcelas-do-aditivo-sobre-o-que-falta.md): o que as parcelas dividem e o máximo)
- **Data:** 2026-11-26

## Contexto

O aditivo copiava as parcelas da versão anterior ([ADR 0032](0032-valor-e-parcelas-do-contrato.md)).
Mas o aditivo muda o total (incluir ou remover um item) e o prazo (prorrogar ou incluir um item
por duração), e com o prazo muda o máximo de parcelas. Copiadas, as parcelas podiam ficar sem
sentido: o total dobrava e as parcelas eram as mesmas, ou o contrato prorrogado para 26 semanas
continuava limitado a 3.

As parcelas são só o **plano de pagamento**. A dívida do paciente é o consumo (atendimentos
lançados × valor), comparado com o que ele pagou, no futuro módulo `billing`; o sistema ainda não
sabe o que foi pago.

## Decisão

- **O admin escolhe as parcelas em todo aditivo** (`installments` obrigatório no pedido, de 1 a
  12), de 1 até os **meses da nova vigência**, a mesma regra da criação
  (`Installments.validateCount`, validada depois de calcular o novo fim). Acima do máximo: 400
  "O número de parcelas deve ser de 1 a N (meses do contrato).".
- As parcelas dividem **o total da nova versão**, com os centavos na primeira, como na criação.
- Sem coluna nova: a `installments` já existia.

## Alternativas descartadas

- **Copiar sempre:** o total muda e as parcelas não.
- **Renegociar só na prorrogação:** incluir um item também muda o total, sem mudar o fim.
- **Descontar as parcelas já pagas:** o sistema não sabe o que foi pago até o `billing`.

## Consequências

- Substitui em parte o [ADR 0032](0032-valor-e-parcelas-do-contrato.md): só a linha "Aditivo:
  copia as parcelas da versão anterior".
- O pedido do aditivo passou a exigir `installments` (mudança na API).
- A tela do aditivo mostra as parcelas antes e depois (prévia do aditivo,
  [ADR 0038](0038-previa-do-aditivo.md)).
