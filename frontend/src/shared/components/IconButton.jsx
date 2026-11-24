// Botão só com ícone (ex: o editar das tabelas). label é o nome lido pelo leitor de tela;
// title, a dica do mouse
import styles from "./IconButton.module.css";

export function IconButton({ label, title, className, children, ...props }) {
  return (
    <button
      type="button"
      className={[styles.button, className].filter(Boolean).join(" ")}
      aria-label={label}
      title={title ?? label}
      {...props}
    >
      {children}
    </button>
  );
}
