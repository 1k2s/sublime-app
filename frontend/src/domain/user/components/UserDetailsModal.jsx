import { useState } from "react";
import { Pencil, UserX } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { Badge } from "@/shared/ui/Badge";
import { Button } from "@/shared/ui/Button";
import { Modal } from "@/shared/ui/Modal";
import styles from "./UserDetailsModal.module.css";

export function UserDetailsModal({ user, isDeactivating = false, onEdit, onDeactivate, onClose }) {
  const [isConfirming, setIsConfirming] = useState(false);

  if (!user) return null;

  function confirmDeactivate() {
    onDeactivate?.(user);
    setIsConfirming(false);
  }

  return (
    <Modal title={user.email} onClose={onClose}>
      <div className={styles.content}>
        <Badge variant={user.active ? "active" : "inactive"} className={styles.status}>
          {user.active ? "Ativo" : "Inativo"}
        </Badge>

        <dl className={styles.grid}>
          <div className={styles.field}>
            <dt className={styles.label}>E-mail</dt>
            <dd className={styles.value}>{user.email}</dd>
          </div>
          <div className={styles.field}>
            <dt className={styles.label}>Perfil</dt>
            <dd className={styles.value}>{getRole(user.role)?.label ?? user.role}</dd>
          </div>
        </dl>

        {/* A API ainda não tem reativação de usuário */}
        {!user.active && <p className={styles.note}>Usuário inativo: não acessa o sistema.</p>}

        {isConfirming ? (
          <div className={styles.confirmBox}>
            <p className={styles.confirmText}>
              Deseja realmente inativar <strong>{user.email}</strong>? O acesso ao sistema é encerrado na hora.
            </p>
            <div className={styles.actions}>
              <Button variant="outline" onClick={() => setIsConfirming(false)}>
                Cancelar
              </Button>
              <button type="button" className={styles.dangerButton} onClick={confirmDeactivate} disabled={isDeactivating}>
                Confirmar inativação
              </button>
            </div>
          </div>
        ) : (
          <div className={styles.footer}>
            <div>
              {user.active && (
                <button type="button" className={styles.dangerButton} onClick={() => setIsConfirming(true)}>
                  <UserX size={16} /> Inativar usuário
                </button>
              )}
            </div>
            <div className={styles.actions}>
              <Button variant="outline" onClick={onClose}>
                Fechar
              </Button>
              <Button onClick={() => onEdit?.(user)}>
                <Pencil size={16} /> Editar
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
