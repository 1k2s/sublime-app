// Lista de opções padrão. O ref chega como prop (React 19), então funciona com o register do React Hook Form
import styles from "./Select.module.css";

export function Select({ invalid = false, className, children, ...props }) {
  const classes = [styles.select, invalid && styles.invalid, className].filter(Boolean).join(" ");

  return (
    <select className={classes} aria-invalid={invalid || undefined} {...props}>
      {children}
    </select>
  );
}
