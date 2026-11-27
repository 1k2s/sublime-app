import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Layers, ListChecks, Lock, Plus, User } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { PageLoader } from "@/shared/components/PageLoader";
import { applyApiFieldErrors } from "@/shared/http/apiFieldErrors";
import { formatDate } from "@/shared/utils/date";
import { listAmendmentChanges } from "../display/amendmentChanges";
import { useContractDraft } from "../hooks/useContractDraft";
import { contractSchema } from "../schemas/contractSchema";
import { toAmendmentFormValues } from "../utils/amendmentFormValues";
import { ContractItemFields, ContractItemsHeader } from "./ContractItemFields";
import { ContractPlanPicker } from "./ContractPlanPicker";
import { ContractResultBar } from "./ContractResultBar";
import { ContractSection } from "./ContractSection";
import { ContractTermSection } from "./ContractTermSection";
import { PatientPicker } from "./PatientPicker";
import styles from "./ContractForm.module.css";

const EMPTY_ITEM = {
  techniqueId: "",
  pricingModel: "",
  durationMinutes: "",
  frequency: "",
  groupPlanId: "",
  weeklyFrequency: "",
};

const DEFAULT_VALUES = {
  patient: null,
  beneficiary: null,
  planId: "",
  planType: "",
  startDate: "",
  paymentMethod: "",
  installments: "",
  items: [EMPTY_ITEM],
};

// Nomes da API → campos do formulário (ver "Erros por campo da API" no architecture.md). Os do
// item chegam como items[0].campo; os @AssertTrue do item, com o nome do método sem "is"
const API_FIELDS = { patientId: "patient", beneficiaryId: "beneficiary" };
const API_ITEM_FIELDS = {
  sessionDurationPriceId: "durationMinutes",
  sessionFrequencyPriceId: "weeklyFrequency",
  validExclusiveArc: "techniqueId",
  frequencyConsistent: "frequency",
};

function toFormField(apiField) {
  const itemField = apiField.match(/^items\[(\d+)\]\.(\w+)$/);
  if (itemField) {
    const [, index, field] = itemField;
    return `items.${index}.${API_ITEM_FIELDS[field] ?? field}`;
  }
  return API_FIELDS[apiField] ?? apiField;
}

// Cadastro do contrato no modelo real, na ordem em que a clínica pensa: paciente, plano, itens
// (um por grupo de preço) e vigência. Aqui ficam o formulário e o layout; o comportamento dos
// itens e a prévia ficam no useContractDraft. plans, techniques e pricingGroups: o catálogo,
// carregado pela page.
// Aditivo (passo 1): contract é a versão atual (com patientName e beneficiaryName), e o formulário
// abre preenchido com ela. Titular, plano e início ficam travados; entra o "Prorrogar até"; o
// rodapé mostra o "antes → depois", e o envio leva à revisão: onSubmit(request, { applyApiErrors,
// changes }), com as "Alterações". sessionBalance: o saldo de sessões dos itens (do session-balance)
export function ContractForm({
  className,
  contract = null,
  sessionBalance = [],
  plans = [],
  techniques = [],
  pricingGroups = [],
  isCatalogLoading = false,
  isCatalogError = false,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) {
  const isAmendment = Boolean(contract);
  const {
    control,
    register,
    setValue,
    setError,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(contractSchema),
    defaultValues: isAmendment
      ? toAmendmentFormValues(contract, plans.find((plan) => plan.id === contract.planId)?.type)
      : DEFAULT_VALUES,
  });
  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const draft = useContractDraft({ control, plans, techniques, pricingGroups, amendedContract: contract });
  const [patient, items = [], beneficiary, paymentMethod, installments, endDate] = useWatch({
    control,
    name: ["patient", "items", "beneficiary", "paymentMethod", "installments", "endDate"],
  });

  // Aditivo: o que mudou em relação à versão atual (só para mostrar; o backend recusa o aditivo
  // sem alteração)
  const changes = isAmendment
    ? listAmendmentChanges({
        contract,
        form: { beneficiary, paymentMethod, installments, endDate },
        formItems: draft.formItems,
        preview: draft.preview.isError ? null : draft.preview.data,
        balances: sessionBalance,
      })
    : null;
  const balanceByGroupId = new Map(sessionBalance.map((balance) => [balance.pricingGroupId, balance]));

  // Erro da API: a page chama no onError, para cada erro por campo aparecer no seu campo
  function applyApiErrors(error) {
    applyApiFieldErrors(error, setError, toFormField);
  }

  // Entrega à page o pedido montado pelo draft, ou marca o erro que impede salvar
  function submit(formValues) {
    if (isAmendment && !changes.hasChanges) return;
    const { request, error } = draft.buildRequest(formValues);
    if (error) {
      setError(error.field, { type: "manual", message: error.message });
      return;
    }
    onSubmit?.(request, { applyApiErrors, changes: changes?.lines ?? [] });
  }

  // Aditivo: os dados que não mudam, só para conferir
  const lockedFields = isAmendment
    ? [
        { label: "Titular", value: contract.patientName },
        { label: "Plano", value: contract.planName },
        { label: "Início", value: formatDate(contract.startDate) },
        { label: "Fim atual", value: formatDate(contract.endDate) },
      ]
    : [];

  const itemsError = errors.items?.message ?? errors.items?.root?.message;

  return (
    <form className={[styles.form, className].filter(Boolean).join(" ")} onSubmit={handleSubmit(submit)} noValidate>
      {/* O conteúdo rola; o rodapé com o resultado e os botões fica fixo */}
      <div className={styles["form-content"]}>
        {isAmendment && (
          <div className={styles["form-top"]}>
            <ContractSection icon={Lock} title="Contrato" description="Titular, plano e início não mudam no aditivo.">
              <dl className={styles["form-locked"]}>
                {lockedFields.map((field) => (
                  <div key={field.label} className={styles["form-locked-field"]}>
                    <dt>{field.label}</dt>
                    <dd>{field.value || "--"}</dd>
                  </div>
                ))}
              </dl>
            </ContractSection>

            <ContractSection icon={User} title="Beneficiário" description="Mantenha, troque ou retire o beneficiário.">
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
                      excludeIds={[contract.patientId]}
                    />
                  )}
                />
              </FormField>
            </ContractSection>
          </div>
        )}

        {/* Paciente e Plano lado a lado: o formulário fica mais curto */}
        {!isAmendment && (
        <div className={styles["form-top"]}>
          <ContractSection icon={User} title="Paciente" description="Selecione o titular e, se houver, o beneficiário.">
            <div className={styles["form-pair"]}>
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
                      excludeIds={patient ? [patient.id] : []}
                    />
                  )}
                />
              </FormField>
            </div>
          </ContractSection>

          <ContractSection icon={Layers} title="Plano" description="Os valores dos itens se ajustam ao plano escolhido.">
            {isCatalogLoading && <PageLoader variant="compact" label="Carregando os planos..." />}
            {isCatalogError && <p className={styles["form-error"]}>Não foi possível carregar os planos.</p>}
            <Controller
              name="planId"
              control={control}
              render={({ field }) => (
                <ContractPlanPicker
                  id="contract-plan"
                  plans={draft.contractPlans}
                  value={field.value}
                  invalid={Boolean(errors.planId)}
                  onChange={(selectedPlan) => {
                    field.onChange(selectedPlan ? String(selectedPlan.id) : "");
                    setValue("planType", selectedPlan?.type ?? "");
                  }}
                />
              )}
            />
            {errors.planId && <p className={styles["form-error"]}>{errors.planId.message}</p>}
          </ContractSection>
        </div>
        )}

        <ContractSection
          icon={ListChecks}
          title="Itens do contrato"
          description={
            isAmendment ? "Um item por grupo de preço. O saldo soma todas as versões." : "Um item por grupo de preço."
          }
        >
          <div className={styles["form-items"]}>
            {itemsError && <p className={styles["form-error"]}>{itemsError}</p>}

            <ContractItemsHeader />
            {fields.map((field, index) => {
              const itemState = draft.itemStateFor(index);
              return (
                <ContractItemFields
                  key={field.id}
                  index={index}
                  item={items[index]}
                  errors={errors.items?.[index]}
                  register={register}
                  setValue={setValue}
                  techniqueOptions={draft.techniqueOptionsFor(index)}
                  prices={itemState.prices}
                  isPricesLoading={itemState.isPricesLoading}
                  isPricesError={itemState.isPricesError}
                  plan={draft.plan}
                  groupPlanOptions={draft.groupPlanOptions}
                  suggestedGroupPlanId={draft.suggestedGroupPlanId}
                  isPreviewLoading={draft.preview.isLoading}
                  resolution={itemState.resolution}
                  itemValue={itemState.itemValue}
                  canRemove={fields.length > 1}
                  onRemove={() => remove(index)}
                  isNew={itemState.isNew}
                  balance={itemState.lockedItem ? (balanceByGroupId.get(itemState.lockedItem.pricingGroupId) ?? null) : null}
                />
              );
            })}

            <div className={styles["form-add"]}>
              <Button variant="outline" onClick={() => append({ ...EMPTY_ITEM })} disabled={!draft.canAddItem}>
                <Plus size={16} aria-hidden /> Adicionar item
              </Button>
            </div>
          </div>
        </ContractSection>

        <ContractTermSection
          control={control}
          register={register}
          errors={errors}
          preview={draft.preview}
          isAmendment={isAmendment}
          currentEndDate={contract?.endDate}
        />
      </div>

      <ContractResultBar
        preview={draft.preview}
        previous={contract}
        hasChanges={changes?.hasChanges ?? true}
        isSubmitting={isSubmitting}
        onCancel={onCancel}
      />
    </form>
  );
}
