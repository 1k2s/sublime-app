// Caixa branca com borda. O título é opcional
import styles from "./Card.module.css";

export function Card({ title, className, children, ...props }) {
  const classes = [styles.card, className].filter(Boolean).join(" ");

  return (
    <section className={classes} {...props}>
      {title && <h2 className={styles["card-title"]}>{title}</h2>}
      {children}
    </section>
  );
}
