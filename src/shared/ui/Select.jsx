// Lista de opções padrão, no mesmo visual do Input. O ref chega como prop (React 19)
import { ChevronDown } from "lucide-react";
import styles from "./Select.module.css";

export function Select({ invalid = false, className, children, ...props }) {
  const classes = [styles.select, invalid && styles.invalid, className].filter(Boolean).join(" ");

  return (
    <div className={styles.wrapper}>
      <select className={classes} aria-invalid={invalid || undefined} {...props}>
        {children}
      </select>
      <ChevronDown className={styles.icon} size={18} aria-hidden />
    </div>
  );
}
