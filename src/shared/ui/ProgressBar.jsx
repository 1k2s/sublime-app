// Barra de progresso em segmentos, um por etapa: os concluídos ficam cheios e o atual, destacado
import styles from "./ProgressBar.module.css";

export function ProgressBar({ value, max, label, valueText }) {
  const current = Math.min(Math.max(value, 0), max);

  return (
    <div className={styles["progress-bar"]}>
      {(label || valueText) && (
        <div className={styles["progress-bar-header"]}>
          <span className={styles["progress-bar-label"]}>{label}</span>
          <span className={styles["progress-bar-value-text"]}>{valueText}</span>
        </div>
      )}

      <div
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={current}
        aria-valuetext={valueText}
        className={styles["progress-bar-track"]}
        style={{ "--segments": max }}
      >
        {Array.from({ length: max }, (_, index) => {
          const status = index < current ? styles["progress-bar-segment-done"] : index === current ? styles["progress-bar-segment-current"] : "";
          return <span key={index} className={`${styles["progress-bar-segment"]} ${status}`} />;
        })}
      </div>
    </div>
  );
}
