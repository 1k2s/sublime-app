import { useState } from "react";
import { Pencil, UserCheck, UserX } from "lucide-react";
import { Badge } from "@/shared/ui/Badge";
import { Button } from "@/shared/ui/Button";
import { Modal } from "@/shared/ui/Modal";
import { formatPercentage } from "../percentage";
import styles from "./ProviderDetailsModal.module.css";

export function ProviderDetailsModal({ provider, userEmail, isAdmin = false, isToggling = false, onEdit, onToggleStatus, onClose }) {
  const [isConfirming, setIsConfirming] = useState(false);

  if (!provider) return null;

  const isActive = provider.active;

  function confirmToggle() {
    onToggleStatus?.(provider);
    setIsConfirming(false);
  }

  return (
    <Modal title={provider.name} onClose={onClose}>
      <div className={styles.content}>
        <Badge variant={isActive ? "active" : "inactive"} className={styles.status}>
          {isActive ? "Ativo" : "Inativo"}
        </Badge>

        <dl className={styles.grid}>
          <div className={styles.field}>
            <dt className={styles.label}>Usuário de acesso</dt>
            <dd className={styles.value}>{userEmail || "--"}</dd>
          </div>
          <div className={styles.field}>
            <dt className={styles.label}>Comissão padrão</dt>
            <dd className={styles.value}>{formatPercentage(provider.commissionPercentage)}</dd>
          </div>
          <div className={styles.field}>
            <dt className={styles.label}>Pacientes que trouxe</dt>
            <dd className={styles.value}>{formatPercentage(provider.referralCommissionPercentage)}</dd>
          </div>
        </dl>

        {isConfirming && (
          <div className={styles.confirmBox}>
            <p className={styles.confirmText}>
              Deseja realmente {isActive ? "inativar" : "reativar"} o prestador <strong>{provider.name}</strong>?
            </p>
            <div className={styles.actions}>
              <Button variant="outline" onClick={() => setIsConfirming(false)}>
                Cancelar
              </Button>
              <button
                type="button"
                className={isActive ? styles.dangerButton : styles.reactivateButton}
                onClick={confirmToggle}
                disabled={isToggling}
              >
                {isActive ? "Confirmar inativação" : "Confirmar reativação"}
              </button>
            </div>
          </div>
        )}

        {!isConfirming && (
          <div className={styles.footer}>
            <div>
              {isAdmin && (
                <button
                  type="button"
                  className={isActive ? styles.dangerButton : styles.reactivateButton}
                  onClick={() => setIsConfirming(true)}
                >
                  {isActive ? <UserX size={16} /> : <UserCheck size={16} />}
                  {isActive ? "Inativar prestador" : "Reativar prestador"}
                </button>
              )}
            </div>
            <div className={styles.actions}>
              <Button variant="outline" onClick={onClose}>
                Fechar
              </Button>
              {isAdmin && (
                <Button onClick={() => onEdit?.(provider)}>
                  <Pencil size={16} /> Editar
                </Button>
              )}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
