// Campo de texto padrão. O ref chega como prop (React 19), então funciona com o register do React Hook Form
import styles from "./Input.module.css";

export function Input({ invalid = false, className, ...props }) {
  const classes = [styles.input, invalid && styles.invalid, className].filter(Boolean).join(" ");

  return <input className={classes} aria-invalid={invalid || undefined} {...props} />;
}
