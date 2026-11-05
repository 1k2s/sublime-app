# Sublime Fisioterapia — instruções para o Claude Code

Sistema de controle de atendimento e repasse de honorários da clínica Sublime
Fisioterapia (Java + Spring Boot, monolito modular, MySQL, React + Vite).

## Estrutura do repositório

Monorepo ([ADR 0022](docs/adr/0022-monorepo.md)):

- `backend/` — API Spring Boot (Maven). Comandos rodam em `backend/` (`./mvnw ...`).
- `frontend/` — interface React + Vite (npm). Comandos rodam em `frontend/`
  (`npm ...`).
- `docs/` — documentação do produto, uma só para os dois lados.

## Documentação

O contexto do projeto está em `docs/`:

| Documento | Conteúdo | Quando ler |
|---|---|---|
| [`docs/architecture.md`](docs/architecture.md) | como o sistema é construído: convenção de idioma, mapa de módulos, princípios de modelagem (DDD: onde cada validação mora, agregados), padrão de construção por camada, formato dos erros; seção **"Frontend"** com princípios (o backend é a fonte da verdade), estrutura de pastas, mapa de módulos, camada `api/`, estado, estilo, rotas e sessão | **sempre**, antes de qualquer tarefa (a seção "Frontend" antes de mexer em `frontend/`) |
| [`docs/domain-model.md`](docs/domain-model.md) | regras de negócio **implementadas** (valem para os dois lados: o front as exibe, não as decide): diagrama ER, entidades, atributos e regras, com o "por quê" em uma linha e link para o ADR | antes de mexer em qualquer entidade, regra de negócio, schema ou tela que mostre essas regras |
| [`docs/roadmap.md`](docs/roadmap.md) | estado atual, próximos passos, pendências técnicas, melhorias futuras e o que ainda não foi modelado; seção "Frontend" com o mesmo para o front | no início de uma tarefa nova, para saber onde o projeto está |
| [`docs/specs/`](docs/specs/) | feature em andamento: regras decididas e ainda não implementadas, desenho e tarefas | antes de trabalhar na feature |
| [`docs/adr/`](docs/adr/README.md) | decisões: contexto, alternativas descartadas e consequências | antes de propor mudar uma decisão já tomada |
| [`docs/development.md`](docs/development.md) | stack, como rodar, banco e reset do schema, dados de teste e catálogo da clínica, problemas conhecidos do ambiente | ao rodar, configurar ou cadastrar dados |

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

## Regras de trabalho neste projeto

- **Consulte o projeto antes de sugerir e antes de escrever.** Toda recomendação
  técnica parte de como os módulos existentes já resolvem o mesmo caso (cite o
  precedente em arquivo:linha) e dos princípios do `architecture.md`. Se for caso
  novo, sem precedente, diga isso.
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

### No frontend

- **O backend é a fonte da verdade:** o front não reimplementa regra de negócio (preço,
  saldo, comissão, repasse, vigência, de qual contrato sai a sessão); mostra o que a API
  resolve. Validação no front (Zod) é só de formato.
- **Contrato da API antes da tela:** confira o endpoint e o DTO no Swagger/backend antes
  de escrever ou alterar uma chamada. Campos com os mesmos nomes da API.
- **Chamadas só pela camada `api/`** (`xxxApi.js` + hooks do TanStack Query); pages e
  components nunca usam o `httpClient` direto.
- **Respeite o mapa de módulos** da seção "Frontend" do `architecture.md` (o mesmo do backend) e
  importe com o alias `@/`.
- **Confira depois das mudanças:** `npm run build` e `npm run lint` (em `frontend/`).
