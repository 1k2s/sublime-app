// Item em edição do carrinho, guiado por Enter: paciente ⏎, técnica ⏎, duração ⏎ e o item entra no carrinho.
// Data e status já vêm preenchidos (hoje, Atendido) e só mudam quando preciso
import { useRef } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PatientSearchInput } from "@/domain/patient/components/PatientSearchInput";
import { useTechniquePrices } from "@/domain/pricing/api/usePricing";
import { TechniqueSearchInput } from "@/domain/pricing/components/TechniqueSearchInput";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Select } from "@/shared/ui/Select";
import { toDateInputValue } from "@/shared/utils/date";
import { maskCpf } from "@/shared/utils/masks";
import { CONSULTATION_STATUSES } from "../consultationStatus";
import { consultationSchema } from "../schemas/consultationSchema";
import { ConsultationAgenda } from "./ConsultationAgenda";
import styles from "./ConsultationForm.module.css";

function emptyDraft(occurredAt = toDateInputValue(new Date()), status = "ATTENDED") {
  return { patient: null, technique: null, occurredAt, durationMinutes: "", status };
}

// Durações da tabela vigente do grupo da técnica, sem repetir (vários planos têm as mesmas)
function durationOptions(priceTable) {
  const durations = priceTable?.prices.map((price) => price.durationMinutes).filter(Boolean) ?? [];
  return [...new Set(durations)].sort((a, b) => a - b);
}

// onAdd(values, clearDraft): a página resolve o contrato e chama clearDraft quando o item entra no carrinho
export function ConsultationForm({ className, onAdd, isAdding = false, isLocked = false }) {
  const {
    control,
    register,
    setValue,
    setFocus,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(consultationSchema), defaultValues: emptyDraft() });

  const techniqueRef = useRef(null);
  const values = useWatch({ control });
  const priceTable = useTechniquePrices(values.technique?.id ?? null);
  // Grupo por frequência (Pilates em Grupo): a duração só é registrada, então o campo é livre
  const isFrequencyBased = priceTable.data?.pricingModel === "FREQUENCY_BASED";
  const durations = durationOptions(priceTable.data);

  function selectPatient(patient) {
    setValue("patient", patient, { shouldValidate: Boolean(patient) });
    if (patient) techniqueRef.current?.focus();
  }

  // As opções de duração dependem da técnica: trocar a técnica limpa a duração escolhida
  function selectTechnique(technique) {
    setValue("technique", technique, { shouldValidate: Boolean(technique) });
    setValue("durationMinutes", "");
    if (technique) setFocus("durationMinutes");
  }

  // Limpa paciente, técnica e duração para o próximo; data e status ficam, para agilizar a sequência
  const submit = handleSubmit((draft) => onAdd(draft, () => reset(emptyDraft(draft.occurredAt, draft.status))));

  // O Enter num <select> não envia o formulário no navegador: envia aqui, para o fluxo seguir só no teclado
  function handleKeyDown(event) {
    if (event.key === "Enter" && event.target.tagName === "SELECT") {
      event.preventDefault();
      event.currentTarget.requestSubmit();
    }
  }

  function durationPlaceholder() {
    if (!values.technique) return "Escolha a técnica";
    if (priceTable.isLoading) return "Carregando...";
    if (durations.length === 0) return "Sem preço vigente";
    return "Selecione";
  }

  return (
    <Card title="Adicionar lançamento" className={[styles["form-card"], className].filter(Boolean).join(" ")}>
      <form className={styles["form-body"]} onSubmit={submit} onKeyDown={handleKeyDown} noValidate>
        <FormField label="Paciente" htmlFor="consultation-patient" error={errors.patient?.message} reserveErrorSpace>
          <Controller
            name="patient"
            control={control}
            render={({ field }) =>
              field.value ? (
                <div className={styles["form-selected"]}>
                  <div className={styles["form-selected-info"]}>
                    <span className={styles["form-selected-name"]}>{field.value.name}</span>
                    <span className={styles["form-selected-cpf"]}>{maskCpf(field.value.cpf)}</span>
                  </div>
                  <Button variant="secondary" className={styles["form-selected-button"]} onClick={() => selectPatient(null)}>
                    Trocar
                  </Button>
                </div>
              ) : (
                // Monta de novo a cada item adicionado e já recebe o foco para o próximo paciente
                <PatientSearchInput
                  id="consultation-patient"
                  autoFocus
                  onSelect={selectPatient}
                  invalid={Boolean(errors.patient)}
                />
              )
            }
          />
        </FormField>

        <FormField label="Técnica" htmlFor="consultation-technique" error={errors.technique?.message} reserveErrorSpace>
          <Controller
            name="technique"
            control={control}
            render={({ field }) =>
              field.value ? (
                <div className={styles["form-selected"]}>
                  <div className={styles["form-selected-info"]}>
                    <span className={styles["form-selected-name"]}>{field.value.name}</span>
                  </div>
                  <Button variant="secondary" className={styles["form-selected-button"]} onClick={() => selectTechnique(null)}>
                    Trocar
                  </Button>
                </div>
              ) : (
                <TechniqueSearchInput
                  ref={techniqueRef}
                  id="consultation-technique"
                  autoFocus={Boolean(values.patient)}
                  onSelect={selectTechnique}
                  invalid={Boolean(errors.technique)}
                />
              )
            }
          />
        </FormField>

        <div className={styles["form-row"]}>
          <FormField label="Data" htmlFor="consultation-date" error={errors.occurredAt?.message} reserveErrorSpace>
            <Input id="consultation-date" type="date" invalid={Boolean(errors.occurredAt)} {...register("occurredAt")} />
          </FormField>

          <FormField label="Duração" htmlFor="consultation-duration" error={errors.durationMinutes?.message} reserveErrorSpace>
            {isFrequencyBased ? (
              <Input
                id="consultation-duration"
                type="number"
                min={1}
                placeholder="Minutos"
                invalid={Boolean(errors.durationMinutes)}
                {...register("durationMinutes")}
              />
            ) : (
              // Sem disabled: o foco chega aqui logo depois da técnica, antes das opções carregarem
              <Select id="consultation-duration" invalid={Boolean(errors.durationMinutes)} {...register("durationMinutes")}>
                <option value="">{durationPlaceholder()}</option>
                {durations.map((minutes) => (
                  <option key={minutes} value={minutes}>
                    {minutes} min
                  </option>
                ))}
              </Select>
            )}
          </FormField>
        </div>

        <FormField label="Status" htmlFor="consultation-status" error={errors.status?.message} reserveErrorSpace>
          <Select id="consultation-status" invalid={Boolean(errors.status)} {...register("status")}>
            {CONSULTATION_STATUSES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </FormField>

        <ConsultationAgenda
          className={styles["form-progress"]}
          patient={values.patient}
          technique={values.technique}
          durationMinutes={values.durationMinutes}
          occurredAt={values.occurredAt}
        />

        <Button type="submit" loading={isAdding} disabled={isLocked}>
          Adicionar ao carrinho
        </Button>
      </form>
    </Card>
  );
}
