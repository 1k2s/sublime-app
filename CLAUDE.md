# Sublime Fisioterapia — instruções para o Claude Code

Sistema de controle de atendimento e repasse de honorários da clínica Sublime
Fisioterapia (Java + Spring Boot, monolito modular, MySQL, ReactJS).

Este arquivo é só um índice. O contexto do projeto está em `docs/`:

| Documento | Conteúdo | Quando ler |
|---|---|---|
| [`docs/architecture.md`](docs/architecture.md) | arquitetura (monolito modular), convenção de idioma, mapa de módulos, princípios de modelagem (DDD: onde cada validação mora, agregados), padrão de construção por camada, formato dos erros, stack e ambiente, **estado atual, próximos passos**, pendências técnicas e melhorias futuras | **sempre**, antes de qualquer tarefa |
| [`docs/domain-model.md`](docs/domain-model.md) | diagrama ER, entidades, atributos e regras de negócio, com o motivo de cada decisão e as alternativas descartadas; **pendências de negócio** (ex: fim da seção `Consultation`); módulos ainda não modelados; dados da clínica | antes de mexer em qualquer entidade, regra de negócio ou schema |

Ao alterar uma regra de negócio ou decisão de arquitetura, atualize o documento
correspondente no mesmo PR — eles são a fonte de verdade do time.
