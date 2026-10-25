// Item em edição do carrinho, guiado por Enter: paciente ⏎, técnica ⏎, horário de chegada ⏎ e o item entra no carrinho
import { useRef } from "react";
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { TechniqueSearchInput } from "@/domain/pricing/components/TechniqueSearchInput";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { ProgressBar } from "@/shared/ui/ProgressBar";
import { maskCpf } from "@/shared/utils/masks";
import styles from "./ConsultationForm.module.css";

export function ConsultationForm({
  className,
  patient,
  technique,
  arrivalTime,
  errors,
  onSelectPatient,
  onSelectTechnique,
  onArrivalTimeChange,
  onAdd,
  isAdding = false,
  addError,
}) {
  const techniqueRef = useRef(null);
  const arrivalRef = useRef(null);

  // Os campos são preenchidos em ordem: conta até o primeiro que falta. O horário já vem com a hora
  // atual, então escolhida a técnica o item fica pronto para entrar no carrinho
  const steps = [
    { label: "Paciente", done: Boolean(patient) },
    { label: "Técnica", done: Boolean(technique) },
    { label: "Horário de chegada", done: Boolean(arrivalTime) },
  ];
  const firstPending = steps.findIndex((step) => !step.done);
  const completed = firstPending === -1 ? steps.length : firstPending;
  const isReady = completed === steps.length;

  function handleSelectPatient(selectedPatient) {
    onSelectPatient(selectedPatient);
    techniqueRef.current?.focus();
  }

  function handleSelectTechnique(selectedTechnique) {
    onSelectTechnique(selectedTechnique);
    arrivalRef.current?.focus();
  }

  function handleSubmit(event) {
    event.preventDefault();
    onAdd();
  }

  return (
    <Card title="Adicionar lançamento" className={className}>
      <form className={styles["form-body"]} onSubmit={handleSubmit} noValidate>
        <FormField label="Paciente" htmlFor="consultation-patient" error={errors.patientId} reserveErrorSpace>
          {patient ? (
            <div className={styles["form-selected"]}>
              <div className={styles["form-selected-info"]}>
                <span className={styles["form-selected-name"]}>{patient.name}</span>
                <span className={styles["form-selected-cpf"]}>{maskCpf(patient.cpf)}</span>
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

        <FormField label="Técnica" htmlFor="consultation-technique" error={errors.techniqueId} reserveErrorSpace>
          {technique ? (
            <div className={styles["form-selected"]}>
              <div className={styles["form-selected-info"]}>
                <span className={styles["form-selected-name"]}>{technique.name}</span>
              </div>
              <Button variant="secondary" onClick={() => onSelectTechnique(null)}>
                Trocar
              </Button>
            </div>
          ) : (
            <TechniqueSearchInput
              ref={techniqueRef}
              id="consultation-technique"
              autoFocus={Boolean(patient)}
              onSelect={handleSelectTechnique}
              invalid={Boolean(errors.techniqueId)}
            />
          )}
        </FormField>

        <FormField
          label="Horário de chegada"
          htmlFor="consultation-arrival"
          error={errors.arrivalTime}
          reserveErrorSpace
        >
          <Input
            ref={arrivalRef}
            id="consultation-arrival"
            type="datetime-local"
            invalid={Boolean(errors.arrivalTime)}
            value={arrivalTime}
            onChange={(event) => onArrivalTimeChange(event.target.value)}
          />
        </FormField>

        <div className={styles["form-progress"]}>
          <ProgressBar
            value={completed}
            max={steps.length}
            label="Progresso do lançamento"
            valueText={`${completed} de ${steps.length} campos`}
          />
          <p className={isReady ? `${styles["form-progress-caption"]} ${styles["form-progress-ready"]}` : styles["form-progress-caption"]} aria-live="polite">
            {isReady
              ? "Tudo pronto: Enter adiciona ao carrinho"
              : `Próximo: ${steps[completed].label} · Enter avança`}
          </p>
        </div>

        {/* Espaço do alerta fica reservado mesmo vazio, para o botão não sair do lugar */}
        <div className={styles["form-alert-slot"]}>
          {addError && (
            <p className={styles["form-alert"]} role="alert">
              {addError.message}
            </p>
          )}
        </div>

        <Button type="submit" loading={isAdding}>
          Adicionar ao carrinho
        </Button>
      </form>
    </Card>
  );
}
