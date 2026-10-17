// Título da página com descrição opcional
import styles from "./PageHeader.module.css";

export function PageHeader({ title, description }) {
  return (
    <header className={styles.header}>
      <h1 className={styles.title}>{title}</h1>
      {description && <p className={styles.description}>{description}</p>}
    </header>
  );
}
