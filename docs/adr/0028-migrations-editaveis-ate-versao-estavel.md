# 0028 — Migrations editáveis até a versão estável

- **Status:** aceito
- **Data:** 2026-11-07

## Contexto

O [ADR 0021](0021-schema-versionado-flyway.md) definiu que uma migration aplicada nunca é
editada: mudança nova é um arquivo novo. O projeto ainda não tem MVP nem versão no ar, o
time zera o banco local com frequência e, antes de colocar a aplicação no ar, o banco será
recriado do zero. Até lá, o histórico de migrations não protege nenhum dado real.

## Decisão

- **Até a versão estável (a versão 1 no ar), as migrations podem ser editadas.** Mudanças
  de schema entram no arquivo da tabela (ex: colunas novas na `V1`).
- **Quem puxar uma migration editada zera o banco local** (`docker compose down -v` e
  `up -d`); quem editar avisa o time no PR.
- Dados de referência continuam numa migration própria (o seed do catálogo, `V4`).
- **Depois da versão estável, volta a valer o ADR 0021:** migration aplicada nunca é
  editada. Antes da versão 1, as migrations são revisadas e consolidadas.
- Substitui, até a versão estável, o ponto "migration aplicada nunca é editada" do ADR 0021.

## Alternativas descartadas

- **Uma migration nova a cada mudança antes do MVP:** acumularia arquivos que só existem
  por causa de bancos de desenvolvimento, que são zerados de qualquer forma.

## Consequências

- Cada migration editada obriga todo o time a zerar o banco local e perder os dados
  locais. O catálogo volta pela `V4` e o admin padrão pelo `AdminInitializer`
  ([ADR 0027](0027-admin-padrao-protegido.md)); os demais dados de teste são recriados.
- Sem o aviso no PR, a aplicação de quem puxar não sobe (o Flyway acusa a diferença de
  checksum).
- A regra muda de novo na versão 1: o roadmap guarda a tarefa de consolidar as migrations.
