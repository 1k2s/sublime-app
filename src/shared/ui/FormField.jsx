// Rótulo + campo + mensagem de erro. O htmlFor deve ser o id do campo passado em children
// Com reserveErrorSpace, a linha do erro ocupa espaço mesmo vazia: o layout não pula quando o erro aparece
import styles from "./FormField.module.css";

export function FormField({ label, htmlFor, error, reserveErrorSpace = false, children }) {
  const errorMessage = error && (
    <span className={styles.error} role="alert">
      {error}
    </span>
  );

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {reserveErrorSpace ? <span className={styles.errorSlot}>{errorMessage}</span> : errorMessage}
    </div>
  );
}
