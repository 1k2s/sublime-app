// Grupo de pílulas para escolher um valor (ex: filtro de status). Só uma fica ativa.
// options: [{ label, value, count? }]; count (opcional) aparece ao lado do rótulo (ex: "Atrasados 3")
import styles from "./FilterPills.module.css";

export function FilterPills({ options, value, onChange, className }) {
  return (
    <div className={[styles.group, className].filter(Boolean).join(" ")}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          aria-pressed={value === option.value}
          className={[styles.pill, value === option.value && styles.pillActive].filter(Boolean).join(" ")}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          {option.count != null && <span className={styles.count}>{option.count}</span>}
        </button>
      ))}
    </div>
  );
}
