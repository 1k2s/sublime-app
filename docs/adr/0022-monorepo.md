# 0022 — Monorepo: backend e frontend no mesmo repositório

- **Status:** aceito
- **Data:** 2026-10-29

## Contexto

O frontend (React + Vite) começou num repositório separado (`sublime-app-front`).
Na prática, quase toda funcionalidade mexe nos dois lados (ex: o endpoint de
reativação e o botão "Reativar"), o que exigia dois PRs ligados. A documentação do
produto (`docs/`: regras, ADRs, specs, roadmap) ficava só no backend, e o agente de
desenvolvimento não enxergava o outro lado. O time é pequeno e o produto é um só. O
próprio `CLAUDE.md` do front previa "reavaliar o monorepo se o vai-e-vem entre dois
PRs começar a atrapalhar".

## Decisão

Um repositório só (`sublime-app`), com:

```
backend/    Spring Boot (Maven)
frontend/   React + Vite (npm)
docs/       documentação do produto, vale para os dois
```

- O backend foi movido com `git mv`, preservando o histórico de cada arquivo.
- O frontend foi importado com `git subtree add --prefix=frontend` a partir da branch
  `dev` do repositório antigo, preservando os commits do time. Para ver o histórico de
  um arquivo do front anterior à importação: `git log --follow -- frontend/<arquivo>`.
- Cada lado mantém o próprio `.gitignore`. A documentação é uma só, em `docs/`
  (o front ganha uma seção "Frontend" no `architecture.md` e no `roadmap.md`), e as
  instruções do agente ficam no `CLAUDE.md` da raiz.
- O `.github` é um só, na raiz (o GitHub ignora `.github` em subpastas). Workflows de
  CI, quando existirem, ficam separados por arquivo (`backend.yml`, `frontend.yml`) e
  filtrados por pasta (`paths: ['backend/**']`).
- O repositório antigo do front é arquivado (somente leitura).

## Alternativas descartadas

- **Dois repositórios + contrato pelo OpenAPI:** resolve a sincronia de tipos, mas
  mantém os PRs duplos, a documentação num lado só e o agente sem visão do outro lado.
  Faz mais sentido com times grandes ou deploys muito independentes.
- **`git submodule`:** dois repositórios disfarçados de um. Exige comandos extras para
  clonar e atualizar e confunde quem não conhece.
- **Backend na raiz + `frontend/`:** mexeria menos agora, mas misturaria os arquivos do
  Maven na raiz com a pasta do front.
- **Importar o front copiando os arquivos (ou com `--squash`):** perderia o histórico
  de commits do time.

## Consequências

- Uma funcionalidade = um PR, com o endpoint e a tela revisados juntos.
- Os comandos do backend rodam em `backend/` (`./mvnw ...`) e os do front em
  `frontend/` (`npm ...`); o `docker-compose.yml` continua na raiz.
- Branches abertas antes da migração não aplicam mais (os caminhos mudaram); a
  migração foi feita com os PRs dos dois lados fechados.
- Bancos e ambientes locais não mudam; só os caminhos dos comandos.
