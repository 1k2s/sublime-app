import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Stepper } from "@/shared/ui/Stepper";
import { Card } from "@/shared/ui/Card";
import { fullPatientSchema } from "../../schemas/patientSchema";
import { ADDRESS_FIELDS, toPatientFormValues } from "../../patientForm";
import { PersonalDataStep } from "./PersonalDataStep";
import { AddressStep } from "./AddressStep";
import { ConfirmationStep } from "./ConfirmationStep";
import { PatientPreviewCard } from "./PatientPreviewCard";
import styles from "./PatientWizard.module.css";

const STEPS = [
  { number: 1, title: "Dados pessoais", description: "Identificação do paciente" },
  { number: 2, title: "Endereço", description: "Onde o paciente mora" },
  { number: 3, title: "Confirmação", description: "Revise e salve" },
];

export function PatientWizard({ initialData = null, isSubmitting = false, onSubmit, onCancel }) {
  const [currentStep, setCurrentStep] = useState(1);

  const isEditing = Boolean(initialData?.id);

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
    defaultValues: toPatientFormValues(initialData),
  });

  const formValues = watch();

  const handleNextFromStep1 = async () => {
    const isValid = await trigger(["name", "cpf", "birthDate", "phone", "email"]);
    if (isValid) {
      setCurrentStep(2);
    }
  };

  const handleNextFromStep2 = async () => {
    const isValid = await trigger(ADDRESS_FIELDS);
    if (isValid) {
      setCurrentStep(3);
    }
  };

  const handleSave = handleSubmit((values) => onSubmit?.(values));

  return (
    <div className={styles.wizardWrapper}>
      <Stepper steps={STEPS} currentStep={currentStep} className={styles.stepperSpacing} />

      <div className={styles.twoColumnGrid}>
        <Card className={styles.mainFormCard}>
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

        <div className={styles.previewColumn}>
          <PatientPreviewCard formData={formValues} />
        </div>
      </div>
    </div>
  );
}
