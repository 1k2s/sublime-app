// Controles de filtragem da listagem de pacientes (busca textual + status)
import { Search } from "lucide-react";
import { FilterPills } from "@/shared/components/FilterPills";
import { Input } from "@/shared/components/Input";
import styles from "@/domain/patient/components/PatientFilters.module.css";

const STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Ativos", value: "ACTIVE" },
  { label: "Inativos", value: "INACTIVE" },
];

export function PatientFilters({ search, onSearchChange, status, onStatusChange }) {
  return (
    <div className={styles.container}>
      <div className={styles.searchWrapper}>
        <Input
          icon={Search}
          type="text"
          placeholder="Buscar por nome ou CPF..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <FilterPills options={STATUS_OPTIONS} value={status} onChange={onStatusChange} />
    </div>
  );
}
