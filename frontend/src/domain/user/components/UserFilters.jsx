import { Search } from "lucide-react";
import { ROLES } from "@/domain/auth/roles";
import { FilterPills } from "@/shared/components/FilterPills";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import styles from "./UserFilters.module.css";

const STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Ativos", value: "ACTIVE" },
  { label: "Inativos", value: "INACTIVE" },
];

export function UserFilters({ search, onSearchChange, role, onRoleChange, status, onStatusChange }) {
  return (
    <div className={styles.container}>
      <div className={styles.searchWrapper}>
        <Input
          icon={Search}
          type="text"
          placeholder="Buscar por e-mail..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div className={styles.roleWrapper}>
        <Select aria-label="Perfil" value={role} onChange={(e) => onRoleChange(e.target.value)}>
          <option value="ALL">Todos os perfis</option>
          {Object.values(ROLES).map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </Select>
      </div>

      <FilterPills options={STATUS_OPTIONS} value={status} onChange={onStatusChange} />
    </div>
  );
}
