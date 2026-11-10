# Documentação

Documentação do produto, uma só para o backend e o frontend. É a fonte da verdade do
time: toda mudança atualiza os documentos no mesmo PR.

## Mapa

| Documento | O que tem | Quando ler |
|---|---|---|
| [`architecture.md`](architecture.md) | como o sistema é construído: convenção de idioma, mapa de módulos, princípios de modelagem (DDD: onde cada validação mora, agregados), padrão de construção por camada, formato dos erros, segurança; seção **"Frontend"** com princípios (o backend é a fonte da verdade), estrutura de pastas, mapa de módulos, camada `api/`, estado, estilo, rotas e sessão | **sempre**, antes de qualquer tarefa (a seção "Frontend" antes de mexer em `frontend/`) |
| [`domain-model.md`](domain-model.md) | regras de negócio **implementadas** (valem para os dois lados: o front as exibe, não as decide): diagrama ER, entidades, atributos e regras, com o "por quê" em uma linha e o link para o ADR | antes de mexer em qualquer entidade, regra de negócio, schema ou tela que mostre essas regras |
| [`roadmap.md`](roadmap.md) | estado atual, próximos passos, pendências técnicas, melhorias futuras e o que ainda não foi modelado; seção "Frontend" com o mesmo para o front | no início de uma tarefa nova, para saber onde o projeto está |
| [`specs/`](specs/README.md) | feature em andamento: regras decididas e ainda não implementadas, desenho e tarefas; some quando a feature termina | antes de trabalhar na feature |
| [`adr/`](adr/README.md) | decisões: contexto, alternativas descartadas e consequências | antes de propor mudar uma decisão já tomada |
| [`development.md`](development.md) | stack, como rodar, variáveis de ambiente, banco e migrations, dados de teste e catálogo da clínica, problemas conhecidos do ambiente | ao rodar, configurar ou cadastrar dados |

## Onde registrar cada coisa

| O quê | Onde |
|---|---|
| Regra implementada | `domain-model.md` (ou `architecture.md`, se for padrão de construção). Só o que o código faz hoje |
| Regra decidida, ainda não implementada | a spec da feature, em `specs/` |
| Decisão com alternativas descartadas ou difícil de reverter | um ADR novo em `adr/` (no frontend, só para decisões grandes). Decisão de feature em andamento fica na spec e vira ADR quando a feature termina |
| Status, tarefa, pendência, ideia | `roadmap.md` (do front, na seção "Frontend"). Nunca em `architecture.md`/`domain-model.md` |
| Feature concluída | regras da spec para o `domain-model.md`, decisões para ADRs, spec removida, `roadmap.md` atualizado |

As regras de escrita (como citar documentos nos comentários do código, títulos sem data,
ADR substituído em parte) e a rotina de conferência ao concluir uma tarefa estão no
[`CLAUDE.md`](../CLAUDE.md), que guia o agente. A conferência automática dos documentos
roda com `node scripts/check-docs.mjs`, na raiz do repositório.
