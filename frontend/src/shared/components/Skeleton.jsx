// Bloco cinza com brilho, no lugar de um conteúdo que ainda está carregando. O tamanho vem do
// className do pai (largura e altura); a forma, do variant: "text" (linha), "pill" ou "circle"
import styles from "./Skeleton.module.css";

export function Skeleton({ variant = "text", className }) {
  return <span className={[styles.skeleton, styles[variant], className].filter(Boolean).join(" ")} aria-hidden="true" />;
}
