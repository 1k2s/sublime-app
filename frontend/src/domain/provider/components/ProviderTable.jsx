import { Pencil } from "lucide-react";
import { Card } from "@/shared/components/Card";
import { Badge } from "@/shared/components/Badge";
import { ListMessage } from "@/shared/components/ListMessage";
import { getRole } from "@/domain/auth/roles";
import { formatPercentage } from "@/shared/utils/money";
import styles from "./ProviderTable.module.css";

const MESSAGES = {
  loading: "Carregando prestadores...",
  error: "Não foi possível carregar os prestadores",
  filtered: "Nenhum prestador encontrado",
  empty: "Nenhum prestador cadastrado",
  emptyHint: "Clique em Novo prestador para começar.",
};

function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

export function ProviderTable({
  providers = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  onViewDetails,
}) {
  if (isLoading || isError || providers.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.thName}>NOME</th>
            <th>E-MAIL</th>
            <th>PERFIL</th>
            <th>COMISSÃO PADRÃO</th>
            <th>COMISSÃO INDICAÇÃO</th>
            <th>STATUS</th>
            <th className={styles.thAction}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {providers.map((provider) => {
            const isAtivo = provider.active;

            return (
              <tr key={provider.id} className={styles.row}>
                <td className={styles.tdName}>
                  <div className={styles.nameCell}>
                    <div className={styles.avatar}>
                      {getInitials(provider.name)}
                    </div>
                    <span className={styles.nameText}>{provider.name}</span>
                  </div>
                </td>

                <td className={styles.tdData}>{provider.email || "--"}</td>

                <td className={styles.tdData}>{getRole(provider.role)?.label ?? "--"}</td>

                <td className={styles.tdData}>{formatPercentage(provider.commissionPercentage)}</td>

                <td className={styles.tdData}>{formatPercentage(provider.referralCommissionPercentage)}</td>

                <td>
                  <Badge variant={isAtivo ? "active" : "inactive"}>
                    {isAtivo ? "Ativo" : "Inativo"}
                  </Badge>
                </td>

                <td className={styles.thAction}>
                  <button
                    type="button"
                    className={styles.actionButton}
                    onClick={() => onViewDetails?.(provider)}
                    aria-label="Ver e editar prestador"
                    title="Ver / Editar"
                  >
                    <Pencil size={16} />
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </Card>
  );
}
