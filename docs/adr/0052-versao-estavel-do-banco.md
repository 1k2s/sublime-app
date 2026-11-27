# 0052 — A versão estável do banco: as migrations congeladas e o MySQL 8.4 LTS

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

O [ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md) deixou as migrations editáveis
"até a versão estável (a versão 1 no ar)": cada mudança de schema editava a V1, e o time zerava o
banco local. O plano mudou: antes do MVP (a produção da clínica), a aplicação vai para um
**ambiente de homologação** numa VPS, com CI/CD, para o time treinar o fluxo de um projeto real.
Num banco que fica no ar, editar uma migration já aplicada quebra o Flyway (o checksum muda), e
zerar deixa de ser uma opção barata.

Junto, o MySQL: o projeto usava o 8.0, que saiu do suporte em abril de 2026.

## Decisão

- **A versão estável é agora** (2026-11-27), antes da homologação. **Uma migration já aplicada
  nunca é editada:** mudança de schema é um arquivo novo (V5, V6...), como no
  [ADR 0021](0021-schema-versionado-flyway.md).
- **As migrations ficam como estão (V1 a V4), revisadas, sem juntar arquivos.** A revisão da V1
  antes de congelar: o tamanho das colunas do paciente (`cpf` 11, `cep` 8, `phone` 20); os
  `CHECK` das regras que as entidades já validam (os percentuais do prestador, os valores e as
  durações dos preços e do atendimento, o fim do contrato depois do início, as sessões e as
  semanas do plano); os nomes dos `CHECK` com o prefixo `chk_`, o da convenção.
- **MySQL 8.4 LTS** (`mysql:8.4.11`), no `docker-compose.yml` e nos testes
  (`TestcontainersConfiguration`; o `MySqlImageTest` confere que são a mesma). A suíte inteira
  aplica as migrations do zero nessa versão.

## Alternativas descartadas

- **Juntar a V2 e a V3 na V1** (o schema num arquivo só): o ganho é pequeno, e mudaria referências
  em ADRs e docs e obrigaria mais um banco zerado. O que importava era revisar a V1, e isso foi
  feito.
- **Continuar editando a V1 até o MVP:** a homologação ficaria com um banco que se zera a cada
  mudança, e o time não treinaria o fluxo das migrations, que é parte do que a homologação ensina.
- **Ficar no MySQL 8.0:** sem correções de segurança desde abril de 2026. Subir antes de congelar
  deixava ajustar a V1, se a 8.4 pedisse algo (não pediu).

## Consequências

- Esta é a **última** vez que o time zera o banco local por causa de uma migration editada.
- Uma coluna ou um `CHECK` novo é uma migration nova; o dado que já existe precisa cumprir a regra
  (ou a migration o corrige antes).
- O aviso "zerem o banco local" deixa de existir nos PRs; no lugar, a migration nova roda sozinha
  quando a aplicação sobe.
- Substitui o [ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md) e, em parte, o
  [ADR 0036](0036-testes-de-integracao-com-testcontainers.md) (a versão do MySQL).
