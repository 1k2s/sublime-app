# Handoff — Sublime Fisioterapia

> Uso pessoal para iniciar uma sessão do Claude Code. **Este arquivo guarda só as
> minhas preferências de interação.** Todo o contexto do projeto — decisões,
> pendências, estado atual, próximos passos, padrões e ambiente — está em
> `docs/architecture.md` e `docs/domain-model.md` (leia os dois antes de começar;
> o estado atual e os próximos passos estão no fim do `architecture.md`).
> Atualizado em 2026-10-19.

## Como eu gosto de trabalhar

- **Explique antes de alterar.** Para cada mudança, diga o que muda e por quê
  (em 1–3 frases) antes de aplicar. Passos pequenos; nada de várias alterações em
  lote sem explicação.
- **Quero entender o código antes de ele ser escrito.** Em código novo, apresente o
  desenho (e, se eu pedir, o código comentado e como ele é usado) antes de criar os
  arquivos.
- **Quando eu mandar algo, pare e leia.** Mensagem minha ou motivo de recusa de
  uma edição pode ser complemento ou dúvida — responda antes de seguir. Nunca
  dispare várias edições depois de uma recusa. Dúvida ≠ pedido de mudança.
- **Ao apontar um problema, mostre o código.** Explicação + trecho exato
  (arquivo:linha) + um exemplo concreto do que dá errado.
- **Análises longas em partes.** Apresente uma parte, espere meu retorno e só
  então siga para a próxima.
- **Correção ≠ melhoria ≠ feature.** Numa rodada de correções, faça só a mudança
  mínima. Melhorias você lista para eu decidir — não aplica junto.
- **Decisões de negócio são minhas.** Quando houver dúvida de regra, pergunte; não
  presuma. Quando houver escolha técnica, dê uma recomendação, não só as opções.
- **Consulte o projeto antes de sugerir — e antes de escrever.** Toda recomendação
  técnica parte de como os módulos existentes já resolvem o mesmo caso (citar o
  precedente em arquivo:linha) e dos princípios de monolito modular e DDD do
  `architecture.md`. Se for caso novo, sem precedente, diga isso.
- **Nomes intuitivos.** Métodos booleanos como pergunta (`is...`, `has...`);
  validações com nome que diz o que validam.
- **Documentação sempre em dia.** Decisão tomada vai para o `domain-model.md` /
  `architecture.md` no mesmo passo; pendência fica registrada como pendência.
- **Handoff só é atualizado quando eu pedir** (uso para trocar de computador).
- Conversa e documentação em português; código em inglês; mensagens ao usuário
  (erros) em português.
- Compile (`./mvnw -q compile`) depois das mudanças e diga claramente o que foi
  ou não testado.

## Onde cada coisa roda

- Desenvolvimento, execução e testes acontecem no meu computador (Claude Code na
  extensão do VS Code, com Docker/MySQL).
- A sessão no Claude Code web é só para conversar/planejar de outro computador —
  não tente subir a aplicação ou o banco por lá.

## Versionamento deste arquivo

- Fica fora do repositório pelo `.git/info/exclude` (linha `docs/handoff.md`).
- Para levar a outro computador: `git add -f docs/handoff.md` na primeira vez da
  branch (depois o `git add` normal já pega as alterações).
- Antes de abrir o PR: `git rm --cached docs/handoff.md` + commit (sai do
  repositório, continua no disco). Some do histórico da `dev` só com merge squash.
