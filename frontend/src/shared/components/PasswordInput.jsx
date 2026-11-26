// Campo de senha com o botão de mostrar e esconder o texto. Ícone à esquerda opcional (padrão: cadeado).
// Repassa o resto das props ao Input (o ref do register do React Hook Form inclusive)
import { useState } from "react";
import { Eye, EyeOff, Lock } from "lucide-react";
import { Input } from "./Input";
import styles from "./PasswordInput.module.css";

export function PasswordInput({ icon = Lock, ...props }) {
  const [isVisible, setIsVisible] = useState(false);
  const toggleLabel = isVisible ? "Esconder senha" : "Mostrar senha";
  const ToggleIcon = isVisible ? EyeOff : Eye;

  const toggle = (
    <button
      type="button"
      className={styles.toggle}
      onClick={() => setIsVisible((visible) => !visible)}
      aria-label={toggleLabel}
      aria-pressed={isVisible}
      title={toggleLabel}
    >
      <ToggleIcon size={18} aria-hidden />
    </button>
  );

  return <Input {...props} type={isVisible ? "text" : "password"} icon={icon} endSlot={toggle} />;
}
