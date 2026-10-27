/**
 * ======================================================================================
 * COMPONENTE REUTILIZÁVEL: Card
 * ======================================================================================
 *
 * Container de superfície com fundo branco (`--color-surface`), borda sutil (`--color-border`)
 * e cantos arredondados (`--radius`), utilizado como invólucro para tabelas, formulários e prévias.
 */

import styles from "./Card.module.css";

/**
 * @param {Object} props
 * @param {string} [props.className] - Classes CSS adicionais
 * @param {import("react").ReactNode} props.children - Conteúdo interno do card
 */
export function Card({ className, children, ...props }) {
  const classes = [styles.card, className].filter(Boolean).join(" ");

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
}
