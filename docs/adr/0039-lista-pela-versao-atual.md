# 0039 — Lista pela versão atual, com o número do aditivo e a data gravados

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

Com aditivos, um contrato vira uma cadeia de versões ([ADR 0011](0011-contrato-versionado-por-aditivos.md)).
A listagem trazia todas: um contrato com dois aditivos aparecia em três linhas, duas como
"Inativo", iguais a um contrato encerrado. A tela precisava dizer em qual aditivo o contrato está
e montar o histórico, mas a resposta só trazia o `previousContractId`. O número da versão não era
persistido, só derivável pela cadeia, e não havia a data em que cada versão foi criada (o
`startDate` é do acordo, igual em todas).

## Decisão

- **`GET /api/contracts` traz só a versão atual** de cada contrato (a que nenhum aditivo
  substituiu: `not exists` no repository), incluindo a inativada. As anteriores ficam no histórico,
  **`GET /api/contracts/{id}/versions`** (a versão e as anteriores, da mais nova para a mais
  antiga).
- **`amendmentNumber` gravado** (coluna `amendment_number`): 0 na original, o da anterior + 1 no
  aditivo, nunca alterado. O `previousContract` continua sendo a ligação; a coluna guarda o
  resultado da contagem.
- **`createdAt` gravado** (coluna `created_at`, `DATE`): o dia em que a versão foi criada, "hoje"
  no fuso da clínica. No aditivo, é a data que calculou o fim.
- **`superseded` calculado, não gravado:** falso na listagem; no `/versions`, pela posição (e a
  primeira, por uma consulta); no `GET /{id}`, uma consulta pela sucessora (`previous_contract_id`
  é `UNIQUE`, com índice).

## Alternativas descartadas

- **Todas as versões na lista, com a substituída como "Substituído":** continua com uma linha por
  versão, e o histórico se mistura com os contratos.
- **Contar a cadeia a cada leitura:** a versão anterior é LAZY, uma consulta por passo; na lista,
  seriam consultas a mais por contrato a cada abertura da tela.
- **Consulta recursiva no SQL** (`WITH RECURSIVE`): SQL nativo, que o projeto não usa, só para
  isso.

## Consequências

- Substitui em parte o [ADR 0011](0011-contrato-versionado-por-aditivos.md): a consequência "o
  número da versão não é persistido: é derivável pela cadeia".
- A V1 foi editada ([ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md)): quem puxar zera
  o banco local.
- Mudança na API: a listagem não traz mais as versões substituídas.
- "Inativo" na tela passa a ser só o contrato inativado.
