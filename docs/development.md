# Ambiente de desenvolvimento

Como rodar, preparar o banco e cadastrar os dados do sistema localmente (backend e
frontend). Para a arquitetura e as regras de negócio, ver
[`architecture.md`](architecture.md) (com a seção "Frontend") e
[`domain-model.md`](domain-model.md).

## Índice <!-- omit from toc -->

- [Stack](#stack)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Rodando localmente](#rodando-localmente)
- [Frontend e API](#frontend-e-api)
- [Banco e schema (Flyway)](#banco-e-schema-flyway)
  - [Criando uma migration](#criando-uma-migration)
  - [Banco local antigo](#banco-local-antigo)
  - [Constraints do banco](#constraints-do-banco)
- [Dados de teste](#dados-de-teste)
- [Catálogo atual da clínica (dados para o cadastro inicial)](#catálogo-atual-da-clínica-dados-para-o-cadastro-inicial)
- [Fluxo de git](#fluxo-de-git)
- [Problemas conhecidos do ambiente](#problemas-conhecidos-do-ambiente)

## Stack

- **Spring Boot 4.1.1**, **Java 25**, **Jackson 3** (pacote `tools.jackson`, não
  `com.fasterxml`), Lombok, springdoc (Swagger).
- **MySQL 8** via `docker-compose.yml` (container `mysql-dev`, porta **3307**,
  banco `sublime_db`, usuário `root`/`root`).
- **Frontend:** React 19 + Vite, JavaScript (sem TypeScript), CSS Modules, React
  Router, TanStack Query, React Hook Form + Zod, Axios. Node 22. Convenções na seção
  "Frontend" do [`architecture.md`](architecture.md); dependências abaixo, em
  "Frontend e API".

## Estrutura do repositório

Monorepo ([ADR 0022](adr/0022-monorepo.md)):

| Pasta | Conteúdo | Comandos rodam em |
|---|---|---|
| `backend/` | API Spring Boot (Maven) | `backend/` (`./mvnw ...`) |
| `frontend/` | interface React + Vite (npm) | `frontend/` (`npm ...`) |
| `docs/` | documentação do produto (vale para os dois) | — |
| raiz | `docker-compose.yml`, `CLAUDE.md`, `README.md`, `.github` | `docker compose ...` |

## Rodando localmente

1. Subir o banco (na raiz): `docker compose up -d`.
2. Subir a API (em `backend/`): `./mvnw spring-boot:run`.
   Swagger: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/api-docs`).
3. Subir a interface (em `frontend/`): `npm ci` na primeira vez (instala as
   dependências do `package-lock.json`) e depois `npm run dev`. Abre em
   `http://localhost:5173`.

Para conferir se o backend compila: `./mvnw -q compile` (em `backend/`). É a
referência quando a IDE acusa erro (ver "Problemas conhecidos"). No front:
`npm run build` e `npm run lint` (em `frontend/`).

## Frontend e API

- **Endereço da API:** variável `VITE_API_URL` no `frontend/.env`
  (`http://localhost:8080/api`). Depois de alterar, reinicie o `npm run dev`.
- **CORS:** o `SecurityConfig` do backend libera `http://localhost:5173` em
  `/api/**` (GET, POST, PUT, PATCH, DELETE), só para desenvolvimento. Mudou a porta
  do Vite? Ajuste lá.
- **Contrato:** o Swagger do backend é a referência dos endpoints, DTOs e mensagens
  de erro. As regras de negócio estão no [`domain-model.md`](domain-model.md).
- **Alias `@/`:** aponta para `frontend/src/`. Configurado no `vite.config.js` (build) e
  no `jsconfig.json` (autocomplete e "ir para definição" no editor; reinicie o VS Code se
  o editor não reconhecer).

**Dependências do frontend** (`frontend/package.json`):

| Dependência | Para que serve |
|---|---|
| `react-router-dom` | rotas |
| `@tanstack/react-query` | cache e invalidação das chamadas à API |
| `axios` | cliente HTTP (token e tratamento de 401) |
| `react-hook-form`, `zod`, `@hookform/resolvers` | formulários e validação de formato |
| `date-fns` | datas (filtros por período, `validFrom`/`validTo`) |
| `decimal.js` | exibição precisa de valores monetários |
| `lucide-react` | ícones |
| `react-pro-sidebar` | menu lateral |
| `sweetalert2` | alertas de erro da API (`shared/utils/alert.js`) |

O `react-pro-sidebar` 1.1.0 funciona com React 19, mas tem manutenção irregular: confira
a compatibilidade antes de subir a versão do React.

## Banco e schema (Flyway)

O schema é criado e alterado **só pelas migrations do Flyway**
([ADR 0021](adr/0021-schema-versionado-flyway.md)):

- As migrations ficam em `backend/src/main/resources/db/migration/` e rodam sozinhas ao
  subir a aplicação. O Flyway anota as que já aplicou na tabela
  `flyway_schema_history` e roda só as que faltam.
- `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate só **confere** se as
  entidades batem com o banco (colunas e tipos). Se não baterem, a aplicação **não
  sobe**, e o log diz qual coluna está diferente. Ele nunca cria nem altera tabelas.

| Migration | O que faz |
|---|---|
| `V1__initial_schema.sql` | todas as tabelas, com constraints de nomes legíveis |
| `V2__price_exclusive_arc_check.sql` | `CHECK` do exclusive arc em `contract_item` e `consultation` |
| `V3__single_current_price.sql` | uma linha de preço vigente por combinação (`current_flag` + `UNIQUE`) |

### Criando uma migration

1. Mude a entidade (ex: um campo novo).
2. Crie o arquivo seguinte: `V4__descricao_em_ingles.sql` (dois sublinhados; o
   número define a ordem de execução).
3. Escreva o SQL (`ALTER TABLE ...`) e suba a aplicação: o Flyway aplica, e o
   `validate` confere se a entidade e o banco ficaram iguais.

Regras:
- **Uma migration já aplicada nunca é editada.** O Flyway guarda um checksum de
  cada arquivo e recusa subir se ele mudar. Corrigir = um arquivo novo. Antes do
  merge, enquanto ninguém mais aplicou, ainda dá para ajustar o arquivo e zerar o
  banco local.
- Enum novo ou valor novo num enum **não exige migration**: as colunas de enum são
  `varchar(30)`, e quem valida o valor é o enum Java.

### Banco local antigo

Um banco criado antes do Flyway (pelo antigo `ddl-auto=update`) não tem o
histórico, e o Flyway recusa iniciar num schema que já tem tabelas. **Zere uma vez:**
1. `docker compose down -v` (apaga o volume `mysql_data` e todos os dados locais);
2. `docker compose up -d`;
3. subir a aplicação (o Flyway cria tudo do zero).

Depois disso, ninguém mais precisa zerar: cada mudança chega como migration.

### Constraints do banco

As constraints são as regras que o próprio banco garante, recusando a gravação que as
viola, venha ela da aplicação ou de um `INSERT` manual. Os nomes seguem um prefixo,
para a mensagem de erro já dizer qual regra foi violada:

| Prefixo | Significado | Exemplo |
|---|---|---|
| `pk_` | primary key (identificador da linha) | `pk_patient` |
| `fk_` | foreign key (aponta para outra tabela) | `fk_contract_patient` |
| `uk_` | unique key (valor que não pode repetir) | `uk_patient_cpf` |
| `chk_` | check (condição que a linha precisa cumprir) | `chk_consultation_one_price` |

**`UNIQUE` é um índice.** O MySQL implementa todo `UNIQUE` como um índice: uma
estrutura ordenada, à parte da tabela, como o índice remissivo de um livro. É
consultando o índice que o banco descobre rapidamente se o valor já existe. Como
efeito colateral, ele acelera buscas por essas colunas. As foreign keys também ganham
índice automaticamente. Índices só de desempenho (`idx_`) ainda não foram
necessários. Todo índice ocupa espaço e deixa a gravação um pouco mais lenta, então
só entram onde se pagam.

**`CHECK` do exclusive arc (V2).** Item de contrato e atendimento apontam para
exatamente uma linha de preço: por duração **ou** por frequência.

```sql
CHECK ((session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL))
```

| duração | frequência | Resultado |
|---|---|---|
| 5 | NULL | ✅ grava |
| NULL | 3 | ✅ grava |
| 5 | 3 | ❌ `Check constraint 'chk_..._one_price' is violated` |
| NULL | NULL | ❌ idem |

O código já valida isso (`ContractItemDraft`, `ConsultationEntity`); o `CHECK` é a
última barreira.

**Um preço vigente por combinação (V3).** Para cada grupo + plano + duração (ou
frequência), só pode existir uma linha com `valid_to` nulo. O código checa antes de
criar, mas duas gravações **simultâneas** passariam as duas pela checagem e criariam
dois preços vigentes. Um `UNIQUE (grupo, plano, duração)` simples não serve, porque
bloquearia também o histórico de reajustes. A solução tem duas peças:

1. **Coluna calculada `current_flag`**, que ninguém preenche: o banco calcula o valor
   a partir de `valid_to`, no mesmo comando que o altera. Vale `1` na linha vigente e
   `NULL` na fechada. É `TINYINT` (o menor inteiro), e as fechadas são `NULL`, não
   `0`, pelo motivo do item 2. Tentar escrever nela dá erro.

   ```sql
   current_flag TINYINT AS (IF(valid_to IS NULL, 1, NULL)) STORED
   ```

2. **`UNIQUE (pricing_group_id, plan_id, duration_minutes, current_flag)`.** O MySQL
   não considera um `NULL` igual a outro no `UNIQUE`, então as linhas fechadas podem
   se repetir à vontade (histórico), e só o `1` (vigente) não pode aparecer duas
   vezes.

| grupo | plano | duração | `valid_to` | `current_flag` | |
|---|---|---|---|---|---|
| Ind. Padrão | Evolução | 60 | 2026-10-01 | NULL | histórico ✅ |
| Ind. Padrão | Evolução | 60 | 2026-10-15 | NULL | histórico ✅ |
| Ind. Padrão | Evolução | 60 | NULL | 1 | vigente ✅ |
| Ind. Padrão | Evolução | 60 | NULL | 1 | ❌ `Duplicate entry ... for key 'uk_duration_price_current'` |

No reajuste, a ordem importa: a linha antiga é fechada no banco **antes** do
`INSERT` da nova (`saveAndFlush` no `PricingService`), senão as duas teriam `1` por
um instante. Uma violação que escapa das checagens do código chega à API como 409
genérico ("conflito de integridade"); a causa exata aparece no log da aplicação.

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
  O Maven compila normalmente; vale o `./mvnw -q compile` (em `backend/`).
  Depois da mudança para o monorepo, se a extensão continuar procurando o
  `pom.xml` na raiz (ou recriar uma pasta `target/` vazia na raiz), use
  **Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"**.
- **Sem acesso ao Maven Central e ao GitHub por HTTPS** (erro de certificado
  PKIX/SSL, provável proxy da rede): só o cache local do Maven funciona (plugins
  novos não baixam) e o `git push` pode falhar. Tentar outra rede ou
  `git config --global http.sslBackend schannel`. **Nunca desligar a verificação
  SSL.**
- **Arquivos que voltaram sozinhos para versões antigas** (já aconteceu com
  `TechniqueController` e `domain-model.md`): suspeita de OneDrive sincronizando a
  Área de Trabalho ou de aba antiga do VS Code salva por cima.
