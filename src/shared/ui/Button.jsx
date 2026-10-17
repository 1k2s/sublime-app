// Botão padrão com suporte a variantes (primary, outline, secondary). Com loading, fica desabilitado e troca o texto
import styles from "./Button.module.css";

export function Button({
  type = "button",
  variant = "primary",
  loading = false,
  disabled = false,
  className,
  children,
  ...props
}) {
  const classes = [styles.button, styles[variant], className].filter(Boolean).join(" ");

  return (
    <button type={type} className={classes} disabled={disabled || loading} aria-busy={loading} {...props}>
      {loading ? "Aguarde..." : children}
    </button>
  );
}

