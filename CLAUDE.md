# Sublime Fisioterapia — instruções para o Claude Code

Sistema de controle de atendimento e repasse de honorários da clínica Sublime
Fisioterapia (Java + Spring Boot, monolito modular, MySQL, React + Vite).

## Estrutura do repositório

Monorepo ([ADR 0022](docs/adr/0022-monorepo.md)):

- `backend/` — API Spring Boot (Maven). Comandos rodam em `backend/` (`./mvnw ...`).
- `frontend/` — interface React + Vite (npm). Comandos rodam em `frontend/`
  (`npm ...`).
- `docs/` — documentação do produto, uma só para os dois lados.
- `scripts/` — scripts de apoio (ex: `check-docs.mjs`, ver "Ao concluir uma tarefa").

## Documentação

O contexto do projeto está em `docs/`; o mapa completo (o que cada documento tem) está
em [`docs/README.md`](docs/README.md). Quando ler cada um:

| Documento | Quando ler |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | **sempre**, antes de qualquer tarefa (a seção "Frontend" antes de mexer em `frontend/`) |
| [`docs/domain-model.md`](docs/domain-model.md) | antes de mexer em qualquer entidade, regra de negócio, schema ou tela que mostre essas regras |
| [`docs/roadmap.md`](docs/roadmap.md) | no início de uma tarefa nova, para saber onde o projeto está |
| [`docs/specs/`](docs/specs/README.md) | antes de trabalhar na feature |
| [`docs/adr/`](docs/adr/README.md) | antes de propor mudar uma decisão já tomada |
| [`docs/development.md`](docs/development.md) | ao rodar, configurar ou cadastrar dados |

## Onde registrar cada coisa

Atualize a documentação **no mesmo passo** da mudança (são a fonte de verdade do
time e entram no mesmo PR):

- **Regra implementada** → `domain-model.md` (ou `architecture.md`, se for padrão de
  construção, do backend ou do front). Só o que o código faz hoje.
- **Regra decidida, ainda não implementada** → spec da feature em `docs/specs/`.
- **Decisão com alternativas descartadas ou difícil de reverter** → ADR novo em
  `docs/adr/` (ADR aceito não é editado; uma decisão nova o substitui). Decisão de
  feature em andamento fica na spec e vira ADR quando a feature é concluída. **No
  frontend, ADR só para decisões grandes** (ex: estratégia de rotas, autenticação ou
  estado); ajustes do dia a dia não são documentados em ADR.
- **Status, tarefa, pendência, ideia** → `roadmap.md` (do front, na seção
  "Frontend"). Nunca em `architecture.md`/`domain-model.md`.
- **Feature concluída** → regras da spec para o `domain-model.md`, decisões para
  ADRs, spec removida, `roadmap.md` atualizado.

Ao escrever:

- **Comentário no código cita um ADR ou uma seção de documento de referência** (ex:
  `Ver o ADR 0026`, `ver "Quem lança" em Consultation, no domain-model`), **nunca uma
  spec** (é temporária: some quando a feature termina) nem o roadmap.
- **Títulos sem data:** a data vai numa linha abaixo do título, para a âncora do índice
  não mudar.
- **Spec nova segue o modelo** do [`docs/specs/README.md`](docs/specs/README.md).
- **ADR que substitui outro em parte:** o antigo recebe o status "aceito (em parte
  substituído por NNNN)"; o texto dele não é editado.

## Regras de trabalho neste projeto

- **Consulte o projeto antes de sugerir e antes de escrever.** Toda recomendação
  técnica parte de como os módulos existentes já resolvem o mesmo caso (cite o
  precedente em arquivo:linha) e dos princípios do `architecture.md`. Se for caso
  novo, sem precedente, diga isso.
- **A documentação não é dogma: busque sempre a melhor forma.** O objetivo é um software
  robusto e rápido. Avalie cada solução pelo mérito (estrutura, desempenho, robustez,
  simplicidade de manter), não só por estar documentada ou por ser o que o código já faz.
  Ao encontrar uma forma melhor, mesmo contrária ao `architecture.md` ou a um precedente,
  **analise e sugira** (o que muda, por quê, o custo), com uma recomendação. Aprovada a
  sugestão, a documentação é atualizada no mesmo passo. Se várias telas ou módulos resolvem
  o mesmo caso de jeitos diferentes, compare e proponha o melhor de cada um como padrão.
- **Validação de entidade em método privado com nome descritivo** (`validateX`,
  `requireX`), nunca `if` direto no corpo do construtor ou da fábrica (princípio 7).
- **Mensagens ao usuário em português**; nomes de código em inglês (convenção de
  idioma no `architecture.md`).
- **Nomes intuitivos:** métodos booleanos como pergunta (`is...`, `has...`);
  validações com nome que diz o que validam.
- **Regra de negócio não se presume:** em dúvida, pergunte. Em escolha técnica, dê
  uma recomendação, não só as opções.
- **Compile depois das mudanças** (`./mvnw -q compile` em `backend/`; erros do Lombok
  na IDE são falsos, ver `development.md`) e diga claramente o que foi ou não testado.
- Commit e push só quando pedido.
- **PRs vão para a `dev`, com "merge commit"** (não "squash" nem "rebase"): os commits da
  branch ficam no histórico.
- **Ao pausar uma feature** (troca de computador ou fim da sessão no meio do trabalho):
  atualize a spec antes do commit, com o que foi feito, o que falta (tarefas da seção 6) e as
  decisões ainda não registradas (seção 1, Status). A sessão seguinte continua pelos
  arquivos, não pela conversa.
- **Comandos de consulta rodam sem pedir permissão:** os que só leem ou verificam, sem
  alterar código, banco, git ou processos.
  - **Verificação:** os scripts de `scripts/` que só conferem (ex: `node scripts/check-docs.mjs`),
    `./mvnw -q compile`, `npm run build` e `npm run lint`.
  - **Estado do projeto:** `git status`, `git branch`, `git log`, `git diff`, `git show` e
    `git fetch`; consultas `GET` à API local.
  - **Continuam pedindo permissão** os que alteram algo: `git add`, `commit`, `push`,
    `checkout`, `merge` e `rm`; instalar dependências; subir ou derrubar processos (backend,
    front, Docker); gravar no banco (`POST`, `PUT`, `DELETE` na API).

### No frontend

- **O backend é a fonte da verdade:** o front não reimplementa regra de negócio (preço,
  saldo, comissão, repasse, vigência, de qual contrato sai a sessão); mostra o que a API
  resolve. Validação no front (Zod) é só de formato.
- **Contrato da API antes da tela:** confira o endpoint e o DTO no Swagger/backend antes
  de escrever ou alterar uma chamada. Campos com os mesmos nomes da API.
- **Chamadas só pela camada `api/`** (`xxxApi.js` + hooks do TanStack Query); pages e
  components nunca usam o `apiClient` direto.
- **Respeite o mapa de módulos** da seção "Frontend" do `architecture.md` (o mesmo do backend) e
  importe com o alias `@/`.
- **Confira depois das mudanças:** `npm run build` e `npm run lint` (em `frontend/`).
- **Mudança visual ou de layout: prévia antes do código.** Quando o usuário pedir uma mudança
  de visual, layout ou fluxo de tela (ou houver mais de um jeito razoável de fazê-la), monte
  primeiro **prévias estáticas** (2 ou 3 versões, com as cores e os dados do projeto) numa
  página de prévia para ele comparar, e só escreva o código da versão escolhida. Evita as idas
  e vindas no código. Ajuste pequeno e já definido (ex: trocar uma cor, um texto) vai direto.

## Ao concluir uma tarefa

Antes de dizer que terminou, confira (e diga o que foi ou não conferido):

1. **Build:** `./mvnw -q compile` em `backend/`; `npm run build` e `npm run lint` em
   `frontend/`.
2. **Documentação:** `node scripts/check-docs.mjs` na raiz (índices, links entre
   arquivos e specs citadas que não existem mais, nos docs e nos comentários do código).
3. **Nomes removidos ou renomeados** na tarefa (spec, arquivo, classe, método, endpoint,
   coluna): procure cada um nos `docs/` e nos comentários do código.
4. **Referência só com o presente:** procure "ainda não", "hoje", "em andamento" e
   "futuramente" no `architecture.md` e no `domain-model.md`; cada ocorrência é uma regra
   que mudou ou um status fora do lugar.
5. **Swagger:** os textos das anotações (`@Operation`) batem com as regras alteradas.
6. **ADRs:** status dos substituídos atualizado; numeração sequencial.
7. **Roadmap:** tire o que foi resolvido; o que a spec deixou para depois vira item
   antes de a spec ser removida.
8. **Ambiente:** variável nova entra no `backend/.env.example`; migration editada
   ([ADR 0028](docs/adr/0028-migrations-editaveis-ate-versao-estavel.md)) vira aviso na
   descrição do PR ("zerem o banco local").
9. **Sugira o commit e a descrição do PR:**
   - **commit em uma linha**, no padrão do histórico (`feat:`, `fix:`, `docs:`,
     `chore:` + descrição em português);
   - **descrição do PR com tudo:** o que mudou e por quê, as decisões tomadas, o que foi
     testado e o que não foi, e os avisos ao time.

   Só sugira: commit e push são feitos pelo usuário.
