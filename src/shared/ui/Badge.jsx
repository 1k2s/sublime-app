// Etiqueta de status. A variante define a cor
import styles from "./Badge.module.css";

export function Badge({ variant = "success", children }) {
  return <span className={`${styles.badge} ${styles[variant]}`}>{children}</span>;
}
