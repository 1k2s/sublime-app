/**
 * ======================================================================================
 * COMPONENTE: PatientWizard
 * ======================================================================================
 *
 * Orquestrador do fluxo de formulário em 3 etapas com painel lateral de prévia:
 * - Etapas:
 *   1. Dados Pessoais (`PersonalDataStep`)
 *   2. Endereço (`AddressStep`)
 *   3. Confirmação (`ConfirmationStep`)
 * - Suporta tanto o modo de **Criação** (quando `initialData` for nulo) quanto o
 *   modo de **Edição** (quando `initialData` contiver os dados do paciente existente).
 * - Utiliza `react-hook-form` integrado ao schema Zod (`fullPatientSchema`) para gerenciar
 *   o estado de todos os campos unificados.
 * - Valida a Etapa 1 antes de permitir o avanço para a Etapa 2 via `trigger()`.
 * - Valida a Etapa 2 antes de permitir o avanço para a Etapa 3 via `trigger()`.
 * - Ao submeter na Etapa 3:
 *   - Se estiver editando: dispara `useUpdatePatient()`.
 *   - Se estiver criando: dispara `useCreatePatient()`.
 */

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Stepper } from "@/shared/ui/Stepper";
import { Card } from "@/shared/ui/Card";
import { fullPatientSchema } from "../../schemas/patientSchema";
import { useCreatePatient, useUpdatePatient } from "../../api/usePatients";
import { PersonalDataStep } from "./PersonalDataStep";
import { AddressStep } from "./AddressStep";
import { ConfirmationStep } from "./ConfirmationStep";
import { PatientPreviewCard } from "./PatientPreviewCard";
import styles from "./PatientWizard.module.css";

/**
 * Definição estática das 3 etapas exibidas no Stepper superior
 */
const STEPS = [
  { number: 1, title: "Dados pessoais", description: "Identificação do paciente" },
  { number: 2, title: "Endereço", description: "Onde o paciente mora" },
  { number: 3, title: "Confirmação", description: "Revise e salve" },
];

/**
 * @param {Object} props
 * @param {Object|null} [props.initialData=null] - Dados iniciais em caso de edição (ou null para novo cadastro)
 * @param {function(): void} props.onCancel - Callback ao cancelar ou voltar à listagem
 * @param {function(): void} props.onSuccess - Callback após criação ou edição bem-sucedida
 */
export function PatientWizard({ initialData = null, onCancel, onSuccess }) {
  // Controle da etapa ativa (1, 2 ou 3)
  const [currentStep, setCurrentStep] = useState(1);

  // Determina se estamos no modo de edição verificando a presença de um ID existente
  const isEditing = Boolean(initialData?.id);

  // Hooks de mutação do TanStack Query
  const createPatientMutation = useCreatePatient();
  const updatePatientMutation = useUpdatePatient();
  const isSubmitting = createPatientMutation.isPending || updatePatientMutation.isPending;

  // Inicialização do formulário com React Hook Form e validação Zod
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    trigger,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(fullPatientSchema),
    mode: "onBlur",
    defaultValues: {
      name: initialData?.name || "",
      cpf: initialData?.cpf || "",
      birthDate: initialData?.birthDate || "",
      phone: initialData?.phone || "",
      email: initialData?.email || "",
      cep: initialData?.cep || "",
      street: initialData?.street || "",
      number: initialData?.number || "",
      complement: initialData?.complement || "",
      neighborhood: initialData?.neighborhood || "",
      city: initialData?.city || "",
      state: initialData?.state || "",
      status: initialData?.status || "ACTIVE",
    },
  });

  // Observa todos os campos em tempo real para alimentar a prévia lateral (PatientPreviewCard)
  const formValues = watch();

  /**
   * Valida apenas os campos da Etapa 1 antes de avançar para a Etapa 2
   */
  const handleNextFromStep1 = async () => {
    const isValid = await trigger(["name", "cpf", "birthDate", "phone", "email"]);
    if (isValid) {
      setCurrentStep(2);
    }
  };

  /**
   * Valida apenas os campos da Etapa 2 antes de avançar para a Etapa 3
   */
  const handleNextFromStep2 = async () => {
    const isValid = await trigger([
      "cep",
      "street",
      "number",
      "complement",
      "neighborhood",
      "city",
      "state",
    ]);
    if (isValid) {
      setCurrentStep(3);
    }
  };

  /**
   * Gravação final: executa criação ou atualização dependendo do modo
   */
  const handleSave = handleSubmit(async (data) => {
    try {
      if (isEditing) {
        await updatePatientMutation.mutateAsync({ id: initialData.id, data });
      } else {
        await createPatientMutation.mutateAsync(data);
      }
      onSuccess?.();
    } catch (err) {
      console.error(isEditing ? "Erro ao atualizar paciente:" : "Erro ao cadastrar paciente:", err);
    }
  });

  return (
    <div className={styles.wizardWrapper}>
      {/* Indicador de progresso no topo */}
      <Stepper steps={STEPS} currentStep={currentStep} className={styles.stepperSpacing} />

      {/* Grid de 2 colunas: formulário da etapa à esquerda e prévia fixa à direita */}
      <div className={styles.twoColumnGrid}>
        <Card className={styles.mainFormCard}>
          {/* Etapa 1: Dados pessoais */}
          {currentStep === 1 && (
            <PersonalDataStep
              register={register}
              errors={errors}
              setValue={setValue}
              watch={watch}
              onCancel={onCancel}
              onNext={handleNextFromStep1}
            />
          )}

          {/* Etapa 2: Endereço residencial */}
          {currentStep === 2 && (
            <AddressStep
              register={register}
              errors={errors}
              setValue={setValue}
              watch={watch}
              onBack={() => setCurrentStep(1)}
              onNext={handleNextFromStep2}
            />
          )}

          {/* Etapa 3: Revisão e confirmação final */}
          {currentStep === 3 && (
            <ConfirmationStep
              formData={formValues}
              isEditing={isEditing}
              isSubmitting={isSubmitting}
              onBack={() => setCurrentStep(2)}
              onSubmit={handleSave}
            />
          )}
        </Card>

        {/* Coluna da direita: prévia reativa do cadastro */}
        <div className={styles.previewColumn}>
          <PatientPreviewCard formData={formValues} />
        </div>
      </div>
    </div>
  );
}
