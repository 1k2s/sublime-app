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
import { getRole } from "@/domain/auth/roles";
import { formatPercentage } from "@/shared/utils/money";
import styles from "./ProviderDetailsModal.module.css";

export function ProviderDetailsModal({
  provider,
  isTogglingStatus = false,
  onClose,
  onEdit,
  onToggleStatus,
}) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (!provider) return null;

  const isActive = provider.active;

  const handleConfirmToggle = () => {
    onToggleStatus?.(provider);
    setShowConfirm(false);
  };

  // O componente continua montado com o modal fechado: sem zerar, a confirmação
  // reapareceria aberta no próximo prestador
  const close = () => {
    setShowConfirm(false);
    onClose?.();
  };

  const footer = (
    <div className={styles.footer}>
      <div>
        {onToggleStatus && !showConfirm && (
          isActive ? (
            <Button
              variant="danger"
              onClick={() => setShowConfirm(true)}
              disabled={isTogglingStatus}
              title="Inativar prestador"
            >
              <UserX size={16} /> Inativar prestador
            </Button>
          ) : (
            <Button
              variant="secondary"
              onClick={() => setShowConfirm(true)}
              disabled={isTogglingStatus}
              title="Reativar prestador"
            >
              <UserCheck size={16} /> Reativar prestador
            </Button>
          )
        )}
      </div>

      <div className={styles.footerActions}>
        <Button variant="outline" onClick={close}>
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
      titleExtra={<Badge variant={isActive ? "active" : "inactive"}>{isActive ? "Ativo" : "Inativo"}</Badge>}
      description="Detalhes do cadastro do prestador"
      footer={footer}
      onClose={close}
    >
      <div className={styles.content}>
        {/* Seção 1: Dados do Prestador e Comissões */}
        <div className={styles.section}>
          <h4 className={styles.sectionTitle}>Dados do Prestador e Comissões</h4>
          <div className={styles.grid}>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Percent size={14} /> Comissão Padrão
              </span>
              <span className={styles.fieldValue}>
                {formatPercentage(provider.commissionPercentage)}
              </span>
            </div>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <Percent size={14} /> Comissão de Pacientes que Trouxe
              </span>
              <span className={styles.fieldValue}>
                {formatPercentage(provider.referralCommissionPercentage)}
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
                {getRole(provider.role)?.label ?? "--"}
              </span>
            </div>
            <div className={styles.field}>
              <span className={styles.fieldLabel}>
                <User size={14} /> Status do Usuário
              </span>
              <span className={styles.fieldValue}>
                <Badge variant={provider.userActive ? "active" : "inactive"}>
                  {provider.userActive ? "Ativo" : "Inativo"}
                </Badge>
              </span>
            </div>
          </div>
          {/* Reativar o prestador exige usuário ativo (ver Provider, no domain-model) */}
          {!provider.userActive && (
            <p className={styles.note}>
              Usuário inativo: o prestador não acessa o sistema. Para reativar o prestador,
              reative antes o usuário na tela de Usuários.
            </p>
          )}
        </div>

        {/* Caixa de Confirmação de Inativação / Reativação */}
        {showConfirm && (
          <div className={styles.confirmBox}>
            <h4 className={styles.confirmTitle}>
              Deseja realmente {isActive ? "inativar" : "reativar"} o prestador{" "}
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
                variant={isActive ? "danger" : "secondary"}
                onClick={handleConfirmToggle}
                disabled={isTogglingStatus}
              >
                {isActive ? "Confirmar inativação" : "Confirmar reativação"}
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
