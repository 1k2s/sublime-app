# 0021 — Schema versionado pelo Flyway, com `validate`, e enums como `varchar`

- **Status:** aceito
- **Data:** 2026-10-29

## Contexto

O schema era gerado pelo Hibernate (`ddl-auto=update`). O `update` cria tabelas e
colunas, mas não remove colunas nem ajusta constraints. Em 29/10, um banco local
criado antes da reescrita do contrato ainda tinha quatro colunas obrigatórias do
modelo antigo, e criar contrato dava 409 até o banco ser zerado. Duas regras de
negócio (exclusive arc e um preço vigente por combinação) só existiam no código,
porque o Hibernate não sabe gerá-las.

## Decisão

- **O schema é criado e alterado só por migrations do Flyway**, em
  `src/main/resources/db/migration/`, aplicadas ao subir a aplicação.
- **`ddl-auto=validate`:** o Hibernate só confere se as entidades batem com o banco.
- **Migration aplicada nunca é editada;** mudança nova = arquivo novo.
- **V1 gerado a partir do schema que o Hibernate criava**, extraído com
  `mysqldump --no-data` e revisado à mão: ordem das dependências, constraints com
  nomes legíveis (`pk_`, `fk_`, `uk_`, `chk_`) e a collation `utf8mb4_0900_ai_ci`
  mantida (a unicidade de nomes ignora maiúsculas e acentos).
- **Enums como `varchar(30)`** (`@JdbcTypeCode(SqlTypes.VARCHAR)`), sem o `CHECK` de
  valores que o Hibernate cria: a validação do valor fica no enum Java.
- **Regras que o Hibernate não gera** em migrations próprias: V2 (`CHECK` do
  exclusive arc) e V3 (`current_flag` + `UNIQUE`, um preço vigente por combinação).
- **O catálogo da clínica não entra nas migrations:** os dados continuam sendo
  cadastrados pela API, passando pelas regras do domínio.

## Alternativas descartadas

- **Manter o `ddl-auto=update`:** deixa colunas antigas para trás e não versiona as
  mudanças; cada banco pode ficar diferente.
- **Escrever o V1 do zero:** risco de uma diferença de tipo derrubar a aplicação no
  `validate`.
- **Enum nativo do MySQL (`enum(...)`) ou o `CHECK` de valores:** todo valor novo num
  enum exigiria uma migration.
- **Tudo num único V1:** misturaria a "foto" do schema existente com as regras novas,
  e quem revisa não enxergaria o que mudou.
- **Catálogo como migration:** os `INSERT`s pulariam as regras do domínio (tipo do
  plano, de-para, preço positivo).

## Consequências

- Todo banco local criado antes do Flyway precisa ser zerado uma vez
  (`docker compose down -v`); depois disso, as mudanças chegam como migration.
- Mudar uma entidade exige a migration correspondente, senão a aplicação não sobe.
- As duas regras de preço passam a ser garantidas também pelo banco. Uma violação que
  escape do código chega à API como 409 genérico; a causa fica no log.
- Explicação das constraints: "Banco e schema (Flyway)" no
  [`development.md`](../development.md).
