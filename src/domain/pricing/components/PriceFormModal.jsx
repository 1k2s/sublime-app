// Modal de preço novo ou reajuste (mesmo endpoint). initialValues preenche o formulário ao reajustar uma linha
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Info } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Modal } from "@/shared/ui/Modal";
import { Select } from "@/shared/ui/Select";
import { useSavePrice } from "../api/usePricing";
import { priceSchema } from "../schemas/priceSchema";
import styles from "./PriceFormModal.module.css";

const DURATION_SUGGESTIONS = [30, 50, 60];

// Plano precisa ser compatível com o modelo do grupo (regra do backend, PlanType.isPricedBy)
function isPlanAllowed(plan, pricingModel) {
  if (!plan.active) return false;
  return pricingModel === "DURATION_BASED" ? plan.type !== "MONTHLY_FEE" : plan.type === "MONTHLY_FEE";
}

function getErrorMessage(error) {
  return error.response?.data?.message ?? "Não foi possível salvar o preço.";
}

export function PriceFormModal({ groups, plans, initialValues, onClose }) {
  const savePrice = useSavePrice();
  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(priceSchema),
    defaultValues: {
      pricingGroupId: initialValues?.pricingGroupId ?? "",
      planId: initialValues?.planId ?? "",
      axis: initialValues?.axis ?? "",
      sessionValue: initialValues?.sessionValue ?? "",
    },
  });

  const groupId = useWatch({ control, name: "pricingGroupId" });
  const group = groups.find((item) => String(item.id) === groupId);
  const isDuration = group?.pricingModel === "DURATION_BASED";
  const availablePlans = group ? plans.filter((plan) => isPlanAllowed(plan, group.pricingModel)) : [];

  function onSubmit(values) {
    savePrice.mutate(
      {
        pricingModel: group.pricingModel,
        pricingGroupId: Number(values.pricingGroupId),
        planId: Number(values.planId),
        [isDuration ? "durationMinutes" : "weeklyFrequency"]: Number(values.axis),
        sessionValue: values.sessionValue.replace(",", "."),
      },
      { onSuccess: onClose }
    );
  }

  const groupField = register("pricingGroupId", {
    onChange: () => setValue("planId", ""),
  });

  return (
    <Modal title="Novo preço / Reajustar" onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <FormField label="Grupo de preço" htmlFor="pricingGroupId" error={errors.pricingGroupId?.message}>
          <Select id="pricingGroupId" invalid={!!errors.pricingGroupId} {...groupField}>
            <option value="">Selecione</option>
            {groups.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField label="Plano" htmlFor="planId" error={errors.planId?.message}>
          <Select id="planId" invalid={!!errors.planId} disabled={!group} {...register("planId")}>
            <option value="">Selecione</option>
            {availablePlans.map((plan) => (
              <option key={plan.id} value={plan.id}>
                {plan.name}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField
          label={isDuration ? "Duração (minutos)" : "Frequência semanal"}
          htmlFor="axis"
          error={errors.axis?.message}
        >
          <Input
            id="axis"
            type="number"
            min="1"
            inputMode="numeric"
            placeholder={isDuration ? "Ex: 50" : "Ex: 2"}
            invalid={!!errors.axis}
            list={isDuration ? "duration-suggestions" : undefined}
            {...register("axis")}
          />
          {isDuration && (
            <datalist id="duration-suggestions">
              {DURATION_SUGGESTIONS.map((minutes) => (
                <option key={minutes} value={minutes} />
              ))}
            </datalist>
          )}
        </FormField>

        <FormField label="Valor da sessão (R$)" htmlFor="sessionValue" error={errors.sessionValue?.message}>
          <Input
            id="sessionValue"
            inputMode="decimal"
            placeholder="0,00"
            invalid={!!errors.sessionValue}
            {...register("sessionValue")}
          />
        </FormField>

        <div className={styles.info}>
          <Info size={18} aria-hidden="true" />
          <p>
            Caso essa combinação já possua um valor cadastrado, a vigência anterior será encerrada na data de hoje e o
            novo preço passará a valer imediatamente.
          </p>
        </div>

        {savePrice.isError && (
          <span className={styles.error} role="alert">
            {getErrorMessage(savePrice.error)}
          </span>
        )}

        <div className={styles.actions}>
          <button type="button" className={styles.cancel} onClick={onClose}>
            Cancelar
          </button>
          <Button type="submit" loading={savePrice.isPending}>
            Salvar Preço
          </Button>
        </div>
      </form>
    </Modal>
  );
}
