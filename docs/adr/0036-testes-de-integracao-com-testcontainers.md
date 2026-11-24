# 0036 — Testes de integração com o MySQL do Testcontainers

- **Status:** aceito
- **Data:** 2026-11-13

## Contexto

Até aqui os testes eram manuais: cada feature exigia dezenas de casos pela API e pela tela,
refeitos a cada merge da `dev`. Os testes de unidade das entidades cobrem as regras isoladas,
mas a autorização por perfil, o lançamento e as constraints do banco só aparecem com a
aplicação inteira. O único teste que subia a aplicação (`contextLoads`) usava o
`application.properties` normal: falhava sem o `mysql-dev` no ar e rodava o Flyway e o
`AdminInitializer` no banco de desenvolvimento.

## Decisão

Os testes de integração sobem a aplicação inteira contra um **MySQL descartável em container**
(Testcontainers), apontado pelo `@ServiceConnection` do Spring Boot. A imagem tem a **versão
exata**, a mesma do `docker-compose.yml` (`mysql:8.0.46`), para os testes e o desenvolvimento
rodarem no mesmo banco. A anotação `@IntegrationTest` (em `src/test`) junta a configuração:
- `@SpringBootTest` com o container (`TestcontainersConfiguration`);
- o perfil `test` (`application-test.properties`), com o segredo do JWT e o admin padrão fixos;
- o MockMvc, para chamar a API.

**Sem `@Transactional` no teste:** cada requisição tem a própria transação, como em produção.
O que um teste grava fica no banco até o fim da execução, por isso cada teste cria dados únicos
(ex: e-mails com um sufixo único) e nunca supõe uma tabela vazia.

Todas as classes dividem o mesmo contexto do Spring e, com ele, o mesmo container, que sobe uma
vez por execução. O Flyway aplica as migrations nele: o teste valida também o schema real.

## Alternativas descartadas

- **H2 em memória:** as migrations usam recursos do MySQL; o banco do teste não seria o da
  produção.
- **O banco do `docker-compose`:** sujaria os dados de desenvolvimento, dependeria do estado
  deixado por quem rodou antes e não existe num CI.
- **Rollback por teste (`@Transactional` no teste):** a requisição do MockMvc entraria na
  transação do teste, e o teste passaria onde a produção falha (alteração que o service não
  grava, um dado lido do cache do Hibernate em vez do banco). O lazy loading fora do service não
  entra nessa conta: o open-in-view do Spring vem ligado e o mantém funcionando nos dois casos.
- **Container por classe de teste (`@Container` estático):** subiria um MySQL por classe, com
  mais tempo e sem ganho.

## Consequências

- `./mvnw test` exige o **Docker no ar** (o Docker Desktop, mesmo sem o `docker compose up`).
- A primeira execução baixa a imagem do MySQL; depois, o container sobe em segundos.
- Teste que conta registros (ex: o tamanho de uma listagem) filtra pelos próprios dados, nunca
  pelo total da tabela.
- O `backend/.env` do desenvolvedor também é lido nos testes (o `spring.config.import` do
  `application.properties`), mas o perfil `test` sobrescreve as chaves que a aplicação usa.
  Propriedade nova lida do `.env` precisa do seu valor no `application-test.properties`, ou o
  teste passa a depender da máquina.
- **Fuso da clínica nos testes** (substitui em parte o [ADR 0034](0034-hoje-no-fuso-da-clinica.md):
  onde o fuso é aplicado). O `@SpringBootTest` não chama o `main`, onde o fuso era fixado: os
  testes rodariam no fuso da máquina. O `TimeZone.setDefault` passa para o bloco `static` do
  `SublimeAppApplication`, que roda ao carregar a classe, em produção e nos testes. O Maven roda
  os testes numa JVM em UTC (como o CI), e um teste confere que o fuso da clínica vale mesmo
  assim. A decisão do ADR 0034 (fuso fixo no código, não um `Clock`) continua.
- O "hoje" do backend continua sem um relógio falso nos testes: os testes usam datas relativas a
  hoje, e os de unidade recebem a data pronta.
- Mudar a versão do MySQL é mudar os dois lugares (o `docker-compose.yml` e o
  `TestcontainersConfiguration`); o `MySqlImageTest` falha se eles divergirem.
- Uma classe de integração com configuração própria (um `@MockitoBean`, outro `@Import`) cria
  um segundo contexto do Spring e um segundo container: a configuração fica só na
  `@IntegrationTest`.
- O mesmo `./mvnw test` roda no CI, sem configuração extra além do Docker.
