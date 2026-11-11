/**
 * ======================================================================================
 * COMPONENTE REUTILIZÁVEL: Badge
 * ======================================================================================
 *
 * Tag em formato pill para indicação visual de status:
 * - Variantes disponíveis via CSS Modules:
 *   - "active": fundo verde suave com texto verde escuro (ex.: status "Ativo").
 *   - "inactive": fundo cinza suave com texto neutro (ex.: status "Inativo").
 *   - "default": estilo padrão baseado na cor primária do tema.
 */

import styles from "./Badge.module.css";

/**
 * @param {Object} props
 * @param {"default"|"active"|"inactive"} [props.variant="default"] - Variante visual do badge
 * @param {string} [props.className] - Classes CSS adicionais
 * @param {import("react").ReactNode} props.children - Texto ou conteúdo da tag
 */
export function Badge({ variant = "default", className, children, ...props }) {
  const classes = [styles.badge, styles[variant], className].filter(Boolean).join(" ");

  return (
    <span className={classes} {...props}>
      {children}
    </span>
  );
}
