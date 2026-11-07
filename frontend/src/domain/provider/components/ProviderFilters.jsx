import { Search } from "lucide-react";
import { Input } from "@/shared/ui/Input";
import styles from "./ProviderFilters.module.css";

const STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Ativos", value: "ACTIVE" },
  { label: "Inativos", value: "INACTIVE" },
];

export function ProviderFilters({ search, onSearchChange, status, onStatusChange }) {
  return (
    <div className={styles.container}>
      <div className={styles.searchWrapper}>
        <Input
          icon={Search}
          type="text"
          placeholder="Buscar por nome ou e-mail..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div className={styles.pillsGroup}>
        {STATUS_OPTIONS.map((opt) => {
          const isSelected = status === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              className={[styles.pill, isSelected && styles.pillActive]
                .filter(Boolean)
                .join(" ")}
              onClick={() => onStatusChange(opt.value)}
            >
              {opt.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
