// Orquestrador do formulário de paciente em 3 etapas com prévia lateral
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AddressStep } from "@/domain/patient/components/wizard/AddressStep";
import { ConfirmationStep } from "@/domain/patient/components/wizard/ConfirmationStep";
import { PatientPreviewCard } from "@/domain/patient/components/wizard/PatientPreviewCard";
import { PersonalDataStep } from "@/domain/patient/components/wizard/PersonalDataStep";
import { fullPatientSchema, toPatientPayload } from "@/domain/patient/schemas/patientSchema";
import { useProviders } from "@/domain/provider/api/useProviders";
import { Card } from "@/shared/components/Card";
import { Stepper } from "@/shared/components/Stepper";
import { formatDate } from "@/shared/utils/date";
import { maskCep, maskCpf, maskPhone } from "@/shared/utils/masks";
import styles from "@/domain/patient/components/wizard/PatientWizard.module.css";

const STEPS = [
  { number: 1, title: "Dados pessoais", description: "Identificação do paciente" },
  { number: 2, title: "Endereço", description: "Onde o paciente mora" },
  { number: 3, title: "Confirmação", description: "Revise e salve" },
];

const STEP_1_FIELDS = new Set(["name", "cpf", "birthDate", "phone", "email", "referringProviderId"]);
const STEP_2_FIELDS = new Set(["cep", "street", "numberHouse", "complement", "city"]);

// Mapeia nomes de campos do ProblemDetail (ex: "address.cep") para os campos do formulário
function normalizeApiFieldName(apiField) {
  if (apiField.startsWith("address.")) {
    return apiField.slice("address.".length);
  }
  return apiField;
}

export function PatientWizard({
  initialData = null,
  isSubmitting = false,
  onCancel,
  onSubmit,
}) {
  const [currentStep, setCurrentStep] = useState(1);
  const isEditing = Boolean(initialData?.id);
  const active = isEditing ? Boolean(initialData.active) : true;
  const hasExistingAddress = Boolean(initialData?.address);

  const { data: allProviders = [] } = useProviders();

  // Permite atribuir prestador ativo ou manter o prestador atual mesmo que tenha sido inativado
  const availableProviders = allProviders.filter(
    (provider) => provider.active || provider.id === initialData?.referringProviderId
  );

  const {
    register,
    control,
    handleSubmit,
    setValue,
    setError,
    trigger,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(fullPatientSchema),
    mode: "onBlur",
    defaultValues: {
      name: initialData?.name ?? "",
      cpf: initialData?.cpf ? maskCpf(initialData.cpf) : "",
      birthDate: initialData?.birthDate ? formatDate(initialData.birthDate) : "",
      phone: initialData?.phone ? maskPhone(initialData.phone) : "",
      email: initialData?.email ?? "",
      referringProviderId: initialData?.referringProviderId
        ? String(initialData.referringProviderId)
        : "",
      cep: initialData?.address?.cep ? maskCep(initialData.address.cep) : "",
      street: initialData?.address?.street ?? "",
      numberHouse: initialData?.address?.numberHouse ?? "",
      complement: initialData?.address?.complement ?? "",
      city: initialData?.address?.city ?? "",
      addressRequired: hasExistingAddress,
    },
  });

  const formValues = useWatch({ control });

  const selectedProvider = availableProviders.find(
    (p) => String(p.id) === String(formValues.referringProviderId)
  );
  const referringProviderName =
    selectedProvider?.name ??
    (String(initialData?.referringProviderId ?? "") === String(formValues.referringProviderId ?? "")
      ? initialData?.referringProviderName
      : null);

  async function handleNextFromStep1() {
    const isValid = await trigger([
      "name",
      "cpf",
      "birthDate",
      "phone",
      "email",
      "referringProviderId",
    ]);
    if (isValid) {
      setCurrentStep(2);
    }
  }

  async function handleNextFromStep2() {
    const isValid = await trigger(["cep", "street", "numberHouse", "complement", "city"]);
    if (isValid) {
      setCurrentStep(3);
    }
  }

  function applyApiErrors(apiErrors) {
    if (!apiErrors || typeof apiErrors !== "object") return;

    let targetStep = null;
    for (const [rawField, message] of Object.entries(apiErrors)) {
      const field = normalizeApiFieldName(rawField);
      setError(field, { type: "server", message });
      if (STEP_1_FIELDS.has(field)) {
        targetStep = 1;
      } else if (STEP_2_FIELDS.has(field) && targetStep !== 1) {
        targetStep = 2;
      }
    }

    if (targetStep) {
      setCurrentStep(targetStep);
    }
  }

  const handleSave = handleSubmit((values) => {
    const payload = toPatientPayload(values);
    onSubmit?.(payload, { applyApiErrors });
  });

  return (
    <div className={styles.wizardWrapper}>
      <Stepper steps={STEPS} currentStep={currentStep} className={styles.stepperSpacing} />

      <div className={styles.twoColumnGrid}>
        <Card className={styles.mainFormCard}>
          {currentStep === 1 && (
            <PersonalDataStep
              register={register}
              control={control}
              errors={errors}
              setValue={setValue}
              providers={availableProviders}
              onCancel={onCancel}
              onNext={handleNextFromStep1}
            />
          )}

          {currentStep === 2 && (
            <AddressStep
              register={register}
              control={control}
              errors={errors}
              setValue={setValue}
              onBack={() => setCurrentStep(1)}
              onNext={handleNextFromStep2}
            />
          )}

          {currentStep === 3 && (
            <ConfirmationStep
              formData={formValues}
              active={active}
              referringProviderName={referringProviderName}
              isEditing={isEditing}
              isSubmitting={isSubmitting}
              onBack={() => setCurrentStep(2)}
              onSubmit={handleSave}
            />
          )}
        </Card>

        <div className={styles.previewColumn}>
          <PatientPreviewCard
            formData={formValues}
            active={active}
            referringProviderName={referringProviderName}
          />
        </div>
      </div>
    </div>
  );
}
