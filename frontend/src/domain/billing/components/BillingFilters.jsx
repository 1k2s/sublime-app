import { Search } from "lucide-react";
import { FilterPills } from "@/shared/components/FilterPills";
import { Input } from "@/shared/components/Input";
import { ACCOUNT_FILTERS } from "../utils/filterAccounts";
import styles from "./BillingFilters.module.css";

// Busca por nome e os filtros da lista, com a contagem de cada um (counts: do countAccounts)
export function BillingFilters({ search, onSearchChange, filter, onFilterChange, counts = {}, className }) {
  const options = Object.values(ACCOUNT_FILTERS).map((option) => ({
    value: option.value,
    label: option.label,
    count: counts[option.value],
  }));

  return (
    <div className={[styles.filters, className].filter(Boolean).join(" ")}>
      <Input
        icon={Search}
        type="search"
        placeholder="Buscar paciente..."
        containerClassName={styles["filters-search"]}
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
      />

      <FilterPills options={options} value={filter} onChange={onFilterChange} />
    </div>
  );
}
