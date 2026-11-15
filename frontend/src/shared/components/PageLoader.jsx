import styles from "./PageLoader.module.css";

export function PageLoader() {
  return (
    <div className={styles.loader}>
      <p>Carregando...</p>
    </div>
  );
}
