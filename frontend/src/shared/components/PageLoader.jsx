// Carregamento da entrada no app: o nome "Sublime" se desenha pelo contorno, se preenche e some,
// em loop, com "Fisioterapia especializada" embaixo. Sem movimento (prefers-reduced-motion), fica
// parado e preenchido. O leitor de tela lê só "Carregando"
import styles from "./PageLoader.module.css";

export function PageLoader() {
  return (
    <div className={styles.loader} role="status" aria-label="Carregando">
      <svg className={styles.mark} viewBox="0 0 440 140" aria-hidden="true" focusable="false">
        <text className={styles.word} x="220" y="100" textAnchor="middle">
          Sublime
        </text>
      </svg>
      <span className={styles.tagline} aria-hidden="true">
        Fisioterapia especializada
      </span>
    </div>
  );
}
