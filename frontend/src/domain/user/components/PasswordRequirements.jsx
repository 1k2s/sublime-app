// Conferência da nova senha enquanto o usuário digita: os critérios da senha forte (PASSWORD_CRITERIA,
// os mesmos do @StrongPassword da API) e a confirmação igual
import { Circle, CircleCheck } from "lucide-react";
import { PASSWORD_CRITERIA } from "@/domain/auth/schemas/passwordRule";
import styles from "./PasswordRequirements.module.css";

export function PasswordRequirements({ newPassword, confirmNewPassword }) {
  const requirements = [
    ...PASSWORD_CRITERIA.map(({ label, test }) => ({ label, isMet: test(newPassword) })),
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
