// Grupo de pílulas para escolher um valor (ex: filtro de status). Só uma fica ativa
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
        </button>
      ))}
    </div>
  );
}
