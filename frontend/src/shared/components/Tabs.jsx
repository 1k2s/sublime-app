// Abas de um modal ou de uma tela: todas com a mesma largura, para a linha da aba ativa deslizar
// entre elas só com o transform (pelo índice). O painel é de quem usa (role="tabpanel").
// tabs: [{ value, label }]. label: o nome do grupo para o leitor de tela (aria-label)
import styles from "./Tabs.module.css";

export function Tabs({ tabs, value, onChange, label, className }) {
  const activeIndex = Math.max(
    0,
    tabs.findIndex((tab) => tab.value === value),
  );

  return (
    <div
      role="tablist"
      aria-label={label}
      className={[styles.tabs, className].filter(Boolean).join(" ")}
      style={{ "--tab-count": tabs.length, "--tab-index": activeIndex }}
    >
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          role="tab"
          aria-selected={tab.value === value}
          className={[styles.tab, tab.value === value && styles.tabActive].filter(Boolean).join(" ")}
          onClick={() => onChange(tab.value)}
        >
          {tab.label}
        </button>
      ))}
      <span className={styles.indicator} aria-hidden />
    </div>
  );
}
