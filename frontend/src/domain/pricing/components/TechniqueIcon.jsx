// Ícone da técnica, escolhido por palavra-chave no nome. Cada um faz um gesto curto ao montar
import styles from "./TechniqueIcon.module.css";

function normalize(name) {
  return name.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

function techniqueKind(name) {
  const key = normalize(name ?? "");
  if (key.includes("pilates")) return "pilates";
  if (key.includes("rpg") || key.includes("postur")) return "posture";
  if (["liberacao", "miofascial", "massag", "drenagem"].some((word) => key.includes(word))) return "manual";
  return "generic";
}

function PilatesIcon() {
  return (
    <>
      <circle cx="12" cy="4.5" r="1.8" />
      <path d="M12 7.5v6.5" />
      <path d="M12 14l-3 6" />
      <path d="M12 14l3 6" />
      <path className={`${styles["technique-icon-arm"]} ${styles["technique-icon-arm-left"]}`} d="M12 9H6.5" />
      <path className={`${styles["technique-icon-arm"]} ${styles["technique-icon-arm-right"]}`} d="M12 9h5.5" />
      <path className={styles["technique-icon-mat"]} d="M5 21.5h14" />
    </>
  );
}

function PostureIcon() {
  return (
    <>
      <path className={styles["technique-icon-axis"]} d="M12 1.5v21" />
      <g className={styles["technique-icon-body"]}>
        <circle cx="12" cy="4.5" r="1.8" />
        <path d="M12 7.5v7" />
        <path d="M12 14.5l-2.5 6.5" />
        <path d="M12 14.5l2.5 6.5" />
        <path d="M8 10.5l4-1.5 4 1.5" />
      </g>
    </>
  );
}

function ManualIcon() {
  return (
    <>
      <circle className={styles["technique-icon-ripple"]} cx="12" cy="20" r="5" />
      <g className={styles["technique-icon-hand"]}>
        <path d="M18 11V6a2 2 0 0 0-4 0" />
        <path d="M14 10V4a2 2 0 0 0-4 0v2" />
        <path d="M10 10.5V6a2 2 0 0 0-4 0v8" />
        <path d="M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15" />
      </g>
    </>
  );
}

function GenericIcon() {
  return (
    <>
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path className={styles["technique-icon-check"]} d="M9 12.5l2 2 4-4.5" />
    </>
  );
}

const ICONS = { pilates: PilatesIcon, posture: PostureIcon, manual: ManualIcon, generic: GenericIcon };

export function TechniqueIcon({ name, size = 18, className }) {
  const Icon = ICONS[techniqueKind(name)];

  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      className={[styles["technique-icon"], className].filter(Boolean).join(" ")}
      aria-hidden
    >
      <Icon />
    </svg>
  );
}
