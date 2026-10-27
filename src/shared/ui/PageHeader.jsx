// Título da página com descrição opcional
import styles from "./PageHeader.module.css";

export function PageHeader({ title, description }) {
  return (
    <header className={styles["page-header"]}>
      <h1 className={styles["page-header-title"]}>{title}</h1>
      {description && <p className={styles["page-header-description"]}>{description}</p>}
    </header>
  );
}
