/**
 * ======================================================================================
 * COMPONENTE: PatientDetailsModal
 * ======================================================================================
 *
 * Modal aberto ao clicar na ação da tabela de pacientes ("Ver / Editar" ou "Ver"):
 * - Apresenta todos os dados do paciente divididos em duas seções claras:
 *   1. Dados Pessoais (CPF, Nascimento, Telefone, E-mail)
 *   2. Endereço Completo (CEP, Logradouro, Número, Complemento, Bairro, Cidade/UF)
 * - Exibe a tag de status (Ativo / Inativo).
 * - Fechamento via clique no backdrop escuro, no botão de "X" ou no botão "Fechar".
 * - Se o usuário for Administrador (`isAdmin = true`), disponibiliza o botão "Editar cadastro",
 *   que transiciona a página diretamente para o wizard de edição.
 */

import { X, User, Phone, Mail, Calendar, MapPin, CreditCard, Pencil } from "lucide-react";
import { Badge } from "@/shared/ui/Badge";
import { Button } from "@/shared/ui/Button";
import styles from "./PatientDetailsModal.module.css";

/**
 * @param {Object} props
 * @param {Object|null} props.patient - Dados do paciente selecionado (ou null se fechado)
 * @param {boolean} [props.isAdmin=false] - Indica se o usuário atual é administrador
 * @param {function(): void} props.onClose - Callback para fechar o modal
 * @param {function(Object): void} [props.onEdit] - Callback disparado ao clicar em "Editar cadastro"
 */
export function PatientDetailsModal({ patient, isAdmin = false, onClose, onEdit }) {
  // Se nenhum paciente estiver selecionado, não renderiza nada
  if (!patient) return null;

  const isAtivo = patient.status === "ACTIVE";

  return (
    /* Backdrop escurecido cobrindo a tela inteira */
    <div className={styles.backdrop} onClick={onClose}>
      {/* Container do modal: stopPropagation impede que cliques internos fechem o modal */}
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        {/* Cabeçalho do modal com nome, status e botão de fechar */}
        <div className={styles.header}>
          <div className={styles.titleInfo}>
            <div className={styles.iconCircle}>
              <User size={20} />
            </div>
            <div>
              <div className={styles.nameRow}>
                <h3 className={styles.name}>{patient.name}</h3>
                <Badge variant={isAtivo ? "active" : "inactive"}>
                  {isAtivo ? "Ativo" : "Inativo"}
                </Badge>
              </div>
              <p className={styles.subtitle}>Detalhes do cadastro do paciente</p>
            </div>
          </div>
          <button
            type="button"
            className={styles.closeButton}
            onClick={onClose}
            aria-label="Fechar"
            title="Fechar"
          >
            <X size={18} />
          </button>
        </div>

        {/* Corpo com as seções de informações */}
        <div className={styles.content}>
          {/* Seção 1: Dados Pessoais */}
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Dados Pessoais</h4>
            <div className={styles.grid}>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <CreditCard size={14} /> CPF
                </span>
                <span className={styles.fieldValue}>{patient.cpf || "--"}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Calendar size={14} /> Data de Nascimento
                </span>
                <span className={styles.fieldValue}>{patient.birthDate || "--"}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Phone size={14} /> Telefone
                </span>
                <span className={styles.fieldValue}>{patient.phone || "--"}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Mail size={14} /> E-mail
                </span>
                <span className={styles.fieldValue}>{patient.email || "--"}</span>
              </div>
            </div>
          </div>

          {/* Seção 2: Endereço */}
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Endereço</h4>
            <div className={styles.grid}>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <MapPin size={14} /> CEP
                </span>
                <span className={styles.fieldValue}>{patient.cep || "--"}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>Logradouro</span>
                <span className={styles.fieldValue}>
                  {patient.street ? `${patient.street}, ${patient.number || "S/N"}` : "--"}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>Complemento / Bairro</span>
                <span className={styles.fieldValue}>
                  {[patient.complement, patient.neighborhood].filter(Boolean).join(" - ") || "--"}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>Cidade / UF</span>
                <span className={styles.fieldValue}>
                  {[patient.city, patient.state].filter(Boolean).join(" / ") || "--"}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Rodapé com botões de ação */}
        <div className={styles.footer}>
          <Button variant="outline" onClick={onClose}>
            Fechar
          </Button>

          {/* Botão de edição exclusivo para perfil administrador */}
          {isAdmin && onEdit && (
            <Button onClick={() => onEdit(patient)}>
              <Pencil size={16} /> Editar cadastro
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
