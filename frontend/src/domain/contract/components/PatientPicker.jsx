// Busca de paciente que, depois de escolhido, vira uma linha no estilo de um campo, com o nome e o
// botão "Remover". Remover limpa a escolha e põe o cursor na busca: digitar escolhe outro; sair do
// campo sem escolher deixa vazio (ex: o contrato fica sem beneficiário).
// excludeIds: pacientes que a busca não oferece (o titular, na busca do beneficiário)
import { useState } from "react";
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { Button } from "@/shared/components/Button";
import { maskCpf } from "@/shared/utils/masks";
import styles from "./PatientPicker.module.css";

export function PatientPicker({ id, value, onChange, invalid = false, excludeIds }) {
  // Só depois do "Remover": a busca abre com o cursor (na abertura do formulário, não)
  const [shouldFocusSearch, setShouldFocusSearch] = useState(false);

  function remove() {
    setShouldFocusSearch(true);
    onChange(null);
  }

  if (!value) {
    return (
      <PatientSearchInput
        id={id}
        onSelect={onChange}
        invalid={invalid}
        excludeIds={excludeIds}
        autoFocus={shouldFocusSearch}
      />
    );
  }

  return (
    <div className={styles.picker}>
      <div className={styles["picker-info"]}>
        <span className={styles["picker-name"]}>{value.name}</span>
        {value.cpf && <span className={styles["picker-cpf"]}>{maskCpf(value.cpf)}</span>}
      </div>
      <Button variant="secondary" className={styles["picker-button"]} onClick={remove}>
        Remover
      </Button>
    </div>
  );
}
