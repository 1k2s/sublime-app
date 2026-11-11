import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { CalendarDays, Check, Layers, User } from "lucide-react";
import { usePricingGroups, useTechniquePrices, useTechniques } from "@/domain/pricing/api/usePricing";
import { formatPriceAxis, getPriceAxisLabel } from "@/domain/pricing/priceAxis";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { contractSchema } from "../schemas/contractSchema";
import { ContractSection } from "./ContractSection";
import { ContractSummary } from "./ContractSummary";
import { PatientPicker } from "./PatientPicker";
import styles from "./ContractForm.module.css";

const PAYMENT_METHODS = [
  { value: "PIX", label: "Pix" },
  { value: "CREDIT_CARD", label: "Cartão de crédito" },
  { value: "DEBIT_CARD", label: "Cartão de débito" },
  { value: "CASH", label: "Dinheiro" },
];

const DEFAULT_VALUES = {
  patient: null,
  beneficiary: null,
  techniqueId: "",
  planId: "",
  priceId: "",
  paymentMethod: "",
  validFrom: "",
  validTo: "",
};

// Opções dos selects técnica → plano → frequência (ou duração), tiradas do catálogo de preços
function usePriceOptions({ techniqueId, planId, priceId }) {
  const techniquesQuery = useTechniques();
  const groupsQuery = usePricingGroups();

  const techniques = techniquesQuery.data ?? [];
  const technique = techniques.find((item) => String(item.id) === techniqueId) ?? null;
  const pricingModel = (groupsQuery.data ?? []).find((group) => group.id === technique?.pricingGroupId)?.pricingModel;

  const pricesQuery = useTechniquePrices(technique?.id ?? null);
  const prices = pricesQuery.data?.prices ?? [];

  const plans = [...new Map(prices.map((price) => [String(price.planId), price.planName])).entries()].map(
    ([id, name]) => ({ id, name }),
  );
  const axisOptions = prices.filter((price) => String(price.planId) === planId);

  return {
    techniques,
    technique,
    pricingModel,
    plans,
    plan: plans.find((item) => item.id === planId) ?? null,
    axisOptions,
    price: axisOptions.find((price) => String(price.id) === priceId) ?? null,
    isLoadingTechniques: techniquesQuery.isLoading || groupsQuery.isLoading,
    isLoadingPrices: pricesQuery.isLoading,
    isError: techniquesQuery.isError || groupsQuery.isError || pricesQuery.isError,
  };
}

export function ContractForm({ className, onSubmit, onCancel, isSubmitting = false }) {
  const {
    control,
    register,
    setValue,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(contractSchema), defaultValues: DEFAULT_VALUES });

  const values = useWatch({ control });
  const options = usePriceOptions(values);
  const { technique, plan, price, pricingModel } = options;
  const paymentLabel = PAYMENT_METHODS.find((method) => method.value === values.paymentMethod)?.label;

  function placeholder(isLoading, text) {
    if (options.isError) return "Não foi possível carregar";
    return isLoading ? "Carregando..." : text;
  }

  // O preço escolhido define qual dos dois ids vai preenchido (exclusive arc)
  function submit(formValues) {
    const isDuration = pricingModel === "DURATION_BASED";
    onSubmit({
      patientId: formValues.patient.id,
      patientName: formValues.patient.name,
      beneficiaryId: formValues.beneficiary?.id ?? null,
      beneficiaryName: formValues.beneficiary?.name ?? null,
      paymentMethod: formValues.paymentMethod,
      validFrom: formValues.validFrom,
      validTo: formValues.validTo || null,
      groupPlanPriceId: isDuration ? String(price.id) : null,
      groupPlanFrequencyPriceId: isDuration ? null : String(price.id),
      techniqueName: technique.name,
      planName: plan.name,
      weeklyFrequency: price.weeklyFrequency ?? null,
      durationMinutes: price.durationMinutes ?? null,
      sessionValue: price.sessionValue,
    });
  }

  return (
    <form
      className={[styles.form, className].filter(Boolean).join(" ")}
      onSubmit={handleSubmit(submit)}
      noValidate
    >
      <ContractSection
        icon={User}
        title="Paciente"
        description="Selecione o titular e, se houver, o beneficiário."
        className={styles["form-patient"]}
      >
        <div className={styles["form-stack"]}>
          <FormField label="Paciente titular *" htmlFor="contract-patient" error={errors.patient?.message} reserveErrorSpace>
            <Controller
              name="patient"
              control={control}
              render={({ field }) => (
                <PatientPicker
                  id="contract-patient"
                  value={field.value}
                  onChange={field.onChange}
                  invalid={Boolean(errors.patient)}
                />
              )}
            />
          </FormField>

          <FormField
            label="Beneficiário (opcional)"
            htmlFor="contract-beneficiary"
            error={errors.beneficiary?.message}
            reserveErrorSpace
          >
            <Controller
              name="beneficiary"
              control={control}
              render={({ field }) => (
                <PatientPicker
                  id="contract-beneficiary"
                  value={field.value}
                  onChange={field.onChange}
                  invalid={Boolean(errors.beneficiary)}
                />
              )}
            />
          </FormField>
        </div>
      </ContractSection>

      <ContractSection
        icon={CalendarDays}
        title="Vigência"
        description="Período negociado do contrato."
        className={styles["form-validity"]}
      >
        <div className={styles["form-stack"]}>
          <FormField label="Data de início *" htmlFor="contract-valid-from" error={errors.validFrom?.message} reserveErrorSpace>
            <Input
              id="contract-valid-from"
              type="date"
              invalid={Boolean(errors.validFrom)}
              {...register("validFrom")}
            />
          </FormField>

          <FormField label="Data de término" htmlFor="contract-valid-to" error={errors.validTo?.message} reserveErrorSpace>
            <Input id="contract-valid-to" type="date" invalid={Boolean(errors.validTo)} {...register("validTo")} />
          </FormField>
        </div>
      </ContractSection>

      <ContractSection
        icon={Layers}
        title="Plano e técnica"
        description="Defina a técnica, o plano, a frequência e a forma de pagamento."
        className={styles["form-plan"]}
      >
        <div className={styles["form-row"]}>
          <FormField label="Técnica *" htmlFor="contract-technique" error={errors.techniqueId?.message} reserveErrorSpace>
            <Select
              id="contract-technique"
              invalid={Boolean(errors.techniqueId)}
              {...register("techniqueId", {
                onChange: () => {
                  setValue("planId", "");
                  setValue("priceId", "");
                },
              })}
            >
              <option value="">{placeholder(options.isLoadingTechniques, "Selecione")}</option>
              {options.techniques.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {item.name}
                </option>
              ))}
            </Select>
          </FormField>

          <FormField label="Plano *" htmlFor="contract-plan" error={errors.planId?.message} reserveErrorSpace>
            <Select
              id="contract-plan"
              disabled={!technique}
              invalid={Boolean(errors.planId)}
              {...register("planId", { onChange: () => setValue("priceId", "") })}
            >
              <option value="">{placeholder(options.isLoadingPrices, "Selecione")}</option>
              {options.plans.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </Select>
          </FormField>

          <FormField
            label={`${getPriceAxisLabel(pricingModel)} *`}
            htmlFor="contract-price"
            error={errors.priceId?.message}
            reserveErrorSpace
          >
            <Select id="contract-price" disabled={!plan} invalid={Boolean(errors.priceId)} {...register("priceId")}>
              <option value="">Selecione</option>
              {options.axisOptions.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {formatPriceAxis(item)}
                </option>
              ))}
            </Select>
          </FormField>

          <FormField
            label="Forma de pagamento *"
            htmlFor="contract-payment"
            error={errors.paymentMethod?.message}
            reserveErrorSpace
          >
            <Select id="contract-payment" invalid={Boolean(errors.paymentMethod)} {...register("paymentMethod")}>
              <option value="">Selecione</option>
              {PAYMENT_METHODS.map((method) => (
                <option key={method.value} value={method.value}>
                  {method.label}
                </option>
              ))}
            </Select>
          </FormField>
        </div>
      </ContractSection>

      <ContractSummary
        className={styles["form-summary"]}
        patient={values.patient}
        beneficiary={values.beneficiary}
        technique={technique}
        plan={plan}
        price={price}
        pricingModel={pricingModel}
        paymentLabel={paymentLabel}
        validFrom={values.validFrom}
        validTo={values.validTo}
      />

      <div className={styles["form-footer"]}>
        <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" loading={isSubmitting}>
          <Check size={18} aria-hidden /> Criar contrato
        </Button>
      </div>
    </form>
  );
}
