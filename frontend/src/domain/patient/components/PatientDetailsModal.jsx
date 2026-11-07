// Modal de detalhes do paciente, com edição e inativação/reativação para perfil ADMIN
import { useState } from "react";
import {
  Calendar,
  CreditCard,
  Info,
  Mail,
  MapPin,
  Pencil,
  Phone,
  User,
  UserCheck,
  UserX,
  X,
} from "lucide-react";
import { Badge } from "@/shared/ui/Badge";
import { Button } from "@/shared/ui/Button";
import { formatDate } from "@/shared/utils/date";
import { maskCep, maskCpf, maskPhone } from "@/shared/utils/masks";
import styles from "@/domain/patient/components/PatientDetailsModal.module.css";

export function PatientDetailsModal({
  patient,
  isAdmin = false,
  isTogglingStatus = false,
  onClose,
  onEdit,
  onToggleStatus,
}) {
  const [showConfirm, setShowConfirm] = useState(false);

  if (!patient) return null;

  const isActive = Boolean(patient.active);
  const address = patient.address;

  function handleConfirmToggle() {
    onToggleStatus?.(patient);
    setShowConfirm(false);
  }

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <div className={styles.titleInfo}>
            <div className={styles.iconCircle}>
              <User size={20} />
            </div>
            <div>
              <div className={styles.nameRow}>
                <h3 className={styles.name}>{patient.name}</h3>
                <Badge variant={isActive ? "active" : "inactive"}>
                  {isActive ? "Ativo" : "Inativo"}
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

        <div className={styles.content}>
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Dados Pessoais</h4>
            <div className={styles.grid}>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <CreditCard size={14} /> CPF
                </span>
                <span className={styles.fieldValue}>
                  {patient.cpf ? maskCpf(patient.cpf) : "--"}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Calendar size={14} /> Data de Nascimento
                </span>
                <span className={styles.fieldValue}>
                  {formatDate(patient.birthDate) || "--"}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Phone size={14} /> Telefone
                </span>
                <span className={styles.fieldValue}>
                  {patient.phone ? maskPhone(patient.phone) : "--"}
                </span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <Mail size={14} /> E-mail
                </span>
                <span className={styles.fieldValue}>{patient.email || "--"}</span>
              </div>
              <div className={styles.field}>
                <span className={styles.fieldLabel}>
                  <UserCheck size={14} /> Origem do Paciente
                </span>
                <span className={styles.fieldValue}>
                  {patient.referringProviderName || "Clínica"}
                </span>
              </div>
            </div>
          </div>

          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Endereço</h4>
            {address ? (
              <div className={styles.grid}>
                <div className={styles.field}>
                  <span className={styles.fieldLabel}>
                    <MapPin size={14} /> CEP
                  </span>
                  <span className={styles.fieldValue}>
                    {address.cep ? maskCep(address.cep) : "--"}
                  </span>
                </div>
                <div className={styles.field}>
                  <span className={styles.fieldLabel}>Rua e Número</span>
                  <span className={styles.fieldValue}>
                    {address.street
                      ? `${address.street}, ${address.numberHouse || "S/N"}`
                      : "--"}
                  </span>
                </div>
                <div className={styles.field}>
                  <span className={styles.fieldLabel}>Complemento</span>
                  <span className={styles.fieldValue}>{address.complement || "--"}</span>
                </div>
                <div className={styles.field}>
                  <span className={styles.fieldLabel}>Cidade</span>
                  <span className={styles.fieldValue}>{address.city || "--"}</span>
                </div>
              </div>
            ) : (
              <span className={styles.fieldValue}>Endereço não informado</span>
            )}
          </div>

          {showConfirm && (
            <div className={styles.confirmBox}>
              <h4 className={styles.confirmTitle}>
                Deseja realmente {isActive ? "inativar" : "reativar"} o paciente{" "}
                <strong>{patient.name}</strong>?
              </h4>

              <div className={styles.infoAlert}>
                <Info size={18} className={styles.infoIcon} />
                <span className={styles.infoText}>
                  {isActive
                    ? "O paciente ficará inativo e não poderá entrar em contratos ou atendimentos novos, mas contratos e atendimentos antigos continuam preservados."
                    : "O paciente voltará a ficar ativo e poderá entrar em contratos e atendimentos novos."}
                </span>
              </div>

              <div className={styles.confirmActions}>
                <Button
                  variant="outline"
                  onClick={() => setShowConfirm(false)}
                  disabled={isTogglingStatus}
                >
                  Cancelar
                </Button>
                <button
                  type="button"
                  className={isActive ? styles.dangerOutlineButton : styles.reactivateButton}
                  onClick={handleConfirmToggle}
                  disabled={isTogglingStatus}
                >
                  {isActive ? "Confirmar inativação" : "Confirmar reativação"}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className={styles.footer}>
          <div>
            {isAdmin && onToggleStatus && !showConfirm && (
              isActive ? (
                <button
                  type="button"
                  className={styles.dangerOutlineButton}
                  onClick={() => setShowConfirm(true)}
                  disabled={isTogglingStatus}
                  title="Inativar paciente"
                >
                  <UserX size={16} /> Inativar paciente
                </button>
              ) : (
                <button
                  type="button"
                  className={styles.reactivateButton}
                  onClick={() => setShowConfirm(true)}
                  disabled={isTogglingStatus}
                  title="Reativar paciente"
                >
                  <UserCheck size={16} /> Reativar paciente
                </button>
              )
            )}
          </div>

          <div className={styles.footerActions}>
            <Button variant="outline" onClick={onClose}>
              Fechar
            </Button>

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
