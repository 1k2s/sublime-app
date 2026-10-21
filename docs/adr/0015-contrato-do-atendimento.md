# 0015 — Contrato do atendimento: vínculo na data e escolha livre

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto

O prestador lança o paciente que compareceu, que pode ser o titular de um contrato,
o beneficiário do contrato de outra pessoa, as duas coisas, ou nenhuma. Atendimentos
são lançados depois do dia (ex: sexta lançada na segunda), às vezes depois de o
contrato vencer.

## Decisão

- **Contratos vinculados** = versões ativas que cobrem a **data do atendimento**
  (`occurredAt`), não a do lançamento, com o paciente como titular **ou**
  beneficiário, cubram ou não o grupo da técnica.
- **Um paciente é titular de no máximo um contrato vigente e beneficiário de no
  máximo um** (409 no `contract`), então são no máximo dois.
- Nenhum → Avulso. Um → é esse. Dois → o usuário escolhe **livremente**, mesmo que só
  um deles cubra o grupo da técnica.
- Com contrato vinculado, o atendimento **nunca** é lançado como Avulso
  (`contractId` obrigatório).
- O contrato escolhido cobre o grupo → debita o item; não cobre → de-para. O
  beneficiário usa o plano como se fosse o titular, nos dois casos.

## Alternativas descartadas

- **Vigência pela data do lançamento:** a sessão de sexta lançada na segunda sairia
  do contrato errado (ou de nenhum, se ele venceu no sábado).
- **Só contratos que cobrem o grupo:** o beneficiário com contrato próprio não
  poderia escolher qual contrato usar.
- **Obrigar o contrato que cobre o grupo quando houver um:** decisão da clínica foi
  deixar a escolha com o usuário.
- **Beneficiário de vários contratos:** a lista de vinculados ficaria sem limite.
- **Olhar qual versão estava ativa em cada dia:** complicaria a consulta para um caso
  raro (ver consequências).

## Consequências

- Um item incluído por aditivo depois da data do atendimento também é elegível
  (Quiropraxia incluída em 10/11, sessão de 05/11 lançada em 12/11 debita o item).
  Aceito: é raro, e o aditivo costuma ser feito justamente por causa desse
  atendimento.
- Sem limite de retroatividade por enquanto (pode entrar com o pagamento do repasse).
- O frontend consulta os vinculados antes de lançar (`eligible-contracts`) e mostra o
  modal só quando há dois.
