import { CheckCircle2, ArrowLeft, Check, User, MapPin } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { hasAddress } from "../../patientForm";
import styles from "./PatientWizard.module.css";

export function ConfirmationStep({
  formData = {},
  isEditing = false,
  isSubmitting = false,
  onBack,
  onSubmit,
}) {
  const withAddress = hasAddress(formData);

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
              : "Revise todos os dados antes de finalizar o cadastro."}
          </p>
        </div>
      </div>

      <div className={styles.reviewContent}>
        <div className={styles.reviewSection}>
          <div className={styles.reviewSectionHeader}>
            <User size={16} />
            <span>Dados Pessoais</span>
          </div>

          <div className={styles.reviewGrid}>
            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Nome Completo</span>
              <span className={styles.reviewValue}>{formData.name || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>CPF</span>
              <span className={styles.reviewValue}>{formData.cpf || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Data de Nascimento</span>
              <span className={styles.reviewValue}>{formData.birthDate || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Telefone</span>
              <span className={styles.reviewValue}>{formData.phone || "Não informado"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>E-mail</span>
              <span className={styles.reviewValue}>{formData.email || "Não informado"}</span>
            </div>
          </div>
        </div>

        <div className={styles.reviewSection}>
          <div className={styles.reviewSectionHeader}>
            <MapPin size={16} />
            <span>Endereço</span>
          </div>

          {withAddress ? (
            <div className={styles.reviewGrid}>
              <div className={styles.reviewField}>
                <span className={styles.reviewLabel}>CEP</span>
                <span className={styles.reviewValue}>{formData.cep || "--"}</span>
              </div>

              <div className={styles.reviewField}>
                <span className={styles.reviewLabel}>Logradouro e Número</span>
                <span className={styles.reviewValue}>
                  {formData.street ? `${formData.street}, ${formData.numberHouse || "--"}` : "--"}
                </span>
              </div>

              <div className={styles.reviewField}>
                <span className={styles.reviewLabel}>Complemento</span>
                <span className={styles.reviewValue}>{formData.complement || "Nenhum"}</span>
              </div>

              <div className={styles.reviewField}>
                <span className={styles.reviewLabel}>Cidade</span>
                <span className={styles.reviewValue}>{formData.city || "--"}</span>
              </div>
            </div>
          ) : (
            <span className={styles.reviewValue}>Não informado</span>
          )}
        </div>
      </div>

      <div className={styles.cardFooter}>
        <Button variant="outline" type="button" onClick={onBack} disabled={isSubmitting}>
          <ArrowLeft size={18} /> Voltar
        </Button>
        <Button type="button" onClick={onSubmit} loading={isSubmitting}>
          <Check size={18} /> {isEditing ? "Salvar alterações" : "Salvar paciente"}
        </Button>
      </div>
    </div>
  );
}
