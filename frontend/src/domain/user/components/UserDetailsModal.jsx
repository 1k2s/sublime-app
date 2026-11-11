import { useState } from "react";
import { Pencil, UserCheck, UserCog, UserX } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import styles from "./UserDetailsModal.module.css";

export function UserDetailsModal({ user, isTogglingStatus = false, onEdit, onToggleStatus, onClose }) {
  const [isConfirming, setIsConfirming] = useState(false);

  if (!user) return null;

  // O admin padrão não pode ser inativado (a API responde 409): a ação nem aparece.
  // Ver "Admin padrão" em User, no domain-model
  const canToggleStatus = !user.defaultAdmin;

  function confirmToggle() {
    onToggleStatus?.(user);
    setIsConfirming(false);
  }

  function close() {
    setIsConfirming(false);
    onClose?.();
  }

  const badges = (
    <>
      <Badge variant={user.active ? "active" : "inactive"}>{user.active ? "Ativo" : "Inativo"}</Badge>
      {user.defaultAdmin && <Badge>Administrador padrão</Badge>}
    </>
  );

  // Durante a confirmação, o rodapé sai: as ações ficam na caixa de confirmação
  const footer = isConfirming ? null : (
    <div className={styles.footer}>
      <div>
        {canToggleStatus &&
          (user.active ? (
            <Button variant="danger" onClick={() => setIsConfirming(true)} disabled={isTogglingStatus}>
              <UserX size={16} /> Inativar usuário
            </Button>
          ) : (
            <Button variant="secondary" onClick={() => setIsConfirming(true)} disabled={isTogglingStatus}>
              <UserCheck size={16} /> Reativar usuário
            </Button>
          ))}
      </div>
      <div className={styles.actions}>
        <Button variant="outline" onClick={close}>
          Fechar
        </Button>
        <Button onClick={() => onEdit?.(user)}>
          <Pencil size={16} /> Editar
        </Button>
      </div>
    </div>
  );

  return (
    <Modal
      title={user.email}
      icon={UserCog}
      titleExtra={badges}
      description="Detalhes do acesso do usuário"
      footer={footer}
      onClose={close}
    >
      <div className={styles.content}>
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

        {user.defaultAdmin && (
          <p className={styles.note}>O administrador padrão não pode ser inativado nem mudar de perfil.</p>
        )}
        {!user.active && <p className={styles.note}>Usuário inativo: não acessa o sistema.</p>}

        {isConfirming && (
          <div className={styles.confirmBox}>
            <p className={styles.confirmText}>
              {user.active ? (
                <>
                  Deseja realmente inativar <strong>{user.email}</strong>? O acesso ao sistema é encerrado na hora.
                </>
              ) : (
                <>
                  Deseja reativar <strong>{user.email}</strong>? O usuário volta a acessar o sistema.
                </>
              )}
            </p>
            <div className={styles.actions}>
              <Button variant="outline" onClick={() => setIsConfirming(false)}>
                Cancelar
              </Button>
              <Button
                variant={user.active ? "danger" : "secondary"}
                onClick={confirmToggle}
                disabled={isTogglingStatus}
              >
                {user.active ? "Confirmar inativação" : "Confirmar reativação"}
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
