/**
 * ======================================================================================
 * COMPONENTE REUTILIZÁVEL: Badge
 * ======================================================================================
 *
 * Etiqueta (retângulo com cantos arredondados) para indicação visual de status:
 * - Variantes disponíveis via CSS Modules:
 *   - "active": o verde suave do app (ex.: status "Ativo", contrato "Vigente").
 *   - "inactive": vermelho suave (ex.: status "Inativo").
 *   - "warning": amarelo suave com texto âmbar (ex.: contrato "Vencido").
 *   - "suggestion": azul suave, para o que é novo ou mudou (ex.: item "Novo" no aditivo).
 *   - "default": o mesmo verde suave, para rótulos neutros (ex.: "Aditivo 2").
 *   - "neutral": cinza, sem destaque (ex.: parcela "A vencer", atendimento "Faltou").
 *   - "outline": só a borda verde, para o próximo da fila (ex.: parcela "Próxima").
 */

import styles from "./Badge.module.css";

/**
 * @param {Object} props
 * @param {"default"|"active"|"inactive"|"warning"|"suggestion"|"neutral"|"outline"} [props.variant="default"] - Variante visual do badge
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
