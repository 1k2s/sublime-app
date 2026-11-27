// Confirmação da senha atual, aberta pelo "Salvar alterações" de Configurações: a API só aceita
// a alteração com ela. Fica aberta durante o salvamento e num erro (ex: senha atual errada), para
// o usuário tentar de novo; quem fecha no sucesso é o formulário
import { useState } from "react";
import { KeyRound } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Modal } from "@/shared/components/Modal";
import { PasswordInput } from "@/shared/components/PasswordInput";
import styles from "./CurrentPasswordModal.module.css";

export function CurrentPasswordModal({ onConfirm, onClose, isSubmitting = false }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [error, setError] = useState(null);

  function handleSubmit(event) {
    event.preventDefault();
    if (!currentPassword) {
      setError("Informe a senha atual");
      return;
    }
    onConfirm(currentPassword);
  }

  return (
    <Modal
      title="Confirme que é você"
      icon={KeyRound}
      description="Digite sua senha atual para salvar as alterações."
      onClose={isSubmitting ? () => {} : onClose}
    >
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <FormField label="Senha atual" htmlFor="confirm-current-password" error={error}>
          <PasswordInput
            id="confirm-current-password"
            autoComplete="current-password"
            icon={KeyRound}
            autoFocus
            invalid={Boolean(error)}
            value={currentPassword}
            onChange={(event) => {
              setCurrentPassword(event.target.value);
              setError(null);
            }}
          />
        </FormField>

        <div className={styles.actions}>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Confirmar e salvar
          </Button>
        </div>
      </form>
    </Modal>
  );
}
