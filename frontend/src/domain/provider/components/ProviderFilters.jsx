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
          placeholder="Buscar por nome..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div className={styles.pillsGroup}>
        {STATUS_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            className={[styles.pill, status === option.value && styles.pillActive].filter(Boolean).join(" ")}
            onClick={() => onStatusChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}
