# Spec — Layout das telas

Novo visual do portal a partir das imagens de referência: as cores novas do menu, o fundo creme,
a tabela padrão das listagens e, depois, o formato do cabeçalho das telas.

Iniciada em 2026-11-18.

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

- **Branch:** `feature/recontrucao-layout-telas`, junto com a tela de preços na
  `merge/estruturacao-layout`. PR para a `dev`, com merge commit.
- **Implementado** (só frontend): as cores do menu, o fundo creme, a tabela padrão nas cinco
  listagens, os losangos de enfeite e o carregamento com o nome da marca (seção 5). Conferido por
  `npm run build` e `npm run lint`; **teste pela tela não feito** (seção 7).
- **Falta:** o teste pela tela, o formato do cabeçalho das telas e os cards de resumo (seção 2),
  as decisões da seção 8 e a documentação de conclusão (seção 9).
- **Ambiente** (notado em 2026-11-18, numa máquina nova): depois de puxar, rodar `npm install`
  (o `@fontsource/cormorant-garamond`) e criar o `frontend/.env` e o `backend/.env` a partir dos
  `.env.example`.

## 2. Objetivo e escopo

Parte do item "Nova identidade visual do portal" dos Próximos passos do Frontend no
[`roadmap.md`](../roadmap.md). Resolve a "cor do conteúdo" que a casca do app deixou em aberto:
o conteúdo fica no creme e no verde-escuro do menu.

**Entra:**

- cores do menu lateral (o funcionamento não muda);
- fundo creme do app;
- tabela padrão das listagens (Preços, Pacientes, Usuários, Prestadores e Contratos);
- cabeçalho das telas no formato da referência (rótulo acima do título, título com a fonte da
  marca, descrição e o botão principal à direita) e os cards de resumo (ex: "Prestadores
  cadastrados", "Ativos"), com prévia antes do código.

- losangos de enfeite no pé do menu e nos cantos de baixo do conteúdo;
- carregamento com o nome da marca (`PageLoader`).

**Fica para depois:** *a decidir com o usuário* o que mais da referência entra nesta branch: no
menu, o subtítulo "Fisioterapia especializada", o usuário num cartão claro com avatar e o selo
"Em breve" no Dashboard; e se o logo cinza no meio do menu fica (a referência não tem).

## 3. Regras

Decisões de visual, com o motivo e o que foi comparado.

**Menu**

- Verde-escuro liso (`#2e422a`) no lugar do degradê, sem a sombra escura à direita; texto e
  ícones em creme. A pílula da tela aberta continua na cor do fundo, com o nome e o ícone no
  verde do menu. *Por quê:* as cores da imagem de referência, medidas direto nela.

**Fundo**

- Creme (`#f6f2e9`) em todas as telas, no `--color-bg`. A pílula do menu usa a mesma variável,
  porque no recolhido ela se emenda no painel pelas curvas.

**Losangos de enfeite** (`app/layout/BackgroundShapes`)

- Três grupos de quadrados girados 38°, desenhados em SVG a partir da referência: no pé do menu
  (contorno verde e uma faixa um pouco mais clara que o fundo, atrás do usuário), no canto de
  baixo à esquerda do conteúdo (junto do menu) e no canto de baixo à direita (o maior).
- Ficam presos aos cantos do painel, por trás das telas: não rolam com o conteúdo, e cards e
  tabelas passam por cima. Encolhem com a janela (até 30% da largura) e somem do menu recolhido
  (o painel corta).
- Cores pelas variáveis: as do menu e as `--color-primary*`, mais a areia
  (`--color-sand`, `--color-sand-soft`) e a faixa do menu (`--shell-band`).

**Carregamento** (`shared/components/PageLoader`)

- A "primeira versão" das prévias de loading (2026-11-15): o nome "Sublime" se desenha pelo
  contorno, se preenche e some, em loop de 2,8s, com "Fisioterapia especializada" embaixo, no
  fundo creme. Na fonte da marca (`--font-brand`), e não na Fraunces da prévia, para ser o mesmo
  nome do menu. Sem movimento (`prefers-reduced-motion`), fica parado e preenchido.
- Em todos os carregamentos de bloco, em três tamanhos (`variant`): `page`, na entrada no app
  (o `HydrateFallback` do router); `section`, no lugar das listas (pelo `ListMessage`), da tabela
  de Preços e do formulário de Configurações; `compact` (só o nome), no resumo do dia do
  Lançamento, na liberação de retroativo e nos planos do cadastro de contrato. O `label` é o
  texto que o leitor de tela lê (ex: "Carregando pacientes...").
- Ficam com texto os carregamentos dentro de campos e botões ("Buscando...", "Calculando...",
  "Aguarde..."): o nome desenhado não cabe neles.

**Tabela padrão** (`shared/components/DataTable`)

- Cabeçalho no verde do menu com texto em creme e em maiúsculas (pelo CSS: no código o texto
  fica normal, para o leitor de tela), linhas pares com listra creme, divisória clara entre as
  linhas e destaque verde-claro no hover. O card se destaca pela sombra, sem borda.
- Valor em dinheiro alinhado à direita e em negrito; a coluna de ações, centralizada; o texto
  principal da linha (nome, plano, e-mail) mais escuro que o resto.
- Cada tela passa as colunas (`header`, `cell`, `align`, `emphasized`) e as linhas; não tem CSS
  de tabela próprio. *Por quê:* as cinco tabelas tinham CSS copiado e visuais diferentes.
- *Comparadas na prévia (2026-11-18):* **A**, igual à imagem (cabeçalho verde médio com
  divisórias, listra sálvia e borda; fria no fundo creme); **B**, a escolhida; **C**, cabeçalho
  sálvia claro com texto verde-escuro.
- O avatar das iniciais com o nome (`AvatarName`) e o botão de ícone (`IconButton`) viraram
  componentes compartilhados, e o `getInitials` (copiado em três tabelas) foi para o
  `shared/utils/text.js`.

## 4. Desenho do backend

Não se aplica.

## 5. Desenho do frontend

| Arquivo | Mudança |
|---|---|
| `index.css` | `--color-bg` creme; `--color-row-stripe`, `--color-row-line`, `--color-primary-strong`, `--color-sand`, `--color-sand-soft` e `--shell-band` (novos); `--shell-gradient` substituído por `--shell-bg`, `--shell-text` e `--shell-muted`; `--shell-active` no verde do menu |
| `shared/components/PageLoader` | a animação do nome da marca no lugar do "Carregando..." |
| `app/layout/BackgroundShapes` (novo) | os losangos, por `placement` (`sidebar`, `contentStart`, `contentEnd`); `AppSidebar` e `AppLayout` os posicionam |
| `app/layout/AppSidebar.module.css`, `AppLayout.module.css` | fundo liso e cores pelas `--shell-*` |
| `shared/components/DataTable` (novo) | a tabela padrão |
| `shared/components/IconButton` (novo) | botão só com ícone |
| `shared/components/AvatarName` (novo) | avatar das iniciais com o nome e uma linha opcional |
| `shared/utils/text.js` | `getInitials` |
| `PriceTable`, `PatientTable`, `UserTable`, `ProviderTable`, `ContractTable` | colunas no `DataTable`; os CSS de tabela de Pacientes, Usuários e Prestadores removidos |

## 6. Tarefas

- [x] Cores do menu.
- [x] Prévia das tabelas e escolha (versão B).
- [x] Fundo creme.
- [x] `DataTable`, `IconButton` e `AvatarName`; as cinco tabelas migradas.
- [x] Losangos de enfeite.
- [x] Carregamento com o nome da marca.
- [ ] Teste pela tela (seção 7).
- [ ] Prévia do cabeçalho das telas e dos cards de resumo; implementar a escolhida.
- [ ] Conclusão: seção 9, `node scripts/check-docs.mjs`, spec removida.

## 7. Casos de teste

Pela interface, com o backend no ar e os dados do seed.

| # | Caso | Esperado |
|---|---|---|
| 1 | Abrir e recolher o menu, passar o mouse e o Tab nos itens | igual a antes, nas cores novas; a pílula se emenda no fundo creme sem linha visível |
| 2 | Abrir cada listagem (Preços, Pacientes, Usuários, Prestadores, Contratos) | tabela no padrão novo; listra nas linhas pares; botão de ação funciona |
| 3 | Janela estreita numa listagem com muitas colunas (Prestadores, Contratos) | a tabela rola de lado dentro do card; a tela não rola de lado |
| 4 | Contrato com beneficiário | a linha "Benef.:" abaixo do titular; valor e parcelas à direita |
| 5 | Modais, campos e o `Combobox` do Lançamento | os fundos internos no creme, legíveis |
| 6 | Recarregar o app (F5) numa tela; de novo com a rede lenta (DevTools, "Slow 4G") | o nome se desenha no fundo creme até a tela abrir |
| 7 | Losangos: menu aberto e recolhido, rolar uma lista longa, janela estreita | no pé do menu só aberto; nos cantos do conteúdo, parados ao rolar e por trás dos cards; menores na janela estreita |

## 8. Pendências e pontos abertos

- *Em aberto:* **carregamento com `prefers-reduced-motion`.** Hoje fica parado e preenchido, e não
  mostra que algo carrega. Em máquinas com os efeitos de animação do Windows desligados (como a usada
  nos testes de 2026-11-18) é sempre assim. Proposta: nesse modo, o nome pisca devagar (só
  opacidade, sem deslocamento). Muda a regra "`prefers-reduced-motion` desliga as animações" do
  `architecture.md` para "desliga os movimentos; o carregamento só pisca".
- *Em aberto:* **fonte Fraunces**, a da prévia do carregamento e, ao que parece, a do título
  "Prestadores" da referência: decidir com o cabeçalho das telas (hoje, a Cormorant Garamond).
- *Em aberto:* o **logo cinza no meio do menu** (a referência não tem) e o grupo de losangos junto
  do menu com ele recolhido.

- O `--color-border` continua no cinza frio (`#e4e9e6`); conferir na tela se combina com o
  creme ou se vai para um tom quente.
- Os outros tons escritos direto nos `.module.css` (ex: o `Badge`) seguem a melhoria "Cores em
  variáveis CSS em todos os componentes" do roadmap.

## 9. Documentação ao concluir

- `architecture.md`, "Estilo": a lista da paleta (`--color-row-*`), a casca com as `--shell-*`
  novas (sem "o degradê") e a tabela padrão (`DataTable`: colunas por tela, sem CSS próprio de
  tabela); em "Quando criar um componente", o `DataTable` como exemplo.
- `roadmap.md`, Frontend: o item 3 dos Próximos passos (a cor do conteúdo decidida) e o "Visual
  global em cada tela".
- `specs/README.md`: tirar esta spec de "Em andamento".

## 10. ADRs a registrar

Não se aplica: decisões de visual do dia a dia, sem ADR no frontend (CLAUDE.md).
