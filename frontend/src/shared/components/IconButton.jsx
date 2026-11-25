// Botão só com ícone (ex: o editar das tabelas). label é o nome lido pelo leitor de tela;
// title, a dica do mouse. variant="soft": no hover, o verde suave (o das etiquetas) e o ícone um
// pouco maior (ex: a tabela clara de Contratos)
import styles from "./IconButton.module.css";

export function IconButton({ label, title, variant = "default", className, children, ...props }) {
  return (
    <button
      type="button"
      className={[styles.button, variant === "soft" && styles.soft, className].filter(Boolean).join(" ")}
      aria-label={label}
      title={title ?? label}
      {...props}
    >
      {children}
    </button>
  );
}
