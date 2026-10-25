// Cabeçalho de página: título, subtítulo e, opcionalmente, uma ação à direita
import styles from "./PageHeader.module.css";

export function PageHeader({ title, subtitle, action }) {
  return (
    <header className={styles.header}>
      <div>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      {action}
    </header>
  );
}
