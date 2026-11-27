// Abas do dashboard com o slide: os painéis ficam lado a lado num trilho, e a troca desliza para
// o lado. A linha da aba ativa acompanha, na curva do menu lateral (--shell-motion); com
// prefers-reduced-motion, troca sem animação.
// Os painéis têm a altura que sobra na tela (o pai define): o conteúdo se ajusta a ela, e a
// rolagem fica dentro do painel (ex: a tabela do Resumo rola por dentro).
// Cada painel é montado na primeira visita e continua montado depois (o Panorama, com o
// ECharts, não paga o custo antes de ser aberto nem recria o gráfico a cada troca). O painel
// fora da tela fica inert (sem foco pelo teclado nem leitor de tela)
import { useRef, useState } from "react";
import styles from "./DashboardTabs.module.css";

// tabs: [{ value, label, icon: Componente, content: nó }]; toolbar: o que vale para as duas abas
// (os filtros), entre as abas e os painéis, fora do slide
export function DashboardTabs({ tabs, activeTab, onChange, toolbar, idPrefix = "dashboard-tab", className }) {
  const [visited, setVisited] = useState(() => new Set([activeTab]));
  const tabRefs = useRef({});
  const activeIndex = Math.max(
    0,
    tabs.findIndex((tab) => tab.value === activeTab)
  );

  function select(value) {
    setVisited((current) => (current.has(value) ? current : new Set(current).add(value)));
    onChange(value);
  }

  // Setas e Home/End entre as abas (padrão WAI-ARIA de abas)
  function handleKeyDown(event) {
    const lastIndex = tabs.length - 1;
    const nextIndex = {
      ArrowRight: activeIndex === lastIndex ? 0 : activeIndex + 1,
      ArrowLeft: activeIndex === 0 ? lastIndex : activeIndex - 1,
      Home: 0,
      End: lastIndex,
    }[event.key];
    if (nextIndex === undefined) return;
    event.preventDefault();
    const next = tabs[nextIndex].value;
    select(next);
    tabRefs.current[next]?.focus();
  }

  return (
    <div
      className={[styles.tabs, className].filter(Boolean).join(" ")}
      style={{ "--tab-count": tabs.length, "--tab-index": activeIndex }}
    >
      <div className={styles.tablist} role="tablist" aria-label="Visões do dashboard" onKeyDown={handleKeyDown}>
        {tabs.map((tab) => {
          const isActive = tab.value === activeTab;
          const Icon = tab.icon;
          return (
            <button
              key={tab.value}
              ref={(element) => {
                tabRefs.current[tab.value] = element;
              }}
              id={`${idPrefix}-${tab.value}`}
              type="button"
              role="tab"
              aria-selected={isActive}
              aria-controls={`${idPrefix}-${tab.value}-panel`}
              tabIndex={isActive ? 0 : -1}
              className={[styles.tab, isActive && styles.active].filter(Boolean).join(" ")}
              onClick={() => select(tab.value)}
            >
              {Icon && <Icon className={styles.icon} />}
              {tab.label}
            </button>
          );
        })}
        <span className={styles.indicator} aria-hidden="true" />
      </div>

      {toolbar}

      <div className={styles.viewport}>
        <div className={styles.track}>
          {tabs.map((tab) => {
            const isActive = tab.value === activeTab;
            return (
              <div
                key={tab.value}
                id={`${idPrefix}-${tab.value}-panel`}
                role="tabpanel"
                aria-labelledby={`${idPrefix}-${tab.value}`}
                inert={!isActive}
                className={styles.panel}
              >
                {(isActive || visited.has(tab.value)) && tab.content}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
