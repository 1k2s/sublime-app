# Ambiente de desenvolvimento

Como rodar, preparar o banco e cadastrar os dados do sistema localmente (backend e
frontend). Para a arquitetura e as regras de negócio, ver
[`architecture.md`](architecture.md) (com a seção "Frontend") e
[`domain-model.md`](domain-model.md).

## Índice <!-- omit from toc -->

- [Stack](#stack)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Rodando localmente](#rodando-localmente)
- [Rodando os testes](#rodando-os-testes)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Frontend e API](#frontend-e-api)
- [Banco e schema (Flyway)](#banco-e-schema-flyway)
  - [Criando uma migration](#criando-uma-migration)
  - [Banco local antigo](#banco-local-antigo)
  - [Constraints do banco](#constraints-do-banco)
- [Dados de teste](#dados-de-teste)
- [Catálogo atual da clínica (dados para o cadastro inicial)](#catálogo-atual-da-clínica-dados-para-o-cadastro-inicial)
- [Fluxo de git](#fluxo-de-git)
- [Homologação (VPS)](#homologação-vps)
- [Problemas conhecidos do ambiente](#problemas-conhecidos-do-ambiente)

## Stack

- **Spring Boot 4.1.1**, **Java 25**, **Jackson 3** (pacote `tools.jackson`, não
  `com.fasterxml`), Lombok, springdoc (Swagger).
- **MySQL 8.4.11** (8.4 LTS, [ADR 0052](adr/0052-versao-estavel-do-banco.md)) via `docker-compose.yml` (versão exata, a mesma dos testes; container `mysql-dev`, porta **3307**,
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
| `deploy/` | os ambientes no ar: `deploy/homologacao/` (o `docker-compose` e o modelo dos segredos da VPS; ver "Homologação") | `deploy/homologacao/` (`docker compose ...`) |
| raiz | `docker-compose.yml` (o MySQL local), `CLAUDE.md`, `README.md` | `docker compose ...` |

## Rodando localmente

1. Subir o banco (na raiz): `docker compose up -d`.
2. **Na primeira vez:** criar o `backend/.env` a partir do `backend/.env.example` (ver
   "Variáveis de ambiente" abaixo).
3. Subir a API (em `backend/`): `./mvnw spring-boot:run`.
   Swagger: `http://localhost:8080/swagger-ui.html` (OpenAPI em `/api-docs`). Para
   chamar as rotas pelo Swagger, faça o login em `POST /api/auth/login`, copie o `token`
   e cole em **Authorize** (cadeado, no topo).
4. Subir a interface (em `frontend/`): na primeira vez, criar o `frontend/.env` a partir
   do `frontend/.env.example` (ver "Frontend e API" abaixo) e rodar `npm ci` (instala as
   dependências do `package-lock.json`); depois, `npm run dev`. Abre em
   `http://localhost:5173`.

Para conferir se o backend compila: `./mvnw -q compile` (em `backend/`). É a
referência quando a IDE acusa erro (ver "Problemas conhecidos"). No front:
`npm run build` e `npm run lint` (em `frontend/`).

## Rodando os testes

Em `backend/`. Padrões de como escrever: "Testes do backend", no
[`architecture.md`](architecture.md).

| O quê | Comando |
|---|---|
| Todos | `./mvnw test` |
| Uma classe | `./mvnw test -Dtest=PercentageTest` |
| Uma classe com `@Nested` | `./mvnw test -Dtest='ProviderEntityTest*'` (o `*` inclui as classes internas) |
| Um método | `./mvnw test -Dtest=PercentageTest#appliesPercentageWithHalfUpRounding` |

- **O Docker precisa estar no ar** (o Docker Desktop): os testes de integração sobem um MySQL
  próprio em container ([ADR 0036](adr/0036-testes-de-integracao-com-testcontainers.md)). O
  `docker compose up` não é necessário, e o banco de desenvolvimento (`sublime_db`) nunca é
  tocado. A primeira execução baixa a imagem do MySQL.
- O perfil `test` (`src/test/resources/application-test.properties`) tem o segredo do JWT e o
  admin padrão próprios. O `backend/.env` também é lido nos testes, mas o perfil `test`
  sobrescreve as chaves que a aplicação usa. Ao ler uma propriedade nova do `.env`, ponha o valor
  de teste dela no `application-test.properties`, para o teste não depender da máquina.
- Pelo VS Code (*Extension Pack for Java*): o ▶ ao lado de cada `@Test` ou a aba **Testing**.
- Resultado de cada classe em `backend/target/surefire-reports/`.

## Variáveis de ambiente

O backend lê três valores que **não ficam no git**:

| Variável | Para que serve | Obrigatória? |
|---|---|---|
| `JWT_SECRET` | segredo da assinatura do token de login, com **pelo menos 32 caracteres** | **sim**: sem ela, a aplicação não sobe |
| `ADMIN_EMAIL` | e-mail do admin padrão (e-mail válido) | só na **primeira subida** (banco ainda sem admin padrão) |
| `ADMIN_PASSWORD` | senha do admin padrão: no mínimo 8 caracteres, com letra maiúscula, letra minúscula, número e caractere especial (fora disso, a aplicação não sobe) | idem |

**Onde ficam:** no arquivo **`backend/.env`**, um por pessoa, ignorado pelo
`backend/.gitignore`. Para criar, copie o modelo versionado `backend/.env.example` e troque
os valores:

```powershell
Copy-Item backend/.env.example backend/.env   # depois edite o backend/.env
```

- O `application.properties` importa o arquivo (`spring.config.import`) procurando em `.env`
  e em `backend/.env`, relativos à pasta de onde a aplicação sobe: funciona tanto com o
  `./mvnw spring-boot:run` em `backend/` quanto com o Run da IDE, que sobe na raiz.
- Sem o arquivo, valem as variáveis de ambiente do sistema (é como funciona num servidor):
  `$env:JWT_SECRET = "..."` no PowerShell, por exemplo.
- **Nunca commite o `.env`.** Uma variável nova entra também no `.env.example`, com um
  valor de exemplo.

**Admin padrão:** na primeira subida (banco sem admin padrão), a aplicação cria um `ADMIN`
com `ADMIN_EMAIL` e `ADMIN_PASSWORD`, já com senha definitiva (sem primeiro acesso). Ele
não pode ser inativado nem mudar de perfil (ver `User` no
[`domain-model.md`](domain-model.md)). **Depois disso as duas variáveis são ignoradas:**
trocá-las no `.env` **não muda** o admin existente. Para trocar o e-mail ou a senha dele,
use a tela de Configurações (`PUT /api/users/me`) ou zere o banco. Sem as duas e sem admin
padrão, a aplicação sobe, mas ninguém consegue logar (um aviso aparece no log).

**Usuários criados antes da autenticação** (senha gravada em texto puro) não conseguem
logar: troque a senha pela API ou recrie o usuário.

## Frontend e API

- **Endereço da API:** variável `VITE_API_URL` no `frontend/.env`
  (`http://localhost:8080/api`). Depois de alterar, reinicie o `npm run dev`. O `.env` é
  ignorado pelo `frontend/.gitignore`; para criar, copie o modelo versionado:
  `Copy-Item frontend/.env.example frontend/.env`. Variável nova entra também no
  `.env.example`. Sem o arquivo, a tela fica em branco e o console do navegador mostra
  "VITE_API_URL não definida" (o `apiClient` falha ao carregar, de propósito: sem o endereço, as
  chamadas iriam para o próprio Vite, que responde com o `index.html`).
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
| `@fontsource/cormorant-garamond` | fonte da marca (nome "Sublime" no menu), servida pelo próprio app |
| `sweetalert2` | alertas de erro da API (`shared/utils/alert.js`) |
| `echarts` | gráficos (import modular, pelo `shared/components/Chart.jsx`; ver "Gráficos" no [`architecture.md`](architecture.md)) |

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
| `V4__seed_pricing_catalog.sql` | seed do catálogo atual da clínica: grupos, técnicas, planos, de-para e preços vigentes (pula o que já existe; [ADR 0030](adr/0030-seed-do-catalogo-por-migration.md)) |

### Criando uma migration

1. Mude a entidade (ex: um campo novo).
2. Crie o arquivo seguinte: `V5__descricao_em_ingles.sql` (dois sublinhados; o
   número define a ordem de execução).
3. Escreva o SQL (`ALTER TABLE ...`) e suba a aplicação: o Flyway aplica, e o
   `validate` confere se a entidade e o banco ficaram iguais.

Regras:
- **Uma migration já aplicada nunca é editada** (a versão estável,
  [ADR 0052](adr/0052-versao-estavel-do-banco.md)): corrigir ou mudar = um arquivo novo. O
  Flyway guarda um checksum de cada arquivo e recusa subir se ele mudar.
- **O dado que já existe precisa cumprir a regra nova:** uma coluna `NOT NULL` nova leva um
  `DEFAULT` (ou um `UPDATE` antes); um `CHECK` novo, o dado corrigido antes dele.
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
| `idx_` | índice só de desempenho | `idx_consultation_provider_launched` |

**`UNIQUE` é um índice.** O MySQL implementa todo `UNIQUE` como um índice: uma
estrutura ordenada, à parte da tabela, como o índice remissivo de um livro. É
consultando o índice que o banco descobre rapidamente se o valor já existe. Como
efeito colateral, ele acelera buscas por essas colunas. As foreign keys também ganham
índice automaticamente. Índice só de desempenho (`idx_`) entra onde se paga (ex: o
rendimento, sempre filtrado por prestador e período do lançamento): todo índice ocupa
espaço e deixa a gravação um pouco mais lenta.

**Os `CHECK` das regras das entidades (V1):** os valores em dinheiro, os percentuais, as
durações, as parcelas e o período do contrato. A entidade já valida (com a mensagem ao
usuário); o `CHECK` é a última barreira para um `UPDATE` manual ou um caminho que escape dela.

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
  A exceção é o **catálogo de preços** da clínica (módulo pricing: grupos, técnicas,
  planos, de-para e preços vigentes), que chega pela migration `V4` e respeita as
  mesmas regras (tipo × `sessionCount`/`termWeeks`, de-para entre famílias, tipo do
  plano × modelo do grupo, um preço vigente por combinação). Depois do seed, o
  catálogo muda só pela aplicação (reajuste pela tela de preço).
- **Ordem de cadastro** (cada passo depende dos anteriores):
  0. login como admin (`POST /api/auth/login`, com o admin criado pelo `ADMIN_EMAIL` /
     `ADMIN_PASSWORD` do `.env`) e o token em **Authorize** no Swagger: todas as rotas
     abaixo exigem o token;
  1. ~~grupos, técnicas, planos, de-para e preços~~: já criados pela `V4` (falta só
     o preço da Evolução Familiar no Individual Especializado e no Em Dupla, ainda
     não definido);
  2. usuários: nascem com **senha provisória**, então cada um faz o primeiro acesso (no
     login da tela, ou `POST /api/auth/first-access`) antes de usar o sistema;
  3. prestadores, vinculando um usuário já existente e ativo;
  4. pacientes (com a origem, se houver);
  5. contratos.

## Catálogo atual da clínica (dados para o cadastro inicial)

> Estes valores são os carregados pela migration `V4__seed_pricing_catalog.sql`. Um
> reajuste é feito pela tela de preço; a migration e esta tabela não são editadas
> para refleti-lo (a tabela registra o catálogo inicial).

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

## Homologação (VPS)

**`https://hom.sublimefisioterapia.com`**: um ambiente igual ao de produção, sem os dados da
clínica, para o time testar pela tela e treinar o fluxo de deploy antes do MVP
([ADR 0053](adr/0053-homologacao-com-caddy-e-imagens-no-ghcr.md)). Roda numa VPS da Hostinger
(Ubuntu, 1 vCPU, 4 GB), em três containers:

```
internet ──https──▶ web (Caddy + o front buildado; as únicas portas abertas: 80 e 443)
                      ├── /                                   → o front (arquivos estáticos)
                      └── /api, /swagger-ui, /api-docs        → backend:8080 (rede interna)
                                                                   └── mysql:3306 (rede interna)
```

| Arquivo | O que é |
|---|---|
| `backend/Dockerfile` | a imagem do backend: compila com o JDK, roda com o JRE e um usuário sem privilégios |
| `frontend/Dockerfile` | a imagem `web`: builda o React (`VITE_API_URL=/api`) e o coloca no Caddy |
| `frontend/Caddyfile` | o porteiro: o HTTPS (certificado do Let's Encrypt, renovado sozinho) e o roteamento |
| `deploy/homologacao/docker-compose.yml` | os três serviços, com as imagens do registro (o que roda na VPS) |
| `deploy/homologacao/docker-compose.build.yml` | acrescenta o `build`: monta as imagens a partir do código |
| `deploy/homologacao/.env.example` | o modelo dos segredos (o `.env` de verdade só existe na VPS) |

**Testar na máquina local** (em `deploy/homologacao/`, com um `.env` local com
`SITE_ADDRESS=http://localhost` e senhas de teste; as portas 80 e 443 livres):
```
docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.build.yml down -v
```
O site abre em `http://localhost`, com o banco vazio (só o catálogo da V4); o `down -v` derruba
tudo e apaga o banco de teste. O `mysql-dev` não é tocado.

**Publicar as imagens** (à mão, até o CI/CD), no registro do GitHub (GHCR):
1. Montar as imagens com o comando de teste acima (`--build`): elas já saem com o nome do registro
   (`ghcr.io/1k2s/sublime-backend:dev` e `ghcr.io/1k2s/sublime-web:dev`).
2. Um token **classic** do GitHub com só o `write:packages` (o fine-grained não funciona com o
   GHCR), e `docker login ghcr.io -u SEU_USUARIO` (o token no lugar da senha).
3. `docker push ghcr.io/1k2s/sublime-backend:dev` e `docker push ghcr.io/1k2s/sublime-web:dev`.

As imagens são públicas (o repositório é público), e a VPS as baixa sem login. Por isso, **nunca
há segredo dentro de uma imagem**: as senhas entram pelo `.env` da VPS, na hora de rodar.

**Subir ou atualizar na VPS** (`ssh deploy@IP_DA_VPS`, o IP no painel da Hostinger), na pasta
`~/sublime-homolog`, que tem só o `docker-compose.yml` e o `.env`:
```
docker compose pull      # baixa as imagens novas
docker compose up -d     # recria só os containers que mudaram
docker compose ps        # os três "Up", o mysql "(healthy)"
docker compose logs --tail 40 backend
```
Quando o `docker-compose.yml` mudar no repositório, copie de novo, **do seu PC**:
`scp deploy/homologacao/docker-compose.yml deploy@IP_DA_VPS:~/sublime-homolog/`. As migrations
novas rodam sozinhas quando o backend sobe ([ADR 0052](adr/0052-versao-estavel-do-banco.md)).

**O `.env` da VPS** foi criado uma vez, lá mesmo, com os segredos gerados pelo
`openssl rand` (eles nunca passam por outra máquina), e só o `deploy` o lê (`chmod 600`). O admin
padrão é criado **só na primeira subida** do banco: gerar o `.env` de novo não muda a senha do
admin que já existe (troque pela tela de Configurações).

**O acesso à VPS:**
- Pelo SSH, só com chave (a senha está desligada) e com o usuário `deploy` (grupos `sudo` e
  `docker`); o `root` não entra pelo SSH. **Uma chave por pessoa:** a pública vai numa linha do
  `~/.ssh/authorized_keys` do `deploy`; para tirar alguém, apague só a linha dele. O time não
  precisa de SSH: o deploy é pelo registro (e, depois, pelo CI/CD).
- Firewall (`ufw`): só as portas 22, 80 e 443.
- Emergência (a chave perdida): o terminal web da Hostinger, com a senha do `root`.

**Regras:**
- **Só o `web` publica portas.** O Docker abre as portas publicadas por fora do `ufw`: um
  `ports` no `mysql` ou no `backend` os deixaria abertos para a internet.
- **O backend lê o `X-Forwarded-Proto`** (`SERVER_FORWARD_HEADERS_STRATEGY: native` no compose). O
  Caddy repassa as chamadas por `http`, e o cabeçalho diz que, do lado de fora, elas vieram por
  `https`. Sem ele, o login pelo navegador dá 403: o Spring vê o site `https` chamando uma API
  `http` e recusa como CORS.

## Problemas conhecidos do ambiente

Não são do código:

- **Erros falsos do Lombok no VS Code** ("Can't initialize javac processor...",
  `getX()` não encontrado): o processador do Lombok falha dentro da extensão Java.
  O Maven compila normalmente; vale o `./mvnw -q compile` (em `backend/`).
  Depois da mudança para o monorepo, se a extensão continuar procurando o
  `pom.xml` na raiz (ou recriar uma pasta `target/` vazia na raiz), use
  **Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"**.
- **Erro de certificado no Maven ou no Git** ("PKIX path building failed",
  "certificate_unknown"): só o cache local do Maven funciona (dependências e plugins novos
  não baixam) e o `git fetch`/`push` pode falhar. **Causa** (confirmada em 2026-11-07): um
  antivírus com inspeção de HTTPS (ex: **Avast Web/Mail Shield**) ou um firewall da rede
  reassina as conexões com um certificado próprio, que o Windows conhece e o Java e o Git
  não. Para confirmar, o emissor mostra quem assinou:
  `echo | openssl s_client -connect repo.maven.apache.org:443 2>/dev/null | openssl x509 -noout -issuer`
  (aparece o antivírus no lugar de uma autoridade pública). **Solução**, usando os
  certificados do Windows, só na sua máquina:
  - Maven: a variável de ambiente do usuário `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`
    (`[Environment]::SetEnvironmentVariable('MAVEN_OPTS', '-Djavax.net.ssl.trustStoreType=Windows-ROOT', 'User')`
    no PowerShell; reabra o VS Code e os terminais). Não versionar: o `Windows-ROOT` só existe
    no Windows e quebraria o build no Linux e no CI.
  - Git: `git config --global http.sslBackend schannel`.

  **Nunca desligar a verificação SSL** (nem a do antivírus).

  O mesmo antivírus faz o certificado da homologação aparecer, nessa máquina, como emitido por
  ele (e não pelo Let's Encrypt). O certificado do servidor está certo: para conferir, veja o
  cadeado de outro aparelho ou o log do Caddy na VPS
  (`docker compose logs web | grep -i "certificate obtained"`).
- **Animações do app não aparecem** (menu, entrada das telas): o sistema pede movimento
  reduzido, e o app respeita (`prefers-reduced-motion`). No Windows, é a opção
  **Configurações → Acessibilidade → Efeitos visuais → Efeitos de animação**
  (`Win+R` → `ms-settings:easeofaccess-visualeffects`); reabra o navegador depois. Para testar
  sem mudar o sistema: DevTools → ⋮ → More tools → **Rendering** → *Emulate CSS media feature
  prefers-reduced-motion*.
- **Arquivos que voltaram sozinhos para versões antigas** (já aconteceu com
  `TechniqueController` e `domain-model.md`): suspeita de OneDrive sincronizando a
  Área de Trabalho ou de aba antiga do VS Code salva por cima.
