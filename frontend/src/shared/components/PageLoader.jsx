// Carregamento com o nome da marca: "Sublime" se desenha pelo contorno, se preenche e some, em
// loop. Sem movimento (prefers-reduced-motion), fica parado e preenchido.
// variant: "page" (tela inteira, na entrada no app), "section" (no lugar de uma lista ou tela) ou
// "compact" (só o nome, pequeno). label: o que o leitor de tela lê (ex: "Carregando pacientes...")
import styles from "./PageLoader.module.css";

export function PageLoader({ variant = "page", label = "Carregando", className }) {
  const hasTagline = variant !== "compact";

  return (
    <div className={[styles.loader, styles[variant], className].filter(Boolean).join(" ")} role="status" aria-label={label}>
      <svg className={styles.mark} viewBox="0 0 440 140" aria-hidden="true" focusable="false">
        <text className={styles.word} x="220" y="100" textAnchor="middle">
          Sublime
        </text>
      </svg>
      {hasTagline && (
        <span className={styles.tagline} aria-hidden="true">
          Fisioterapia especializada
        </span>
      )}
    </div>
  );
}
