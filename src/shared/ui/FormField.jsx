// Rótulo + campo + mensagem de erro. O htmlFor deve ser o id do campo passado em children
import styles from "./FormField.module.css";

export function FormField({ label, htmlFor, error, children }) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
