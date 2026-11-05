/**
 * ======================================================================================
 * COMPONENTE REUTILIZÁVEL: Stepper
 * ======================================================================================
 *
 * Barra superior de navegação por etapas numeradas (conforme Imagem 2):
 * - Círculos com numeração:
 *   - Etapa atual: preenchido em verde primário (`--color-primary`) com texto branco.
 *   - Etapas concluídas: também destacadas.
 *   - Próximas etapas: fundo cinza suave com texto neutro.
 * - Título da etapa em negrito e subtítulo descritivo logo abaixo.
 * - Linhas conectoras horizontais divisórias entre cada etapa.
 */

import styles from "./Stepper.module.css";

/**
 * @param {Object} props
 * @param {Array<{number: number, title: string, description?: string}>} props.steps - Lista de passos
 * @param {number} props.currentStep - Número da etapa ativa no momento
 * @param {string} [props.className] - Classes CSS adicionais
 */
export function Stepper({ steps, currentStep, className }) {
  return (
    <div className={[styles.stepper, className].filter(Boolean).join(" ")}>
      {steps.map((step, index) => {
        const isCurrent = currentStep === step.number;
        const isCompleted = currentStep > step.number;
        const isLast = index === steps.length - 1;

        return (
          <div key={step.number} className={styles.stepItemWrapper}>
            {/* Bloco visual do passo: círculo + títulos */}
            <div className={styles.stepItem}>
              <div
                className={[
                  styles.circle,
                  isCurrent && styles.circleActive,
                  isCompleted && styles.circleCompleted,
                ]
                  .filter(Boolean)
                  .join(" ")}
              >
                {step.number}
              </div>
              <div className={styles.stepContent}>
                <span
                  className={[
                    styles.stepTitle,
                    (isCurrent || isCompleted) && styles.stepTitleActive,
                  ]
                    .filter(Boolean)
                    .join(" ")}
                >
                  {step.title}
                </span>
                {step.description && (
                  <span className={styles.stepDescription}>
                    {step.description}
                  </span>
                )}
              </div>
            </div>

            {/* Linha horizontal conectora entre os passos (exceto no último) */}
            {!isLast && <div className={styles.connector} />}
          </div>
        );
      })}
    </div>
  );
}
