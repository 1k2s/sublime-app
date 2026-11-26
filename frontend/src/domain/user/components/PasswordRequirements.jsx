// Conferência da nova senha enquanto o usuário digita: as mesmas regras de formato do settingsSchema
import { Circle, CircleCheck } from "lucide-react";
import { MIN_PASSWORD_LENGTH } from "../schemas/settingsSchema";
import styles from "./PasswordRequirements.module.css";

export function PasswordRequirements({ newPassword, confirmNewPassword }) {
  const requirements = [
    { label: `Pelo menos ${MIN_PASSWORD_LENGTH} caracteres`, isMet: newPassword.length >= MIN_PASSWORD_LENGTH },
    { label: "Confirmação igual à nova senha", isMet: newPassword !== "" && newPassword === confirmNewPassword },
  ];

  return (
    <ul className={styles.list} aria-label="Requisitos da nova senha">
      {requirements.map(({ label, isMet }) => {
        const Icon = isMet ? CircleCheck : Circle;
        return (
          <li key={label} className={isMet ? styles.met : undefined}>
            <Icon size={14} aria-hidden />
            {label}
            <span className={styles["sr-only"]}>{isMet ? " (atendido)" : " (pendente)"}</span>
          </li>
        );
      })}
    </ul>
  );
}
