// Busca de paciente que, depois de escolhido, vira uma linha com o nome e o botão de trocar.
// excludeIds: pacientes que a busca não oferece (o titular, na busca do beneficiário)
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { Button } from "@/shared/components/Button";
import { maskCpf } from "@/shared/utils/masks";
import styles from "./PatientPicker.module.css";

export function PatientPicker({ id, value, onChange, invalid = false, excludeIds }) {
  if (!value) return <PatientSearchInput id={id} onSelect={onChange} invalid={invalid} excludeIds={excludeIds} />;

  return (
    <div className={styles.picker}>
      <div className={styles["picker-info"]}>
        <span className={styles["picker-name"]}>{value.name}</span>
        {value.cpf && <span className={styles["picker-cpf"]}>{maskCpf(value.cpf)}</span>}
      </div>
      <Button variant="secondary" className={styles["picker-button"]} onClick={() => onChange(null)}>
        Trocar
      </Button>
    </div>
  );
}
