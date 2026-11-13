// Janela sobre o conteúdo. Fecha com Esc, clicando no fundo ou no X.
// Opcionais: icon (ícone no círculo), titleExtra (ao lado do título, ex: Badge), description
// (subtítulo) e footer (faixa de rodapé, com borda em cima).
// size="large": formulário longo (ex: cadastro de contrato). O conteúdo ocupa a altura toda e
// cuida da própria rolagem e do próprio rodapé fixo.
// closeOnBackdrop={false}: o clique no fundo não fecha (num formulário longo, um clique sem querer
// perderia o que foi preenchido); fecham o X, o Esc e o botão de cancelar do conteúdo
import { useEffect } from "react";
import { X } from "lucide-react";
import styles from "./Modal.module.css";

export function Modal({
  title,
  icon: Icon,
  titleExtra,
  description,
  footer,
  onClose,
  size = "default",
  closeOnBackdrop = true,
  children,
}) {
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div className={styles.backdrop} onMouseDown={closeOnBackdrop ? onClose : undefined}>
      <div
        className={[styles.dialog, size === "large" && styles.large].filter(Boolean).join(" ")}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className={styles.header}>
          {Icon && (
            <span className={styles.icon}>
              <Icon size={20} aria-hidden />
            </span>
          )}
          <div className={styles.heading}>
            <div className={styles.titleRow}>
              <h2 className={styles.title}>{title}</h2>
              {titleExtra}
            </div>
            {description && <p className={styles.description}>{description}</p>}
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Fechar" title="Fechar">
            <X size={18} />
          </button>
        </header>

        <div className={styles.body}>{children}</div>

        {footer && <footer className={styles.footer}>{footer}</footer>}
      </div>
    </div>
  );
}
