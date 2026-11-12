import { Search } from "lucide-react";
import { FilterPills } from "@/shared/components/FilterPills";
import { Input } from "@/shared/components/Input";
import styles from "./ContractFilters.module.css";

// Os status vêm do backend (active e current; ver contractStatus.js)
const STATUS_OPTIONS = [
  { label: "Vigentes", value: "CURRENT" },
  { label: "Vencidos", value: "EXPIRED" },
  { label: "Inativos", value: "INACTIVE" },
  { label: "Todos", value: "ALL" },
];

export function ContractFilters({ search, onSearchChange, status, onStatusChange, className }) {
  return (
    <div className={[styles.filters, className].filter(Boolean).join(" ")}>
      <Input
        icon={Search}
        type="search"
        placeholder="Buscar por titular ou beneficiário..."
        containerClassName={styles["filters-search"]}
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
      />

      <FilterPills options={STATUS_OPTIONS} value={status} onChange={onStatusChange} />
    </div>
  );
}
