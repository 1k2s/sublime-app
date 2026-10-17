// Item em edição do carrinho, guiado por Enter: busca o paciente ⏎, horário de chegada ⏎ e o item entra no carrinho
import { useRef } from "react";
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { maskCpf } from "@/shared/utils/masks";
import styles from "./ConsultationForm.module.css";

export function ConsultationForm({
  className,
  patient,
  arrivalTime,
  errors,
  onSelectPatient,
  onArrivalTimeChange,
  onAdd,
  isAdding = false,
  addError,
}) {
  const arrivalRef = useRef(null);

  function handleSelectPatient(selectedPatient) {
    onSelectPatient(selectedPatient);
    arrivalRef.current?.focus();
  }

  function handleSubmit(event) {
    event.preventDefault();
    onAdd();
  }

  return (
    <Card title="Adicionar lançamento" className={className}>
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <FormField label="Paciente" htmlFor="consultation-patient" error={errors.patientId}>
          {patient ? (
            <div className={styles.selected}>
              <div className={styles.patient}>
                <span className={styles.name}>{patient.name}</span>
                <span className={styles.cpf}>{maskCpf(patient.cpf)}</span>
              </div>
              <Button variant="secondary" onClick={() => onSelectPatient(null)}>
                Trocar
              </Button>
            </div>
          ) : (
            // Monta de novo a cada item adicionado e já recebe o foco para o próximo paciente
            <PatientSearchInput
              id="consultation-patient"
              autoFocus
              onSelect={handleSelectPatient}
              invalid={Boolean(errors.patientId)}
            />
          )}
        </FormField>

        <FormField label="Horário de chegada" htmlFor="consultation-arrival" error={errors.arrivalTime}>
          <Input
            ref={arrivalRef}
            id="consultation-arrival"
            type="datetime-local"
            invalid={Boolean(errors.arrivalTime)}
            value={arrivalTime}
            onChange={(event) => onArrivalTimeChange(event.target.value)}
          />
        </FormField>

        <p className={styles.hint}>
          <kbd>Enter</kbd> escolhe o paciente e avança para o próximo campo.
        </p>

        {addError && (
          <p className={styles.error} role="alert">
            {addError.message}
          </p>
        )}

        <Button type="submit" className={styles.submit} loading={isAdding}>
          Adicionar ao carrinho
        </Button>
      </form>
    </Card>
  );
}
