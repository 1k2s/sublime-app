# Ambiente de desenvolvimento

Como rodar, preparar o banco e cadastrar os dados do sistema localmente. Para a
arquitetura e as regras de negócio, ver [`architecture.md`](architecture.md) e
[`domain-model.md`](domain-model.md).

## Stack

- **Spring Boot 4.1.1**, **Java 25**, **Jackson 3** (pacote `tools.jackson`, não
  `com.fasterxml`), Lombok, springdoc (Swagger).
- **MySQL 8** via `docker-compose.yml` (container `mysql-dev`, porta **3307**,
  banco `sublime_db`, usuário `root`/`root`).
- **Frontend:** ReactJS (planejado).

## Rodando localmente

1. Subir o banco: `docker compose up -d`.
2. Subir a aplicação: `./mvnw spring-boot:run`.
3. Swagger: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/api-docs`).

Para conferir se o código compila: `./mvnw -q compile`. É a referência quando a
IDE acusa erro (ver "Problemas conhecidos").

## Banco e schema

- **Schema gerado pelo Hibernate:** `spring.jpa.hibernate.ddl-auto=update` e
  `spring.flyway.enabled=false`, até as migrations do Flyway (ver
  [`roadmap.md`](roadmap.md)).
- O `update` cria tabelas e colunas, mas **não remove colunas nem ajusta
  constraints antigas**. Quando uma entidade perde campos ou muda de
  obrigatoriedade, é preciso zerar o banco:
  1. `docker compose down -v` (apaga o volume `mysql_data`);
  2. `docker compose up -d`;
  3. subir a aplicação (o Hibernate recria as tabelas).

## Dados de teste

- Criados **pela API** (Swagger ou frontend), nunca por `INSERT`/`data.sql`, para
  passar pelas regras do domínio. Scripts de apoio, se houver, ficam em `scripts/`.
- **Ordem de cadastro** (cada passo depende dos anteriores):
  1. grupos de preço;
  2. técnicas;
  3. planos;
  4. de-para dos planos (`PUT /api/plans/{id}/equivalent`, nos dois lados);
  5. preços;
  6. usuários;
  7. prestadores;
  8. pacientes (com a origem, se houver);
  9. contratos.

## Catálogo atual da clínica (dados para o cadastro inicial)

Os planos seguem a tabela "Planos atuais da clínica" em `Plan` (tipo, sessões,
`termWeeks` e de-para). Os grupos e técnicas estão em "Grupos atuais da clínica",
em `PricingGroup` (ambos no [`domain-model.md`](domain-model.md)).

**Valores por sessão nos grupos por duração (60 min / 30 min):**

| Plano | Individual Padrão | Individual Especializado | Em Dupla |
|---|---|---|---|
| Avulso | 220,00 / 132,00 | 275,00 / 176,00 | 265,00 / 158,00 |
| Essencial | 180,00 / 113,00 | 242,00 / 140,80 | 238,00 / 143,00 |
| Evolução | 167,00 / 100,00 | 231,00 / 132,00 | 225,00 / 136,00 |
| Evolução Familiar | 156,00 / 93,00 | *a definir* | *a definir* |
| Transformação | 143,00 / 90,00 | 186,00 / 117,00 | 215,00 / 129,00 |
| Transformação Familiar | 137,00 / 85,00 | 178,00 / 112,00 | 205,00 / 126,00 |
| Vitalidade | 134,00 / 80,00 | 175,00 / 105,00 | 200,00 / 124,00 |
| Vitalidade Familiar | 129,00 / 77,00 | 170,00 / 100,00 | 195,00 / 118,00 |

- Individual Especializado: o Essencial (4 sessões) usa o valor do antigo plano
  "6 sessões"; o Evolução, o do antigo "12 sessões".
- Em Dupla 30 min Essencial: 143,00 (a planilha original tinha 130, corrigido).
- Falta definir o valor da Evolução Familiar no Individual Especializado e no Em
  Dupla.

**Pilates em Grupo — valor por aula (1x / 2x / 3x por semana):**

| Plano | 1x | 2x | 3x |
|---|---|---|---|
| Mensal | 82,50 | 73,63 | 58,91 |
| Trimestral | 73,36 | 65,81 | 52,50 |
| Semestral | 70,06 | 62,57 | 51,06 |
| Semestral Familiar | 67,27 | 60,41 | 49,98 |
| Anual | 63,46 | 55,85 | 48,91 |
| Anual Familiar | 58,38 | 54,58 | 47,83 |

O valor mensal da planilha (ex: Mensal 2x = R$ 589,05) não é gravado: só o valor
por aula é linha de preço.

## Fluxo de git

- Branch `feature/...` (ou `feat/...`) → PR para a `dev`.
- **Conferir o diff antes de commitar** (ver "Arquivos que voltaram sozinhos").

## Problemas conhecidos do ambiente

Não são do código:

- **Erros falsos do Lombok no VS Code** ("Can't initialize javac processor...",
  `getX()` não encontrado): o processador do Lombok falha dentro da extensão Java.
  O Maven compila normalmente; vale o `./mvnw -q compile`.
- **Sem acesso ao Maven Central e ao GitHub por HTTPS** (erro de certificado
  PKIX/SSL, provável proxy da rede): só o cache local do Maven funciona (plugins
  novos não baixam) e o `git push` pode falhar. Tentar outra rede ou
  `git config --global http.sslBackend schannel`. **Nunca desligar a verificação
  SSL.**
- **Arquivos que voltaram sozinhos para versões antigas** (já aconteceu com
  `TechniqueController` e `domain-model.md`): suspeita de OneDrive sincronizando a
  Área de Trabalho ou de aba antiga do VS Code salva por cima.
