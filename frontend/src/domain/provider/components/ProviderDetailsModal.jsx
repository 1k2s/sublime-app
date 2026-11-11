import { useState } from "react";
import {
  User,
  Mail,
  Percent,
  Shield,
  Pencil,
  AlertTriangle,
  UserX,
  UserCheck,
} from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { ROLES } from "@/domain/auth/roles";
import styles from "./ProviderDetailsModal.module.css";

function getRoleLabel(roleValue) {
  return ROLES[roleValue]?.label || roleValue || "Prestador";
}

export function ProviderDetailsModal({
  provider,
  onClose,
  onEdit,
  onToggleStatus,
}) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (!provider) return null;

  const isAtivo = provider.status === "ACTIVE";

  const handleConfirmToggle = () => {
    const nextStatus = isAtivo ? "INACTIVE" : "ACTIVE";
    onToggleStatus?.(provider, nextStatus);
    setShowConfirm(false);
  };

  const footer = (
    <div className={styles.footer}>
      <div>
        {onToggleStatus && !showConfirm && (
          isAtivo ? (
            <Button
              variant="danger"
              onClick={() => setShowConfirm(true)}
              title="Desativar prestador"
            >
              <UserX size={16} /> Desativar prestador
            </Button>
          ) : (
            <Button
              variant="secondary"
              onClick={() => setShowConfirm(true)}
              title="Reativar prestador"
            >
              <UserCheck size={16} /> Reativar prestador
            </Button>
          )
        )}
      </div>

      <div className={styles.footerActions}>
        <Button variant="outline" onClick={onClose}>
          Fechar
        </Button>

        {onEdit && !showConfirm && (
          <Button onClick={() => onEdit(provider)}>
            <Pencil size={16} /> Editar cadastro
          </Button>
        )}
      </div>
    </div>
  );

  return (
    <Modal
      title={provider.name}
      icon={User}
      titleExtra={<Badge variant={isAtivo ? "active" : "inactive"}>{isAtivo ? "Ativo" : "Inativo"}</Badge>}
      description="Detalhes do cadastro do prestador"
      footer={footer}
      onClose={onClose}
    >
      <div className={styles.content}>
        {/* Seção 1: Dados do Prestador e Comissões */}
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>Dados do Prestador e Comissões</h4>
          <div className={styles.grid}>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Percent size={14} /> Comissão Padrão (Clínica)
              </span>
              <span className={styles.fieldValue}>
                {provider.commissionPercentage}%
              </span>
            </div>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Percent size={14} /> Comissão por Indicação
              </span>
              <span className={styles.fieldValue}>
                {provider.referralCommissionPercentage}%
              </span>
            </div>
          </div>
        </div>

        {/* Seção 2: Conta de Acesso */}
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>Conta de Acesso (Usuário)</h4>
          <div className={styles.grid}>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Mail size={14} /> E-mail de Login
              </span>
              <span className={styles.fieldValue}>{provider.email || "--"}</span>
            </div>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Shield size={14} /> Perfil de Acesso
              </span>
              <span className={styles.fieldValue}>
                {getRoleLabel(provider.role)}
              </span>
            </div>
          </div>
        </div>

        {/* Caixa de Confirmação de Desativação / Reativação */}
        {showConfirm && (
          <div className={styles.confirmBox}>
            <h4 className={styles.confirmTitle}>
              Deseja realmente {isAtivo ? "desativar" : "reativar"} o prestador{" "}
              <strong>{provider.name}</strong>?
            </h4>

            <div className={styles.warningAlert}>
              <AlertTriangle size={18} className={styles.warningIcon} />
              <span className={styles.warningText}>
                <strong>Aviso:</strong> Prestadores inativos mantêm todo o histórico de
                atendimentos, mas não podem ser selecionados em novos lançamentos ou como
                origem de novos pacientes.
              </span>
            </div>

            <div className={styles.confirmActions}>
              <Button variant="outline" onClick={() => setShowConfirm(false)}>
                Cancelar
              </Button>
              <Button
                variant={isAtivo ? "danger" : "secondary"}
                onClick={handleConfirmToggle}
              >
                {isAtivo ? "Confirmar desativação" : "Confirmar reativação"}
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
