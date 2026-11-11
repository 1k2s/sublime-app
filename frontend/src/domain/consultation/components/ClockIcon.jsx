import styles from "./ClockIcon.module.css";

export function ClockIcon({ hours = 0, minutes = 0, size = 18, className }) {
  const hourAngle = ((hours % 12) + minutes / 60) * 30;
  const minuteAngle = minutes * 6;

  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      className={[styles["clock-icon"], className].filter(Boolean).join(" ")}
      aria-hidden
    >
      <circle cx="12" cy="12" r="9" />
      <path className={styles["clock-icon-hand"]} d="M12 12V8" style={{ transform: `rotate(${hourAngle}deg)` }} />
      <path className={styles["clock-icon-hand"]} d="M12 12V5.5" style={{ transform: `rotate(${minuteAngle}deg)` }} />
    </svg>
  );
}
