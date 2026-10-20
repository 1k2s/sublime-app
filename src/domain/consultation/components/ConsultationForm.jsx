// Item em edição do carrinho, guiado por Enter: paciente ⏎, técnica ⏎, horário de chegada ⏎ e o item entra no carrinho
import { useRef } from "react";
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { useTechniques } from "@/domain/pricing/api/useTechniques";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { ProgressBar } from "@/shared/ui/ProgressBar";
import { Select } from "@/shared/ui/Select";
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
  const { data: techniques = [], isLoading: isLoadingTechniques, isError: techniquesError } = useTechniques();

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

  function handleTechniqueChange(event) {
    const id = Number(event.target.value);
    onSelectTechnique(techniques.find((option) => option.id === id) ?? null);
  }

  // No select, as setas escolhem e o Enter só avança: não envia o formulário
  function handleTechniqueKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      arrivalRef.current?.focus();
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    onAdd();
  }

  function techniquePlaceholder() {
    if (isLoadingTechniques) return "Carregando...";
    if (techniquesError) return "Não foi possível carregar as técnicas";
    return "Selecione a técnica";
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
          <Select
            ref={techniqueRef}
            id="consultation-technique"
            invalid={Boolean(errors.techniqueId)}
            disabled={isLoadingTechniques || techniquesError}
            value={technique?.id ?? ""}
            onChange={handleTechniqueChange}
            onKeyDown={handleTechniqueKeyDown}
          >
            <option value="" disabled>
              {techniquePlaceholder()}
            </option>
            {techniques.map((option) => (
              <option key={option.id} value={option.id}>
                {option.name}
              </option>
            ))}
          </Select>
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
