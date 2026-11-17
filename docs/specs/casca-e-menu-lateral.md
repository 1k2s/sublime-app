# Spec — Casca do app e menu lateral

Novo visual da casca do app: o menu lateral verde que abre no hover, o painel claro do
conteúdo e uma largura única para todas as telas.

Iniciada em 2026-11-17.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo e escopo](#2-objetivo-e-escopo)
- [3. Regras](#3-regras)
- [4. Desenho do backend](#4-desenho-do-backend)
- [5. Desenho do frontend](#5-desenho-do-frontend)
- [6. Tarefas](#6-tarefas)
- [7. Casos de teste](#7-casos-de-teste)
- [8. Pendências e pontos abertos](#8-pendências-e-pontos-abertos)
- [9. Documentação ao concluir](#9-documentação-ao-concluir)
- [10. ADRs a registrar](#10-adrs-a-registrar)

## 1. Status

- **Branch:** `faeture/sidebar-style` (criada a partir da `dev`, com tudo dela). PR para a
  `dev`, com merge commit.
- **Implementado** (só frontend): tudo da seção 5. Conferido por `npm run build` e
  `npm run lint`. **Teste pela tela em andamento**, ajuste a ajuste (ver seção 7).
- **Documentos de referência ainda não atualizados:** só na conclusão (seção 9).

## 2. Objetivo e escopo

Parte do item "Nova identidade visual do portal" dos Próximos passos do
[`roadmap.md`](../roadmap.md). O visual saiu de prévias comparadas antes do código.

**Entra:**

- casca do app (área verde e painel claro do conteúdo) e animação de entrada das telas;
- menu lateral próprio, no lugar do `react-pro-sidebar` (removido do projeto);
- fonte da marca (Cormorant Garamond) no nome "Sublime";
- largura única das telas, definida pelo layout.

**Fica para depois:**

- **Cor do conteúdo:** o conteúdo continua com a paleta atual (`--color-primary`, `#5f7a58`);
  só o menu usa o verde novo (`#0BA17C`). Comparar as duas usando o menu novo e decidir.
- **Cores em variáveis CSS em todos os componentes** e **visual global em cada tela** (já no
  roadmap, item 3 dos Próximos passos).

## 3. Regras

Decisões de visual e de comportamento, com o motivo e o que foi testado e descartado.

**Casca**

- O verde do menu (degradê de `#0BA17C` até quase preto) fica só na casca (tokens
  `--shell-*` no `index.css`); o conteúdo segue com as `--color-*`. *Por quê:* comparar as
  duas cores com o menu pronto antes de mexer nas telas.
- O painel do conteúdo tem cantos de 24px e margem de 6px em cima, à direita e embaixo.
- *Descartados:* moldura de vidro em volta do app (testada na tela; só combinaria com tela de
  canto arredondado) e cantos arredondados na janela do app.

**Menu: forma e posição**

- Recolhido com 52px, aberto com 210px.
- **Os itens não mudam de lugar entre os modos:** a linha de cada item tem a mesma altura nos
  dois, e o ícone fica na mesma coluna (centro a 26px da borda). O logo do topo e o botão Sair
  ficam na mesma coluna. *Por quê:* no recolhido antigo os itens pulavam ao abrir e fechar.
  Para isso, as pílulas começam a 6px da borda (*descartado:* recolhido com 60px).
- O fundo de cada item é uma pílula translúcida em degradê; no recolhido, encolhe até um
  círculo de 40px em volta do ícone. O círculo que havia em volta do ícone saiu.
- A tela aberta é marcada por uma **pílula clara**, na cor do conteúdo, ligada ao painel por
  curvas côncavas, como uma aba. No aberto, tem a altura do item; no recolhido, é um
  retângulo arredondado que passa do círculo em cima e embaixo (*descartado:* virar círculo).
  O item ativo tem texto e ícone maiores.
- A **pílula translúcida do mouse e do Tab** acompanha o item apontado; ao voltar ao menu,
  aparece direto no item, sem deslizar do anterior.
- Topo: o logo (44px, silhueta branca) e, no aberto, o nome "Sublime". Rodapé: o botão Sair
  (círculo de 40px) e, no aberto, o e-mail e o perfil. O nome e os dados do usuário entram da
  direita ao abrir.
- No aberto, o logo da marca aparece em cinza no espaço livre entre os itens e o usuário.
- Janela baixa (até 760px de altura): linhas mais curtas, para o menu caber sem rolar.

**Menu: abrir e fechar**

- **Abre com o mouse em cima** (60ms de atraso) e **fecha ao sair** (150ms). *Por quê:* os
  atrasos evitam abrir com o mouse só de passagem e fechar num escorregão.
- Pelo teclado, o foco num item (Tab) abre; Tab para fora do menu fecha. Com o mouse em cima,
  a perda do foco não fecha: o Lançamento leva o foco para a barra ao abrir, e o menu fechava
  sob o mouse.
- **O menu aberto empurra o conteúdo**, que se ajusta durante a animação.
- *Descartados:* abrir pelo clique (seta de recolher e logo como botão de expandir, com a
  escolha lembrada no navegador); menu aberto **por cima** do conteúdo, nas duas formas
  testadas (reservando a faixa do menu aberto à esquerda, e deixando o menu cobrir parte do
  conteúdo em telas menores): ambas ficaram estranhas em telas menores.
- Sem balões com o nome dos itens no recolhido: com o hover, o menu abre antes.

**Largura das telas**

- Todas as telas têm até **1440px, centralizadas**, definidas pelo layout; a page não define
  `max-width`. *Por quê:* Contratos (1200px) e Lançamento (sem limite) tinham larguras
  diferentes.
- Formulário curto limita só os campos, centralizados no cartão, que tem a largura da tela
  (ex: Configurações, campos com até 560px).

**Fonte da marca**

- O nome "Sublime" usa a **Cormorant Garamond, peso 500** (28px, letras com 0,04em de
  espaçamento). A fonte original do logo não foi identificada (o logo com texto só existe em
  160px); entre as candidatas comparadas numa prévia, foi a mais parecida.
- Servida pelo próprio app (pacote `@fontsource/cormorant-garamond`, só o peso 500).
  *Descartado:* link do Google Fonts (dependência externa e envio do IP dos usuários a
  terceiros).

**Construção**

- O recolhido só troca valores de variáveis; as que mudam entre os modos são registradas
  com `@property`, e o navegador as anima junto com a largura, no mesmo ritmo
  (`--shell-motion`, 0,35s).
- CSS aninhado (o nativo, com `&`) no `AppLayout` e no `AppSidebar`.
- `prefers-reduced-motion` desliga as animações.

## 4. Desenho do backend

Não se aplica.

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `src/index.css` | tokens da casca (`--shell-gradient`, `--shell-active`, `--shell-motion`) e `--font-brand` |
| `src/main.jsx` | importa a Cormorant Garamond 500 |
| `app/layout/AppLayout.jsx` e `.module.css` | casca: menu, painel do conteúdo, `main` com `key` pela rota (animação de entrada e rolagem no topo) e `.page` com a largura única |
| `app/layout/AppSidebar.jsx` e `.module.css` | menu novo, com o hover, os atrasos, as pílulas e o foco do teclado |
| `domain/*/pages/*.module.css` | Contratos, Pacientes, Prestadores e Usuários sem o `max-width` próprio |
| `domain/user/pages/SettingsPage.*` e `components/SettingsForm.module.css` | página com a largura padrão; campos com até 560px, centralizados |
| `public/sublime-logo-symbol.png` | símbolo do logo, 600px, usado no topo e no fundo do menu |
| `public/logo sem fundo.png` | original do símbolo (2048px), guardado como fonte da versão de 600px |
| `package.json` | sai o `react-pro-sidebar`; entra o `@fontsource/cormorant-garamond` |

## 6. Tarefas

**Feito**

- [x] Casca verde, painel do conteúdo e animação de entrada das telas.
- [x] Menu próprio, sem o `react-pro-sidebar`.
- [x] Itens fixos nos dois modos, fundo do item em pílula e círculo, pílula clara com curvas.
- [x] Abertura no hover, com atrasos, foco do teclado e correção do foco no Lançamento.
- [x] Topo com logo e nome, rodapé com Sair e dados do usuário, entrada da direita.
- [x] Fonte da marca.
- [x] Largura única das telas e Configurações.
- [x] Sobras do menu por clique apagadas (o hook que lembrava o estado no navegador e a cor
      dos balões).

**Falta**

- [ ] Terminar o teste pela tela (seção 7) e os ajustes que surgirem.
- [ ] Rever os avisos do `npm audit` que apareceram ao instalar a fonte (o `npm audit fix` não
      foi rodado).
- [ ] Conclusão: documentação (seção 9), spec removida, commit e PR para a `dev`.

## 7. Casos de teste

Pela interface, com um usuário ADMIN e um PROVIDER.

| # | Caso | Esperado |
|---|---|---|
| 1 | Passar o mouse no menu e tirar | abre depois de um instante e fecha ao sair; os itens não mudam de lugar |
| 2 | Passar o mouse rápido pela borda esquerda | o menu não abre |
| 3 | Clicar em Lançamento com o menu aberto | a tela abre e o menu continua aberto enquanto o mouse está nele |
| 4 | Tab a partir da página | o foco no primeiro item abre o menu; Tab para fora fecha |
| 5 | Trocar de tela | a pílula clara desliza até o item, e o conteúdo entra subindo |
| 6 | `/patients/new` e uma rota fora do menu | marca Pacientes; fora do menu, sem pílula clara |
| 7 | Telas em janela larga e estreita | até 1440px, centralizadas; ao abrir o menu, o conteúdo se ajusta |
| 8 | Configurações | cartão com a largura da tela, campos centralizados |
| 9 | Janela com até 760px de altura | o menu cabe sem rolar |
| 10 | Windows com escala de 125% | sem linhas verdes na emenda da pílula clara |
| 11 | Sistema com "reduzir movimento" | sem animações |
| 12 | PROVIDER | só os itens do perfil dele |

## 8. Pendências e pontos abertos

- *Em aberto:* **cor do conteúdo** (seção 2).
- *Em aberto:* **CSS aninhado como padrão do projeto** (hoje só no `AppLayout` e no
  `AppSidebar`).
- *Em aberto:* usar a fonte da marca também no título do login (hoje Georgia).
- O nome da branch tem um erro de digitação (`faeture`); corrigir só se o time quiser.

## 9. Documentação ao concluir

- **`architecture.md`** (seção "Frontend", "Estilo"): os tokens `--shell-*` da casca,
  separados das `--color-*` do conteúdo; a largura das telas é do layout (a page não define
  `max-width`; formulário curto limita só os campos); o menu abre no hover e empurra o
  conteúdo; o CSS aninhado, se virar padrão. Sem medidas nem tempos de animação.
- **`development.md`** (tabela de bibliotecas do front): sai o `react-pro-sidebar` e o aviso
  de compatibilidade dele; entra o `@fontsource/cormorant-garamond` (fonte da marca, servida
  pelo app).
- **`roadmap.md`:** tirar a pendência "Sidebar: trocar a logo... Topbar"; no item 3 dos
  Próximos passos, marcar a casca e o menu como feitos e deixar a comparação da cor do
  conteúdo; levar para lá os pontos em aberto da seção 8.
- **`specs/README.md`:** tirar esta spec de "Em andamento".

## 10. ADRs a registrar

Nenhum previsto: no frontend, ADR só para decisões grandes (rotas, autenticação, estado), e
estas são de visual e de comportamento do menu. As alternativas descartadas ficam na seção 3
até a conclusão; o `architecture.md` guarda o resumo.
