import { Search } from "lucide-react";
import { Input } from "@/shared/ui/Input";
import styles from "./ContractFilters.module.css";

const STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Ativos", value: "ACTIVE" },
  { label: "Encerrados", value: "CLOSED" },
];

export function ContractFilters({ search, onSearchChange, status, onStatusChange, className }) {
  return (
    <div className={[styles.filters, className].filter(Boolean).join(" ")}>
      <Input
        icon={Search}
        type="search"
        placeholder="Buscar por paciente..."
        containerClassName={styles["filters-search"]}
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
      />

      <div className={styles["filters-pills"]}>
        {STATUS_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            aria-pressed={status === option.value}
            className={[styles["filters-pill"], status === option.value && styles["filters-pill-active"]]
              .filter(Boolean)
              .join(" ")}
            onClick={() => onStatusChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}
