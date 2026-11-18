// Nome com o avatar das iniciais, na primeira coluna das tabelas de pessoas. detail: uma linha
// menor abaixo do nome (ex: o beneficiário do contrato)
import { getInitials } from "@/shared/utils/text";
import styles from "./AvatarName.module.css";

export function AvatarName({ name, detail }) {
  return (
    <div className={styles.person}>
      <span className={styles.avatar} aria-hidden="true">
        {getInitials(name)}
      </span>
      <div className={styles.text}>
        <span className={styles.name}>{name ?? "--"}</span>
        {detail && <span className={styles.detail}>{detail}</span>}
      </div>
    </div>
  );
}
