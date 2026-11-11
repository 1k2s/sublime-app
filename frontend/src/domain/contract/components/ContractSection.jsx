// Card de uma seção do formulário: ícone, título e descrição no topo, conteúdo ocupando o resto da altura
import { Card } from "@/shared/components/Card";
import styles from "./ContractSection.module.css";

export function ContractSection({ icon: Icon, title, description, className, children }) {
  return (
    <Card className={[styles.section, className].filter(Boolean).join(" ")}>
      <header className={styles["section-header"]}>
        <span className={styles["section-icon"]}>
          <Icon size={20} aria-hidden />
        </span>
        <div className={styles["section-text"]}>
          <h2 className={styles["section-title"]}>{title}</h2>
          <p className={styles["section-description"]}>{description}</p>
        </div>
      </header>
      <div className={styles["section-body"]}>{children}</div>
    </Card>
  );
}
