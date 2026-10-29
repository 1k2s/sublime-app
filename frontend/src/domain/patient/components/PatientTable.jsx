/**
 * ======================================================================================
 * COMPONENTE: PatientTable
 * ======================================================================================
 *
 * Tabela de apresentação dos pacientes cadastrados (conforme Imagem 1):
 * - Envolvida em container Card com bordas sutis e cantos arredondados.
 * - Colunas: NOME (com avatar circular contendo as iniciais), CPF, TELEFONE, CIDADE, STATUS e AÇÕES.
 * - Suporta estados de carregamento (loading), lista vazia (empty) e dados preenchidos.
 * - Controle de perfil (RBAC):
 *   - Administrador (`isAdmin = true`): exibe a ação "Ver / Editar".
 *   - Prestador (`isAdmin = false`): exibe apenas "Ver".
 */

import { Card } from "@/shared/ui/Card";
import { Badge } from "@/shared/ui/Badge";
import styles from "./PatientTable.module.css";

/**
 * Extrai as duas primeiras iniciais de um nome completo para exibir dentro do avatar.
 * Exemplo: "Maria Eduarda Santos" -> "ME"
 *
 * @param {string} name - Nome completo do paciente
 * @returns {string} Iniciais em maiúsculas (máximo 2 caracteres)
 */
function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

/**
 * @param {Object} props
 * @param {Array<Object>} props.patients - Lista de pacientes a renderizar
 * @param {boolean} [props.isLoading=false] - Indica se os dados estão sendo carregados da API
 * @param {boolean} [props.isAdmin=false] - Indica se o usuário atual é administrador
 * @param {function(Object): void} [props.onViewDetails] - Callback disparado ao clicar no botão de ação da linha
 */
export function PatientTable({
  patients = [],
  isLoading = false,
  isAdmin = false,
  onViewDetails,
}) {
  // Estado de carregamento: exibe feedback visual enquanto o TanStack Query busca os dados
  if (isLoading) {
    return (
      <Card className={styles.loadingCard}>
        <span className={styles.emptyText}>Carregando pacientes...</span>
      </Card>
    );
  }

  // Estado vazio: nenhum paciente cadastrado ou nenhum registro corresponde aos filtros atuais
  if (patients.length === 0) {
    return (
      <Card className={styles.emptyCard}>
        <p className={styles.emptyTitle}>Nenhum paciente encontrado</p>
        <span className={styles.emptyText}>
          Tente ajustar os filtros ou cadastrar um novo paciente.
        </span>
      </Card>
    );
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.thName}>NOME</th>
            <th>CPF</th>
            <th>TELEFONE</th>
            <th>CIDADE</th>
            <th>STATUS</th>
            <th className={styles.thAction}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {patients.map((patient) => {
            const isAtivo = patient.status === "ACTIVE";
            const location = [patient.city, patient.state].filter(Boolean).join(", ");

            return (
              <tr key={patient.id} className={styles.row}>
                {/* Nome do paciente acompanhado do avatar com as iniciais */}
                <td className={styles.tdName}>
                  <div className={styles.nameCell}>
                    <div className={styles.avatar}>
                      {getInitials(patient.name)}
                    </div>
                    <span className={styles.nameText}>{patient.name}</span>
                  </div>
                </td>

                {/* CPF formatado */}
                <td className={styles.tdData}>{patient.cpf}</td>

                {/* Telefone formatado */}
                <td className={styles.tdData}>{patient.phone}</td>

                {/* Cidade e UF */}
                <td className={styles.tdData}>{location || "--"}</td>

                {/* Tag indicadora de status: Ativo (verde) ou Inativo (cinza) */}
                <td>
                  <Badge variant={isAtivo ? "active" : "inactive"}>
                    {isAtivo ? "Ativo" : "Inativo"}
                  </Badge>
                </td>

                {/* Ação contextual baseada no papel do usuário logado */}
                <td className={styles.thAction}>
                  <button
                    type="button"
                    className={styles.actionButton}
                    onClick={() => onViewDetails?.(patient)}
                  >
                    {isAdmin ? "Ver / Editar" : "Ver"}
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
