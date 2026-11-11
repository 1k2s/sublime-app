import { Search } from "lucide-react";
import { FilterPills } from "@/shared/components/FilterPills";
import { Input } from "@/shared/components/Input";
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

      <FilterPills options={STATUS_OPTIONS} value={status} onChange={onStatusChange} />
    </div>
  );
}
