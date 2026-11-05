// Janela sobre o conteúdo. Fecha com Esc ou clicando no fundo
import { useEffect } from "react";
import styles from "./Modal.module.css";

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div className={styles.backdrop} onMouseDown={onClose}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 className={styles.title}>{title}</h2>
        {children}
      </div>
    </div>
  );
}
