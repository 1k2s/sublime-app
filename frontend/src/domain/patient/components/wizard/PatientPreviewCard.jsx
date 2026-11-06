import { Eye, User, FileText, Calendar, Phone, Info } from "lucide-react";
import { Card } from "@/shared/ui/Card";
import styles from "./PatientPreviewCard.module.css";

export function PatientPreviewCard({ formData = {} }) {
  const name = formData.name?.trim() || "Nome do paciente";
  const cpf = formData.cpf?.trim() || "--";
  const birthDate = formData.birthDate?.trim() || "--";
  const phone = formData.phone?.trim() || "Não informado";

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
            <span className={styles.itemLabel}>Paciente</span>
            <span className={styles.itemName}>{name}</span>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <FileText size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>CPF</span>
            <span className={styles.itemValue}>{cpf}</span>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Calendar size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Data de nascimento</span>
            <span className={styles.itemValue}>{birthDate}</span>
          </div>
        </div>

        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Phone size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Telefone</span>
            <span className={styles.itemValue}>{phone}</span>
          </div>
        </div>
      </div>

      <div className={styles.infoBanner}>
        <Info size={18} className={styles.infoIcon} />
        <span className={styles.infoText}>
          Você poderá revisar todos os dados antes de salvar o paciente.
        </span>
      </div>
    </Card>
  );
}
