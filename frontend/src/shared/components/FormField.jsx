// Rótulo + campo + mensagem de erro. O htmlFor deve ser o id do campo passado em children
// Com reserveErrorSpace, a linha do erro ocupa espaço mesmo vazia: o layout não pula quando o erro aparece
// hint (opcional): uma dica embaixo do campo, no lugar do erro; o erro, quando há, toma o lugar dela
import styles from "./FormField.module.css";

export function FormField({ label, htmlFor, error, hint, reserveErrorSpace = false, children }) {
  const message = error ? (
    <span className={styles.error} role="alert">
      {error}
    </span>
  ) : (
    hint && <span className={styles.hint}>{hint}</span>
  );

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
      </label>
      {children}
      {reserveErrorSpace ? <span className={styles.errorSlot}>{message}</span> : message}
    </div>
  );
}
