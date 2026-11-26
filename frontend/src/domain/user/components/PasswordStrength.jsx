// Barra de força da nova senha (fraca, média ou forte), logo abaixo do campo. Só a forte é aceita pela API
import { getPasswordStrength } from "../display/passwordStrength";
import styles from "./PasswordStrength.module.css";

const SEGMENTS = [1, 2, 3];

export function PasswordStrength({ password }) {
  const strength = getPasswordStrength(password);

  return (
    <div className={[styles.strength, strength && styles[strength.key]].filter(Boolean).join(" ")}>
      <div className={styles.bar} aria-hidden="true">
        {SEGMENTS.map((segment) => (
          <span key={segment} className={[styles.segment, strength?.filled >= segment && styles.filled].filter(Boolean).join(" ")} />
        ))}
      </div>
      <span className={styles.label} aria-live="polite">
        {strength ? `Força da senha: ${strength.label}` : "Força da senha"}
      </span>
    </div>
  );
}
