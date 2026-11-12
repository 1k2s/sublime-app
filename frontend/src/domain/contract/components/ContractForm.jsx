import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { useQueries } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { Layers, ListChecks, Plus, User } from "lucide-react";
import { getTechniquePrices } from "@/domain/pricing/api/pricingApi";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { applyApiFieldErrors } from "@/shared/http/apiFieldErrors";
import { useContractPreview } from "../api/useContracts";
import { resolveItemLine } from "../resolveItemLine";
import { contractSchema } from "../schemas/contractSchema";
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

// O item no formato do ContractItemRequestDTO, com a linha achada pelo resolveItemLine: o modelo
// do grupo diz qual das duas vai (exclusive arc)
function toItemRequest(item, line) {
  const techniqueId = Number(item.techniqueId);
  if (item.pricingModel === "DURATION_BASED") {
    return { techniqueId, sessionDurationPriceId: line.id, frequency: item.frequency };
  }
  return { techniqueId, sessionFrequencyPriceId: line.id };
}

// Chave da linha (as duas tabelas de preço têm ids próprios): liga o item ao valor da prévia
function lineKey(durationPriceId, frequencyPriceId) {
  return durationPriceId != null ? `d${durationPriceId}` : `f${frequencyPriceId}`;
}

// Cadastro do contrato no modelo real, na ordem em que a clínica pensa: paciente, plano, itens
// (um por grupo de preço) e vigência. O item guarda as escolhas (duração, frequência), e a linha
// de preço sai do plano: trocar o plano mantém os itens e atualiza os valores. O fim e as opções
// do plano do Grupo vêm da prévia do backend. plans, techniques e pricingGroups: o catálogo,
// carregado pela page
export function ContractForm({
  className,
  plans = [],
  techniques = [],
  pricingGroups = [],
  isCatalogLoading = false,
  isCatalogError = false,
  onSubmit,
  onCancel,
  isSubmitting = false,
}) {
  const {
    control,
    register,
    setValue,
    setError,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(contractSchema), defaultValues: DEFAULT_VALUES });
  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const values = useWatch({ control });
  const items = values.items ?? [];

  // Planos contratáveis: ativos e sem o Avulso (que nunca entra em contrato)
  const contractPlans = plans.filter((plan) => plan.active && plan.type !== "SINGLE_SESSION");
  const plan = plans.find((item) => String(item.id) === values.planId) ?? null;

  // Técnicas ativas, com o modelo do grupo (diz os campos do item). Plano de mensalidade só tem
  // o Pilates em Grupo
  const modelByGroupId = new Map(pricingGroups.map((group) => [group.id, group.pricingModel]));
  const catalogTechniques = techniques
    .filter((technique) => technique.active)
    .map((technique) => ({ ...technique, pricingModel: modelByGroupId.get(technique.pricingGroupId) }));
  const techniqueById = new Map(catalogTechniques.map((technique) => [String(technique.id), technique]));
  const allowedTechniques =
    plan?.type === "MONTHLY_FEE"
      ? catalogTechniques.filter((technique) => technique.pricingModel === "FREQUENCY_BASED")
      : catalogTechniques;

  // Um item por grupo: cada item não oferece as técnicas dos grupos usados nos outros. A técnica
  // já escolhida continua na lista do próprio item (o aviso dele explica se não serve ao plano).
  // index -1 = um item novo (todos os grupos usados contam)
  function techniqueOptionsFor(index) {
    const usedGroupIds = new Set(
      items
        .filter((_, itemIndex) => itemIndex !== index)
        .map((item) => techniqueById.get(item?.techniqueId)?.pricingGroupId)
        .filter(Boolean),
    );
    const current = techniqueById.get(items[index]?.techniqueId);
    const options = allowedTechniques.filter((technique) => !usedGroupIds.has(technique.pricingGroupId));
    return current && !options.includes(current) ? [current, ...options] : options;
  }
  const canAddItem = techniqueOptionsFor(-1).length > 0;

  // Linhas de preço de cada técnica escolhida, numa leva só (mesmo cache do useTechniquePrices)
  const techniqueIds = [...new Set(items.map((item) => item?.techniqueId).filter(Boolean))];
  const pricesQueries = useQueries({
    queries: techniqueIds.map((techniqueId) => ({
      queryKey: ["prices", Number(techniqueId)],
      queryFn: () => getTechniquePrices(Number(techniqueId)),
    })),
  });
  const pricesByTechniqueId = new Map(techniqueIds.map((techniqueId, index) => [techniqueId, pricesQueries[index]]));

  // A linha de preço de cada item, pelo plano atual (ou o aviso de por que não há linha)
  const resolutions = items.map((item) =>
    resolveItemLine(item, {
      plan,
      prices: pricesByTechniqueId.get(item?.techniqueId)?.data?.prices,
      technique: techniqueById.get(item?.techniqueId),
    }),
  );

  // Prévia do contrato (fim, opções do Grupo, valores, total e parcelas), com as contas do
  // backend: os itens com linha, no formato do cadastro, e as parcelas escolhidas (vazio = o
  // máximo; acima do máximo, a prévia aplica o máximo)
  const resolvedItems = items.flatMap((item, index) =>
    resolutions[index].line ? [toItemRequest(item, resolutions[index].line)] : [],
  );
  const previewQuery = useContractPreview({
    planId: values.planId ? Number(values.planId) : null,
    startDate: values.startDate,
    items: resolvedItems,
    installments: Number(values.installments) || null,
  });
  const preview = {
    isEnabled: Boolean(values.planId && values.startDate),
    isLoading: previewQuery.isLoading,
    isError: previewQuery.isError,
    error: previewQuery.error,
    data: previewQuery.data,
  };
  const groupPlanOptions = previewQuery.data?.groupPlanOptions ?? [];
  const previewItemByLine = new Map(
    (previewQuery.data?.items ?? []).map((previewItem) => [
      lineKey(previewItem.sessionDurationPriceId, previewItem.sessionFrequencyPriceId),
      previewItem,
    ]),
  );

  // O plano do Grupo escolhido saiu das opções (a prévia mudou com o plano ou as frequências).
  // Conferido depois da prévia: as opções vêm dela
  function groupPlanIssue(item) {
    const isStale =
      item?.pricingModel === "FREQUENCY_BASED" &&
      plan?.type === "PACKAGE" &&
      item.groupPlanId &&
      previewQuery.data &&
      !groupPlanOptions.some((option) => String(option.id) === item.groupPlanId);
    return isStale ? "As opções do plano do Grupo mudaram: escolha de novo." : null;
  }
  const issues = items.map((item, index) => resolutions[index].issue ?? groupPlanIssue(item));

  // Sessões e total de cada item, da prévia
  function previewItemOf(item, line) {
    if (!line) return null;
    const isDuration = item.pricingModel === "DURATION_BASED";
    return previewItemByLine.get(lineKey(isDuration ? line.id : null, isDuration ? null : line.id)) ?? null;
  }

  // Erro da API: a page chama no onError, para cada erro por campo aparecer no seu campo
  function applyApiErrors(error) {
    applyApiFieldErrors(error, setError, toFormField);
  }

  // Entrega no formato do ContractRequestDTO (o fim e os valores não vão: são calculados). Item
  // sem linha no plano não vai: o aviso dele diz o motivo. As parcelas são as que a prévia
  // aplicou (as escolhidas, ou o máximo): por isso a prévia precisa estar em dia
  function submit(formValues) {
    if (issues.some(Boolean) || resolutions.some((resolution) => !resolution.line)) {
      setError("items", { type: "manual", message: "Há itens sem preço no plano escolhido: confira os avisos." });
      return;
    }
    if (!previewQuery.data || previewQuery.isPlaceholderData || previewQuery.isError) {
      setError("installments", { type: "manual", message: "Aguarde o cálculo do contrato (fim e valor, no rodapé)." });
      return;
    }
    onSubmit?.(
      {
        patientId: formValues.patient.id,
        beneficiaryId: formValues.beneficiary?.id ?? null,
        planId: Number(formValues.planId),
        startDate: formValues.startDate,
        paymentMethod: formValues.paymentMethod,
        installments: previewQuery.data.installments,
        items: formValues.items.map((item, index) => toItemRequest(item, resolutions[index].line)),
      },
      { applyApiErrors },
    );
  }

  const itemsError = errors.items?.message ?? errors.items?.root?.message;

  return (
    <form className={[styles.form, className].filter(Boolean).join(" ")} onSubmit={handleSubmit(submit)} noValidate>
      {/* O conteúdo rola; o rodapé com o resultado e os botões fica fixo */}
      <div className={styles["form-content"]}>
        {/* Paciente e Plano lado a lado: o formulário fica mais curto */}
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
                      excludeIds={values.patient ? [values.patient.id] : []}
                    />
                  )}
                />
              </FormField>
            </div>
          </ContractSection>

          <ContractSection icon={Layers} title="Plano" description="Os valores dos itens se ajustam ao plano escolhido.">
            {isCatalogLoading && <p className={styles["form-hint"]}>Carregando os planos...</p>}
            {isCatalogError && <p className={styles["form-error"]}>Não foi possível carregar os planos.</p>}
            <Controller
              name="planId"
              control={control}
              render={({ field }) => (
                <ContractPlanPicker
                  id="contract-plan"
                  plans={contractPlans}
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

        <ContractSection icon={ListChecks} title="Itens do contrato" description="Um item por grupo de preço.">
          <div className={styles["form-items"]}>
            {itemsError && <p className={styles["form-error"]}>{itemsError}</p>}

            <ContractItemsHeader />
            {fields.map((field, index) => {
              const pricesQuery = pricesByTechniqueId.get(items[index]?.techniqueId);
              // Logo após o "Adicionar item", o fields já tem o item novo, mas os valores do
              // useWatch (items e resolutions) só chegam no render seguinte
              const line = resolutions[index]?.line ?? null;
              return (
                <ContractItemFields
                  key={field.id}
                  index={index}
                  item={items[index]}
                  errors={errors.items?.[index]}
                  register={register}
                  setValue={setValue}
                  techniqueOptions={techniqueOptionsFor(index)}
                  prices={pricesQuery?.data?.prices}
                  isPricesLoading={pricesQuery?.isLoading}
                  isPricesError={pricesQuery?.isError}
                  plan={plan}
                  groupPlanOptions={groupPlanOptions}
                  suggestedGroupPlanId={previewQuery.data?.suggestedGroupPlanId}
                  isPreviewLoading={previewQuery.isLoading}
                  resolution={{ line, issue: issues[index] ?? null }}
                  itemValue={previewItemOf(items[index] ?? {}, line)}
                  canRemove={fields.length > 1}
                  onRemove={() => remove(index)}
                />
              );
            })}

            <div className={styles["form-add"]}>
              <Button variant="outline" onClick={() => append({ ...EMPTY_ITEM })} disabled={!canAddItem}>
                <Plus size={16} aria-hidden /> Adicionar item
              </Button>
            </div>
          </div>
        </ContractSection>

        <ContractTermSection control={control} register={register} errors={errors} preview={preview} />
      </div>

      <ContractResultBar preview={preview} isSubmitting={isSubmitting} onCancel={onCancel} />
    </form>
  );
}
