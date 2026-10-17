// Caixa branca com borda. O título é opcional
import styles from "./Card.module.css";

export function Card({ title, className, children }) {
  const classes = [styles.card, className].filter(Boolean).join(" ");

  return (
    <section className={classes}>
      {title && <h2 className={styles.title}>{title}</h2>}
      {children}
    </section>
  );
}
