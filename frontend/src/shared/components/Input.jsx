// Campo de texto padrão com suporte opcional a ícone (icon) ou texto (prefix, ex: "R$") à esquerda e a
// um controle à direita (endSlot, ex: o botão de mostrar a senha do PasswordInput).
// O ref chega como prop (React 19), funcionando nativamente com o register do React Hook Form.
import styles from "./Input.module.css";

export function Input({ invalid = false, icon: Icon, prefix, endSlot, className, containerClassName, ...props }) {
  const hasStartSlot = Boolean(Icon || prefix);

  if (hasStartSlot || endSlot) {
    const inputClasses = [
      styles.inputWithIcon,
      !hasStartSlot && styles.withoutStartSlot,
      endSlot && styles.withEndSlot,
      className,
    ];

    return (
      <div className={[styles.inputWrapper, invalid && styles.invalidWrapper, containerClassName].filter(Boolean).join(" ")}>
        {hasStartSlot && <span className={styles.iconSlot}>{Icon ? <Icon size={18} /> : prefix}</span>}
        <input className={inputClasses.filter(Boolean).join(" ")} aria-invalid={invalid || undefined} {...props} />
        {endSlot && <span className={styles.endSlot}>{endSlot}</span>}
      </div>
    );
  }

  const classes = [styles.input, invalid && styles.invalid, className].filter(Boolean).join(" ");
  return <input className={classes} aria-invalid={invalid || undefined} {...props} />;
}
