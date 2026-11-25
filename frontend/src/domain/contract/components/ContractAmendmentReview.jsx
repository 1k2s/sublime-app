import { ArrowLeft, Check, CheckCircle2 } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import styles from "./ContractAmendmentReview.module.css";

// Passo 2 do aditivo: as "Alterações" (só o que mudou, do listAmendmentChanges) antes de salvar.
// "Voltar" volta ao formulário preenchido; "Confirmar aditivo" grava. changes: as linhas
// { key, label, text } ou { key, label, before, after }
export function ContractAmendmentReview({ patientName, changes = [], isSubmitting = false, onBack, onConfirm }) {
  const footer = (
    <div className={styles.footer}>
      <Button variant="outline" onClick={onBack} disabled={isSubmitting}>
        <ArrowLeft size={16} aria-hidden /> Voltar
      </Button>
      <Button onClick={onConfirm} loading={isSubmitting}>
        <Check size={16} aria-hidden /> Confirmar aditivo
      </Button>
    </div>
  );

  return (
    <Modal
      title={`Revisar aditivo · ${patientName ?? "Contrato"}`}
      icon={CheckCircle2}
      footer={footer}
      onClose={isSubmitting ? () => {} : onBack}
      closeOnBackdrop={false}
    >
      <section className={styles.summary}>
        <h3 className={styles.title}>Alterações</h3>
        <ul className={styles.changes}>
          {changes.map((change) => (
            <li key={change.key} className={styles.change}>
              <span className={styles.label}>{change.label}</span>
              {change.text != null ? (
                <span>{change.text}</span>
              ) : (
                <span>
                  <span className={styles.before}>{change.before}</span> → {change.after}
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>
    </Modal>
  );
}
