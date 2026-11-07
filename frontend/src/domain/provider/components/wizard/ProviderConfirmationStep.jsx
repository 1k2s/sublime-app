import { CheckCircle2, ArrowLeft, Check, User, KeyRound } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { Badge } from "@/shared/ui/Badge";
import { ROLES } from "@/domain/auth/roles";
import styles from "./ProviderWizard.module.css";

function getRoleLabel(roleValue) {
  return ROLES[roleValue]?.label || roleValue || "Prestador";
}

export function ProviderConfirmationStep({
  formData = {},
  isEditing = false,
  isSubmitting = false,
  onBack,
  onSubmit,
}) {
  const isAtivo = (formData.status || "ACTIVE") === "ACTIVE";
  const hasProvisionalPassword = Boolean(formData.password?.trim());

  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <CheckCircle2 size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Confirmação</h2>
          <p className={styles.cardSubtitle}>
            {isEditing
              ? "Revise todos os dados antes de salvar as alterações."
              : "Revise todos os dados antes de finalizar o cadastro do prestador."}
          </p>
        </div>
      </div>

      <div className={styles.reviewContent}>
        {/* Bloco 1: Dados do Prestador */}
        <div className={styles.reviewSection}>
          <div className={styles.reviewSectionHeader}>
            <User size={16} />
            <span>Dados do Prestador e Comissões</span>
          </div>

          <div className={styles.reviewGrid}>
            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Nome Completo</span>
              <span className={styles.reviewValue}>{formData.name || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Status</span>
              <div>
                <Badge variant={isAtivo ? "active" : "inactive"}>
                  {isAtivo ? "Ativo" : "Inativo"}
                </Badge>
              </div>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Comissão Padrão (Clínica)</span>
              <span className={styles.reviewValue}>
                {formData.commissionPercentage
                  ? `${formData.commissionPercentage}%`
                  : "--"}
              </span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Comissão por Indicação</span>
              <span className={styles.reviewValue}>
                {formData.referralCommissionPercentage
                  ? `${formData.referralCommissionPercentage}%`
                  : "--"}
              </span>
            </div>
          </div>
        </div>

        {/* Bloco 2: Conta de Acesso */}
        <div className={styles.reviewSection}>
          <div className={styles.reviewSectionHeader}>
            <KeyRound size={16} />
            <span>Conta de Acesso</span>
          </div>

          <div className={styles.reviewGrid}>
            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>E-mail de Login</span>
              <span className={styles.reviewValue}>{formData.email || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Perfil</span>
              <span className={styles.reviewValue}>
                {getRoleLabel(formData.role)}
              </span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Credencial de Acesso</span>
              <span className={styles.reviewValue}>
                {hasProvisionalPassword
                  ? "Senha provisória definida (troca obrigatória no 1º acesso)"
                  : "Manter senha atual"}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.cardFooter}>
        <Button
          variant="outline"
          type="button"
          onClick={onBack}
          disabled={isSubmitting}
        >
          <ArrowLeft size={18} /> Voltar
        </Button>
        <Button type="button" onClick={onSubmit} loading={isSubmitting}>
          <Check size={18} />{" "}
          {isEditing ? "Salvar alterações" : "Salvar prestador"}
        </Button>
      </div>
    </div>
  );
}
