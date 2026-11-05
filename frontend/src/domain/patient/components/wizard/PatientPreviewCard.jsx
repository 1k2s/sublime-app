/**
 * ======================================================================================
 * COMPONENTE: PatientPreviewCard
 * ======================================================================================
 *
 * Painel lateral de "Prévia do cadastro" exibido à direita do formulário (conforme Imagem 2):
 * - Permanece fixo durante a rolagem (`position: sticky`).
 * - Atualiza em tempo real enquanto o usuário digita nos campos das etapas.
 * - Exibe:
 *   1. Identificação do Paciente (Nome + Tag de status Ativo/Inativo).
 *   2. CPF formatado.
 *   3. Data de nascimento formatada.
 *   4. Telefone formatado.
 * - Banner informativo inferior com ícone `Info` lembrando que os dados podem ser revisados.
 */

import { Eye, User, FileText, Calendar, Phone, Info } from "lucide-react";
import { Card } from "@/shared/ui/Card";
import { Badge } from "@/shared/ui/Badge";
import styles from "./PatientPreviewCard.module.css";

/**
 * @param {Object} props
 * @param {Object} [props.formData={}] - Objeto de campos do formulário observado pelo useForm (watch)
 */
export function PatientPreviewCard({ formData = {} }) {
  // Valores com fallback visual amigável enquanto os campos estiverem vazios
  const name = formData.name?.trim() || "Nome do paciente";
  const cpf = formData.cpf?.trim() || "--";
  const birthDate = formData.birthDate?.trim() || "--";
  const phone = formData.phone?.trim() || "--";
  const status = formData.status || "ACTIVE";

  return (
    <Card className={styles.card}>
      {/* Cabeçalho do Card de Prévia */}
      <div className={styles.header}>
        <div className={styles.iconCircle}>
          <Eye size={20} />
        </div>
        <div>
          <h3 className={styles.title}>Prévia do cadastro</h3>
          <p className={styles.subtitle}>Confira as informações antes de salvar.</p>
        </div>
      </div>

      {/* Lista de itens com ícones circulares individuais */}
      <div className={styles.itemsList}>
        {/* Item 1: Nome do paciente e tag de status */}
        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <User size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Paciente</span>
            <div className={styles.nameWithBadge}>
              <span className={styles.itemName}>{name}</span>
              <Badge variant={status === "ACTIVE" ? "active" : "inactive"}>
                {status === "ACTIVE" ? "Ativo" : "Inativo"}
              </Badge>
            </div>
          </div>
        </div>

        {/* Item 2: CPF */}
        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <FileText size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>CPF</span>
            <span className={styles.itemValue}>{cpf}</span>
          </div>
        </div>

        {/* Item 3: Data de nascimento */}
        <div className={styles.item}>
          <div className={styles.itemIconCircle}>
            <Calendar size={18} />
          </div>
          <div className={styles.itemContent}>
            <span className={styles.itemLabel}>Data de nascimento</span>
            <span className={styles.itemValue}>{birthDate}</span>
          </div>
        </div>

        {/* Item 4: Telefone */}
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

      {/* Banner informativo no rodapé do painel */}
      <div className={styles.infoBanner}>
        <Info size={18} className={styles.infoIcon} />
        <span className={styles.infoText}>
          Você poderá revisar todos os dados antes de salvar o paciente.
        </span>
      </div>
    </Card>
  );
}
