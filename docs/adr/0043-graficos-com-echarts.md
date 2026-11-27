# 0038 — Gráficos do frontend com o ECharts, import modular e invólucro próprio

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

O dashboard trouxe o primeiro gráfico do frontend (a composição do repasse do período), e outros
podem vir se a clínica pedir (rendimento por dia, atendimentos por status ou por técnica). A
biblioteca vale para todo gráfico futuro e é cara de trocar, porque cada gráfico é montado no
formato dela.

## Decisão

- **ECharts**, pelo pacote `echarts`.
- **Import modular:** `echarts/core` e só os tipos usados, registrados com o `echarts.use` num
  lugar só (`shared/components/Chart.jsx`). O pacote inteiro passa de 1 MB; a tela com gráfico
  carrega o componente com `React.lazy`, e o ECharts só baixa quando ele aparece.
- **Invólucro próprio** (`Chart.jsx`): recebe o `option` montado em `display/` do módulo, com os
  números da API, cria o gráfico, acompanha o tamanho do contêiner, liga os eventos e o descarta
  ao desmontar.

## Alternativas descartadas

- **Recharts:** componentes React, mas em SVG, mais lento com muitos pontos e com menos tipos de
  gráfico.
- **Chart.js:** canvas e leve, mas com menos recursos de interação (destaque ligado à legenda,
  animação de atualização).
- **`echarts-for-react`:** mais uma dependência para um invólucro de poucas linhas.

## Consequências

- Ver "Gráficos" na seção "Frontend" do `architecture.md`.
- O canvas não lê `var()`: as cores da paleta são lidas das variáveis CSS e passadas ao `option`;
  o texto que importa (centro e legenda) fica em HTML.
- Um tipo de gráfico novo precisa ser registrado no `Chart.jsx`, e cada gráfico novo do dashboard
  precisa de uma consulta no backend (o front não agrupa nem soma).
