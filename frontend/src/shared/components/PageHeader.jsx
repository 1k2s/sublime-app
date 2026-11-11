// Cabeçalho de página: título, descrição opcional, botão de voltar (onBack) e ação à direita (action)
import { ArrowLeft } from "lucide-react";
import styles from "./PageHeader.module.css";

export function PageHeader({ title, description, onBack, action, className }) {
  return (
    <header className={[styles["page-header"], className].filter(Boolean).join(" ")}>
      <div className={styles["page-header-title-area"]}>
        {onBack && (
          <button type="button" className={styles["page-header-back"]} onClick={onBack} aria-label="Voltar" title="Voltar">
            <ArrowLeft size={22} />
          </button>
        )}
        <div className={styles["page-header-text"]}>
          <h1 className={styles["page-header-title"]}>{title}</h1>
          {description && <p className={styles["page-header-description"]}>{description}</p>}
        </div>
      </div>

      {action && <div className={styles["page-header-action"]}>{action}</div>}
    </header>
  );
}
