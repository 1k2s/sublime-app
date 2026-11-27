// Invólucro do ECharts: recebe o `option` pronto (montado em `display/` do módulo, com os números
// da API) e só desenha. Cria o gráfico uma vez, acompanha o tamanho do contêiner (o menu lateral
// empurra o conteúdo, sem evento de janela) e o descarta ao desmontar.
// onEvents: { mouseover: (params) => ..., ... }, os eventos do ECharts (memorize o objeto na tela,
// para não religar a cada render). ref: { dispatchAction }, para a tela destacar uma parte do
// gráfico de fora (ex: o hover numa linha da legenda).
// Import modular: só o que os gráficos do projeto usam entra no bundle. Um tipo de gráfico novo
// é registrado aqui, no `echarts.use`.
// O tamanho é do pai (`className`); o padrão está no Chart.module.css.
import { useEffect, useImperativeHandle, useRef } from "react";
import * as echarts from "echarts/core";
import { PieChart } from "echarts/charts";
import { CanvasRenderer } from "echarts/renderers";
import styles from "./Chart.module.css";

echarts.use([PieChart, CanvasRenderer]);

function prefersReducedMotion() {
  return window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
}

export function Chart({ option, label, onEvents, className, ref }) {
  const containerRef = useRef(null);
  const chartRef = useRef(null);

  useImperativeHandle(ref, () => ({ dispatchAction: (action) => chartRef.current?.dispatchAction(action) }), []);

  useEffect(() => {
    const container = containerRef.current;
    const chart = echarts.init(container);
    chartRef.current = chart;
    const observer = new ResizeObserver(() => chart.resize());
    observer.observe(container);
    return () => {
      observer.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart || !onEvents) return undefined;
    const entries = Object.entries(onEvents);
    entries.forEach(([name, handler]) => chart.on(name, handler));
    return () => entries.forEach(([name, handler]) => chart.off(name, handler));
  }, [onEvents]);

  // A fonte é a da tela; sem animação para quem pediu menos movimento. notMerge: a série que
  // saiu do option sai do gráfico
  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;
    const fontFamily = getComputedStyle(containerRef.current).fontFamily;
    chart.setOption(
      { animation: !prefersReducedMotion(), ...option, textStyle: { fontFamily, ...option.textStyle } },
      { notMerge: true }
    );
  }, [option]);

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={label}
      className={[styles.chart, className].filter(Boolean).join(" ")}
    />
  );
}
