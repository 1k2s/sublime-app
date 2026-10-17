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
 * - Efeito visual de blur no backdrop escurecido (`backdrop-filter: blur(5px)`).
 * - Fechamento via clique no backdrop escuro, no botão de "X" ou no botão "Fechar".
 * - Se o usuário for Administrador (`isAdmin = true`):
 *   1. Disponibiliza o botão "Editar cadastro" (que abre o wizard de edição).
 *   2. Disponibiliza o botão de desativar/reativar paciente com pedido de confirmação
 *      e alerta explicativo sobre a futura verificação de pendências contratuais.
 */

import { useState } from "react";
import {
  X,
  User,
  Phone,
  Mail,
  Calendar,
  MapPin,
  CreditCard,
  Pencil,
  AlertTriangle,
  UserX,
  UserCheck,
} from "lucide-react";
import { Badge } from "@/shared/ui/Badge";
import { Button } from "@/shared/ui/Button";
import styles from "./PatientDetailsModal.module.css";

/**
 * @param {Object} props
 * @param {Object|null} props.patient - Dados do paciente selecionado (ou null se fechado)
 * @param {boolean} [props.isAdmin=false] - Indica se o usuário atual é administrador
 * @param {function(): void} props.onClose - Callback para fechar o modal
 * @param {function(Object): void} [props.onEdit] - Callback disparado ao clicar em "Editar cadastro"
 * @param {function(Object, string): void} [props.onToggleStatus] - Callback para alternar o status do paciente
 */
export function PatientDetailsModal({
  patient,
  isAdmin = false,
  onClose,
  onEdit,
  onToggleStatus,
}) {
  const [showConfirm, setShowConfirm] = useState(false);

  // Se nenhum paciente estiver selecionado, não renderiza nada
  if (!patient) return null;

  const isAtivo = patient.status === "ACTIVE";

  const handleConfirmToggle = () => {
    const nextStatus = isAtivo ? "INACTIVE" : "ACTIVE";
    onToggleStatus?.(patient, nextStatus);
    setShowConfirm(false);
  };

  return (
    /* Backdrop escurecido com desfoque blur cobrindo a tela inteira */
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

          {/* Caixa de Confirmação de Desativação com Alerta de Pendências */}
          {showConfirm && (
            <div className={styles.confirmBox}>
              <h4 className={styles.confirmTitle}>
                Deseja realmente {isAtivo ? "desativar" : "reativar"} o paciente{" "}
                <strong>{patient.name}</strong>?
              </h4>

              {/* Alerta de verificação futura de contratos e pendências */}
              <div className={styles.warningAlert}>
                <AlertTriangle size={18} className={styles.warningIcon} />
                <span className={styles.warningText}>
                  <strong>Aviso:</strong> Em versões futuras, a desativação verificará automaticamente
                  se existem contratos vigentes, mensalidades em aberto e agendamentos pendentes antes de ser concluída.
                </span>
              </div>

              <div className={styles.confirmActions}>
                <Button variant="outline" onClick={() => setShowConfirm(false)}>
                  Cancelar
                </Button>
                <button
                  type="button"
                  className={isAtivo ? styles.dangerOutlineButton : styles.reactivateButton}
                  onClick={handleConfirmToggle}
                >
                  {isAtivo ? "Confirmar desativação" : "Confirmar reativação"}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Rodapé com botões de ação */}
        <div className={styles.footer}>
          {/* Lado esquerdo: Botão de Desativar ou Reativar paciente (apenas para Admin) */}
          <div>
            {isAdmin && onToggleStatus && !showConfirm && (
              isAtivo ? (
                <button
                  type="button"
                  className={styles.dangerOutlineButton}
                  onClick={() => setShowConfirm(true)}
                  title="Desativar paciente"
                >
                  <UserX size={16} /> Desativar paciente
                </button>
              ) : (
                <button
                  type="button"
                  className={styles.reactivateButton}
                  onClick={() => setShowConfirm(true)}
                  title="Reativar paciente"
                >
                  <UserCheck size={16} /> Reativar paciente
                </button>
              )
            )}
          </div>

          {/* Lado direito: Fechar e Editar */}
          <div className={styles.footerActions}>
            <Button variant="outline" onClick={onClose}>
              Fechar
            </Button>

            {/* Botão de edição exclusivo para perfil administrador */}
            {isAdmin && onEdit && !showConfirm && (
              <Button onClick={() => onEdit(patient)}>
                <Pencil size={16} /> Editar cadastro
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
