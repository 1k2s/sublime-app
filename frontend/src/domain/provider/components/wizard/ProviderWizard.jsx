import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Stepper } from "@/shared/components/Stepper";
import { Card } from "@/shared/components/Card";
import { showError } from "@/shared/utils/alert";
import { createProviderSchema } from "../../schemas/providerSchema";
import { useCreateProvider, useUpdateProvider } from "../../api/useProviders";
import { ProviderDataStep } from "./ProviderDataStep";
import { ProviderAccessStep } from "./ProviderAccessStep";
import { ProviderConfirmationStep } from "./ProviderConfirmationStep";
import { ProviderPreviewCard } from "./ProviderPreviewCard";
import styles from "./ProviderWizard.module.css";

const STEPS = [
  { number: 1, title: "Dados do prestador", description: "Identificação e comissões" },
  { number: 2, title: "Conta de acesso", description: "Login e senha provisória" },
  { number: 3, title: "Confirmação", description: "Revise e salve" },
];

export function ProviderWizard({ initialData = null, onCancel, onSuccess }) {
  const [currentStep, setCurrentStep] = useState(1);
  const isEditing = Boolean(initialData?.id);

  const createProviderMutation = useCreateProvider();
  const updateProviderMutation = useUpdateProvider();
  const isSubmitting =
    createProviderMutation.isPending || updateProviderMutation.isPending;

  const {
    register,
    control,
    handleSubmit,
    trigger,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createProviderSchema(isEditing)),
    mode: "onBlur",
    defaultValues: {
      name: initialData?.name || "",
      commissionPercentage: initialData?.commissionPercentage || "40",
      referralCommissionPercentage:
        initialData?.referralCommissionPercentage || "45",
      email: initialData?.email || "",
      role: initialData?.role || "PROVIDER",
      password: "",
      status: initialData?.status || "ACTIVE",
    },
  });

  const formValues = useWatch({ control });

  const handleNextFromStep1 = async () => {
    const isValid = await trigger([
      "name",
      "commissionPercentage",
      "referralCommissionPercentage",
    ]);
    if (isValid) {
      setCurrentStep(2);
    }
  };

  const handleNextFromStep2 = async () => {
    const isValid = await trigger(["email", "role", "password", "status"]);
    if (isValid) {
      setCurrentStep(3);
    }
  };

  const handleSave = handleSubmit(async (data) => {
    try {
      if (isEditing) {
        await updateProviderMutation.mutateAsync({
          id: initialData.id,
          currentProvider: initialData,
          data,
        });
      } else {
        await createProviderMutation.mutateAsync(data);
      }
      onSuccess?.();
    } catch (error) {
      showError(error.message);
    }
  });

  return (
    <div className={styles.wizardWrapper}>
      <Stepper
        steps={STEPS}
        currentStep={currentStep}
        className={styles.stepperSpacing}
      />

      <div className={styles.twoColumnGrid}>
        <Card className={styles.mainFormCard}>
          {currentStep === 1 && (
            <ProviderDataStep
              register={register}
              errors={errors}
              onCancel={onCancel}
              onNext={handleNextFromStep1}
            />
          )}

          {currentStep === 2 && (
            <ProviderAccessStep
              register={register}
              errors={errors}
              isEditing={isEditing}
              onBack={() => setCurrentStep(1)}
              onNext={handleNextFromStep2}
            />
          )}

          {currentStep === 3 && (
            <ProviderConfirmationStep
              formData={formValues}
              isEditing={isEditing}
              isSubmitting={isSubmitting}
              onBack={() => setCurrentStep(2)}
              onSubmit={handleSave}
            />
          )}
        </Card>

        <div className={styles.previewColumn}>
          <ProviderPreviewCard formData={formValues} />
        </div>
      </div>
    </div>
  );
}
