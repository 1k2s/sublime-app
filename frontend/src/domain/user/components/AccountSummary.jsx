// Faixa do topo de Configurações: avatar, e-mail salvo, perfil e o selo de admin padrão.
// Mostra o que a API devolveu (o /me), não o que está sendo digitado
import { ShieldCheck } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { Badge } from "@/shared/components/Badge";
import { getInitials } from "@/shared/utils/text";
import styles from "./AccountSummary.module.css";

export function AccountSummary({ user }) {
  const roleLabel = getRole(user.role)?.label ?? user.role;

  return (
    <div className={styles.summary}>
      <span className={styles.avatar} aria-hidden="true">
        {getInitials(user.email.split("@")[0])}
      </span>

      <div className={styles.text}>
        <strong className={styles.email}>{user.email}</strong>
        <span className={styles.caption}>Conta de acesso ao portal</span>
      </div>

      <div className={styles.badges}>
        <Badge className={styles.role}>{roleLabel}</Badge>
        {user.defaultAdmin && (
          <Badge className={styles["default-admin"]} title="Não pode ser inativado nem mudar de perfil">
            <ShieldCheck size={13} aria-hidden />
            Admin padrão
          </Badge>
        )}
      </div>
    </div>
  );
}
