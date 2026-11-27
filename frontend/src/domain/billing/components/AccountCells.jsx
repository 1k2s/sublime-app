import styles from "./AccountCells.module.css";

// O cabeçalho de uma conta (o título e um subtítulo à direita) e as células iguais embaixo, cada
// uma com rótulo, valor e uma nota. O saldo tem o mesmo tamanho dos outros: só a cor muda
// (tone: "negative" vermelho, "positive" azul). cells: [{ label, value, note, tone? }]
export function AccountCells({ title, subtitle, cells }) {
  return (
    <div className={styles.box}>
      <div className={styles.header}>
        <h3 className={styles.title}>{title}</h3>
        {subtitle && <span className={styles.note}>{subtitle}</span>}
      </div>
      <div className={styles.cells} style={{ gridTemplateColumns: `repeat(${cells.length}, minmax(0, 1fr))` }}>
        {cells.map((cell) => (
          <div key={cell.label} className={styles.cell}>
            <span className={styles.label}>{cell.label}</span>
            <span className={[styles.money, cell.tone && styles[cell.tone]].filter(Boolean).join(" ")}>{cell.value}</span>
            <span className={[styles.note, cell.tone && styles[cell.tone]].filter(Boolean).join(" ")}>{cell.note}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
