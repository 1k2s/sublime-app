// Trilha vertical de etapas que ocupa a altura que o pai der, sem empurrar os vizinhos
import { Check } from "lucide-react";
import { ProgressRobot } from "./ProgressRobot";
import styles from "./ProgressSteps.module.css";

export function ProgressSteps({ label, steps, value, showMascot = false, className }) {
  const current = Math.min(Math.max(value, 0), steps.length);
  const isComplete = current === steps.length;
  const valueText = isComplete ? "Todas as etapas concluídas" : `${steps[current]}, etapa ${current + 1} de ${steps.length}`;

  function stepStatus(index) {
    if (index < current) return styles["progress-steps-step-done"];
    if (index === current) return styles["progress-steps-step-current"];
    return "";
  }

  return (
    <div className={[styles["progress-steps"], className].filter(Boolean).join(" ")}>
      {label && <span className={styles["progress-steps-label"]}>{label}</span>}

      <div className={styles["progress-steps-area"]}>
        <ol
          role="progressbar"
          aria-label={label}
          aria-valuemin={0}
          aria-valuemax={steps.length}
          aria-valuenow={current}
          aria-valuetext={valueText}
          className={`${styles["progress-steps-list"]} ${isComplete ? styles["progress-steps-list-complete"] : ""}`}
        >
          {steps.map((step, index) => (
            <li key={step} className={`${styles["progress-steps-step"]} ${stepStatus(index)}`} style={{ "--index": index }}>
              {index < steps.length - 1 && (
                <span className={styles["progress-steps-connector"]}>
                  <span className={styles["progress-steps-connector-fill"]} />
                </span>
              )}
              <span className={styles["progress-steps-marker"]}>
                {index < current ? <Check size={14} strokeWidth={3} aria-hidden /> : index + 1}
              </span>
              <span className={styles["progress-steps-name"]}>{step}</span>
            </li>
          ))}
        </ol>

        {showMascot && <ProgressRobot className={styles["progress-steps-mascot"]} value={current} max={steps.length} />}
      </div>
    </div>
  );
}
