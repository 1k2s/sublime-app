/**
 * ======================================================================================
 * COMPONENTE: ConfirmationStep (Etapa 3 do Wizard)
 * ======================================================================================
 *
 * Apresenta a revisão consolidada de todos os dados antes da persistência final:
 * - Seção 1: Dados Pessoais (Nome, Status, CPF, Nascimento, Telefone, E-mail).
 * - Seção 2: Endereço (CEP, Logradouro, Número, Complemento, Bairro, Cidade/UF).
 * - Mensagens e botão contextualizados:
 *   - Modo Criação (`isEditing = false`): texto "Salvar paciente".
 *   - Modo Edição (`isEditing = true`): texto "Salvar alterações".
 * - Feedback de carregamento (`loading={isSubmitting}`) e bloqueio do botão Voltar durante o envio.
 */

import { CheckCircle2, ArrowLeft, Check, User, MapPin } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { Badge } from "@/shared/ui/Badge";
import styles from "./PatientWizard.module.css";

/**
 * @param {Object} props
 * @param {Object} props.formData - Objeto consolidado com os valores de todos os campos preenchidos
 * @param {boolean} [props.isEditing=false] - Indica se o fluxo atual é de edição
 * @param {boolean} [props.isSubmitting=false] - Indica se a mutação da API está em andamento
 * @param {function(): void} props.onBack - Retorna para a etapa 2 (Endereço)
 * @param {function(): void} props.onSubmit - Dispara a submissão e gravação dos dados
 */
export function ConfirmationStep({
  formData = {},
  isEditing = false,
  isSubmitting = false,
  onBack,
  onSubmit,
}) {
  const isAtivo = formData.status === "ACTIVE";

  return (
    <div className={styles.stepContainer}>
      {/* Cabeçalho da Etapa 3 */}
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

      {/* Conteúdo de Revisão */}
      <div className={styles.reviewContent}>
        {/* Bloco de Dados Pessoais */}
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
              <span className={styles.reviewLabel}>Status Inicial</span>
              <div>
                <Badge variant={isAtivo ? "active" : "inactive"}>
                  {isAtivo ? "Ativo" : "Inativo"}
                </Badge>
              </div>
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
              <span className={styles.reviewValue}>{formData.phone || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>E-mail</span>
              <span className={styles.reviewValue}>{formData.email || "Não informado"}</span>
            </div>
          </div>
        </div>

        {/* Bloco de Endereço */}
        <div className={styles.reviewSection}>
          <div className={styles.reviewSectionHeader}>
            <MapPin size={16} />
            <span>Endereço</span>
          </div>

          <div className={styles.reviewGrid}>
            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>CEP</span>
              <span className={styles.reviewValue}>{formData.cep || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Logradouro e Número</span>
              <span className={styles.reviewValue}>
                {formData.street ? `${formData.street}, ${formData.number || "S/N"}` : "--"}
              </span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Complemento</span>
              <span className={styles.reviewValue}>{formData.complement || "Nenhum"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Bairro</span>
              <span className={styles.reviewValue}>{formData.neighborhood || "--"}</span>
            </div>

            <div className={styles.reviewField}>
              <span className={styles.reviewLabel}>Cidade / UF</span>
              <span className={styles.reviewValue}>
                {[formData.city, formData.state].filter(Boolean).join(" / ") || "--"}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Rodapé com botão de voltar e ação final de salvar */}
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
