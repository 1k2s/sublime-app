// Botão padrão. Com loading, fica desabilitado e troca o texto
import styles from "./Button.module.css";

export function Button({ type = "button", loading = false, disabled = false, className, children, ...props }) {
  const classes = [styles.button, className].filter(Boolean).join(" ");

  return (
    <button type={type} className={classes} disabled={disabled || loading} aria-busy={loading} {...props}>
      {loading ? "Aguarde..." : children}
    </button>
  );
}
