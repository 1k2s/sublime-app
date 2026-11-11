import { Pencil } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Card } from "@/shared/components/Card";
import { ListMessage } from "@/shared/components/ListMessage";
import { maskCpf, maskPhone } from "@/shared/utils/masks";
import styles from "@/domain/patient/components/PatientTable.module.css";

function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

const MESSAGES = {
  loading: "Carregando pacientes...",
  error: "Não foi possível carregar os pacientes",
  filtered: "Nenhum paciente encontrado",
  empty: "Nenhum paciente cadastrado",
  emptyHint: "Clique em Novo paciente para começar.",
};

export function PatientTable({
  patients = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  onViewDetails,
}) {
  if (isLoading || isError || patients.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr className={styles.headRow}>
            <th className={`${styles.th} ${styles.thName}`}>NOME</th>
            <th className={styles.th}>CPF</th>
            <th className={styles.th}>TELEFONE</th>
            <th className={styles.th}>CIDADE</th>
            <th className={styles.th}>STATUS</th>
            <th className={`${styles.th} ${styles.thAction}`}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {patients.map((patient) => (
            <tr key={patient.id} className={styles.row}>
              <td className={`${styles.td} ${styles.tdName}`}>
                <div className={styles.nameCell}>
                  <div className={styles.avatar}>
                    {getInitials(patient.name)}
                  </div>
                  <span className={styles.nameText}>{patient.name}</span>
                </div>
              </td>

              <td className={`${styles.td} ${styles.tdData}`}>
                {patient.cpf ? maskCpf(patient.cpf) : "--"}
              </td>

              <td className={`${styles.td} ${styles.tdData}`}>
                {patient.phone ? maskPhone(patient.phone) : "--"}
              </td>

              <td className={`${styles.td} ${styles.tdData}`}>
                {patient.address?.city || "--"}
              </td>

              <td className={styles.td}>
                <Badge variant={patient.active ? "active" : "inactive"}>
                  {patient.active ? "Ativo" : "Inativo"}
                </Badge>
              </td>

              <td className={`${styles.td} ${styles.thAction}`}>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onViewDetails?.(patient)}
                  aria-label={`Ver ou editar ${patient.name}`}
                  title="Ver / Editar"
                >
                  <Pencil size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
