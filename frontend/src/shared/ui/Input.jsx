// Campo de texto padrão com suporte opcional a ícone à esquerda (icon).
// O ref chega como prop (React 19), funcionando nativamente com o register do React Hook Form.
import styles from "./Input.module.css";

export function Input({ invalid = false, icon: Icon, className, containerClassName, ...props }) {
  if (Icon) {
    return (
      <div className={[styles.inputWrapper, invalid && styles.invalidWrapper, containerClassName].filter(Boolean).join(" ")}>
        <span className={styles.iconSlot}>
          <Icon size={18} />
        </span>
        <input
          className={[styles.inputWithIcon, className].filter(Boolean).join(" ")}
          aria-invalid={invalid || undefined}
          {...props}
        />
      </div>
    );
  }

  const classes = [styles.input, invalid && styles.invalid, className].filter(Boolean).join(" ");
  return <input className={classes} aria-invalid={invalid || undefined} {...props} />;
}
