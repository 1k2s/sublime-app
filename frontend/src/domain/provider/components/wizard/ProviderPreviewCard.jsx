import { Eye, User, Percent, Mail, Shield, Info } from "lucide-react";
import { Card } from "@/shared/ui/Card";
import { Badge } from "@/shared/ui/Badge";
import { ROLES } from "@/domain/auth/roles";
import styles from "./ProviderPreviewCard.module.css";

function getRoleLabel(roleValue) {
  return ROLES[roleValue]?.label || roleValue || "Prestador";
}

export function ProviderPreviewCard({ formData = {} }) {
  const name = formData.name?.trim() || "Nome do prestador";
  const commission = formData.commissionPercentage?.trim() || "40";
  const referralCommission = formData.referralCommissionPercentage?.trim() || "45";
  const email = formData.email?.trim() || "--";
  const role = getRoleLabel(formData.role || "PROVIDER");
  const status = formData.status || "ACTIVE";

  return (
    <Card className={styles.card}>
      <div className={styles.header}>
        <div className={styles.iconCircle}>
          <Eye size={20} />
        </div>
        <div>
          <h3 className={styles.title}>Prévia do cadastro</h3>
          <p className={styles.subtitle}>Confira as informações antes de salvar.</p>
        </div>
      </div>

      <div className={styles.itemsList}>
        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <User size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Prestador</span>
            <div className={styles.nameWithBadge}>
              <span className={styles.itemName}>{name}</span>
              <Badge variant={status === "ACTIVE" ? "active" : "inactive"}>
                {status === "ACTIVE" ? "Ativo" : "Inativo"}
              </Badge>
            </div>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Percent size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Comissões</span>
            <span className={styles.itemValue}>
              {commission}% padrão • {referralCommission}% indicação
            </span>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Mail size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>E-mail de acesso</span>
            <span className={styles.itemValue}>{email}</span>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Shield size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Perfil</span>
            <span className={styles.itemValue}>{role}</span>
          </div>
        </div>
      </div>

      <div className={styles.infoBanner}>
        <Info size={18} className={styles.infoIcon} />
        <span className={styles.infoText}>
          No primeiro login com a senha provisória, o prestador definirá sua
          própria senha pessoal.
        </span>
      </div>
    </Card>
  );
}
