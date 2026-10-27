/**
 * ======================================================================================
 * COMPONENTE: PatientFilters
 * ======================================================================================
 *
 * Renderiza os controles de filtragem da listagem de pacientes (conforme Imagem 1):
 * - Input de busca textual com ícone de lupa (busca por nome ou dígitos de CPF).
 * - Abas tipo "pill" para alternar o status do paciente:
 *   - "Todos": exibe todos os pacientes.
 *   - "Ativos": exibe apenas pacientes com status ACTIVE (selecionado por padrão).
 *   - "Inativos": exibe pacientes com status INACTIVE.
 */

import { Search } from "lucide-react";
import { Input } from "@/shared/ui/Input";
import styles from "./PatientFilters.module.css";

/**
 * Opções de filtro de status disponíveis na interface
 */
const STATUS_OPTIONS = [
  { label: "Todos", value: "ALL" },
  { label: "Ativos", value: "ACTIVE" },
  { label: "Inativos", value: "INACTIVE" },
];

/**
 * @param {Object} props
 * @param {string} props.search - Valor atual do termo de busca
 * @param {function(string): void} props.onSearchChange - Callback disparado ao alterar o texto de busca
 * @param {string} props.status - Status selecionado ("ALL" | "ACTIVE" | "INACTIVE")
 * @param {function(string): void} props.onStatusChange - Callback disparado ao clicar em uma aba de status
 */
export function PatientFilters({ search, onSearchChange, status, onStatusChange }) {
  return (
    <div className={styles.container}>
      {/* Campo de pesquisa com ícone de lupa posicionado à esquerda */}
      <div className={styles.searchWrapper}>
        <Input
          icon={Search}
          type="text"
          placeholder="Buscar por nome ou CPF..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      {/* Grupo de botões "pills" para seleção do filtro de status */}
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
