import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Wallet } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Modal } from "@/shared/components/Modal";
import { Select } from "@/shared/components/Select";
import { applyApiFieldErrors } from "@/shared/http/apiFieldErrors";
import { formatCurrency, formatPercentage } from "@/shared/utils/money";
import { PAYMENT_METHOD_OPTIONS } from "@/domain/contract/display/paymentMethods";
import { getSuggestionLabel, OTHER_AMOUNT } from "../display/paymentSuggestions";
import { formatReceivables } from "../display/receivables";
import { toPaymentFormValues, usePaymentDraft } from "../hooks/usePaymentDraft";
import { paymentSchema } from "../schemas/paymentSchema";
import styles from "./RegisterPaymentModal.module.css";

const FORM_ID = "register-payment-form";
const CARD_INSTALLMENTS = Array.from({ length: 12 }, (_, index) => index + 1);

// A conferência antes de salvar: a taxa e a previsão de recebimento, da prévia do backend
function Summary({ preview }) {
  if (!preview.isEnabled) return null;
  if (preview.isError) return <p className={styles.previewError}>{preview.error?.message}</p>;
  if (!preview.data) return <div className={styles.summary}>Calculando...</div>;
  const { fee, feePercentage, receivables } = preview.data;
  return (
    <div className={styles.summary}>
      <span>
        Taxa: <strong>{Number(fee) > 0 ? `${formatCurrency(fee)} · ${formatPercentage(feePercentage)}` : "sem taxa"}</strong>
      </span>
      <span>
        Previsão de recebimento: <strong>{formatReceivables(receivables)}</strong>
      </span>
    </div>
  );
}

// Registrar um pagamento (só pelo Financeiro): o contrato (os da conta ou "Sem contrato
// (avulso)"), o que está sendo pago (as sugestões da API ou "Outro valor": cada opção preenche o
// valor, editável), a forma, o "parcelado em" (só no crédito), o bruto e o líquido (preenchido com
// o bruto), a data e a conferência (a taxa e a previsão, da prévia). onSubmit(request, {
// applyApiErrors }): a page grava; a regra é da API (ver "Payment" no domain-model)
export function RegisterPaymentModal({ patientId, patientName, account, isSubmitting = false, onSubmit, onCancel }) {
  const {
    control,
    register,
    handleSubmit,
    setValue,
    getFieldState,
    setError,
    formState: { errors },
  } = useForm({ resolver: zodResolver(paymentSchema), defaultValues: toPaymentFormValues(account) });
  const draft = usePaymentDraft({ control, setValue, getFieldState, account, patientId });
  const [selectedSuggestion, selectedContractId] = useWatch({ control, name: ["suggestion", "contractId"] });

  function submit(values) {
    onSubmit?.(draft.buildRequest(values), { applyApiErrors: (error) => applyApiFieldErrors(error, setError) });
  }

  const footer = (
    <>
      <Button variant="outline" onClick={onCancel} disabled={isSubmitting}>
        Cancelar
      </Button>
      <Button type="submit" form={FORM_ID} loading={isSubmitting}>
        <Check size={16} aria-hidden /> Registrar pagamento
      </Button>
    </>
  );

  return (
    <Modal
      icon={Wallet}
      title="Registrar pagamento"
      description={patientName}
      footer={footer}
      onClose={isSubmitting ? () => {} : onCancel}
      closeOnBackdrop={false}
    >
      <form id={FORM_ID} className={styles.form} onSubmit={handleSubmit(submit)} noValidate>
        <FormField label="Contrato" htmlFor="payment-contract" error={errors.contractId?.message}>
          <Select
            id="payment-contract"
            value={selectedContractId}
            onChange={(event) => draft.selectContract(event.target.value)}
          >
            {draft.contractOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </FormField>

        <fieldset className={styles.choices}>
          <legend className={styles.legend}>O que está sendo pago</legend>
          {[...draft.suggestions, { type: OTHER_AMOUNT }].map((suggestion) => (
            <label
              key={suggestion.type}
              className={[styles.choice, selectedSuggestion === suggestion.type && styles.choiceOn].filter(Boolean).join(" ")}
            >
              <span>
                <input
                  type="radio"
                  name="suggestion"
                  checked={selectedSuggestion === suggestion.type}
                  onChange={() => draft.selectSuggestion(suggestion.type)}
                />
                {suggestion.type === OTHER_AMOUNT ? "Outro valor" : getSuggestionLabel(suggestion)}
              </span>
              <span className={styles.choiceValue}>{suggestion.amount != null && formatCurrency(suggestion.amount)}</span>
            </label>
          ))}
        </fieldset>

        <div className={styles.row}>
          <FormField label="Forma de pagamento" htmlFor="payment-method" error={errors.paymentMethod?.message}>
            <Select id="payment-method" invalid={Boolean(errors.paymentMethod)} {...register("paymentMethod")}>
              {PAYMENT_METHOD_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </FormField>
          {draft.isCredit && (
            <FormField label="Parcelado em" htmlFor="payment-card" error={errors.cardInstallments?.message}>
              <Select id="payment-card" {...register("cardInstallments")}>
                {CARD_INSTALLMENTS.map((count) => (
                  <option key={count} value={String(count)}>
                    {count === 1 ? "À vista (1x)" : `${count}x`}
                  </option>
                ))}
              </Select>
            </FormField>
          )}
        </div>

        <div className={styles.row}>
          <FormField label="Valor pago (bruto)" htmlFor="payment-amount" error={errors.amount?.message}>
            <Input
              id="payment-amount"
              prefix="R$"
              inputMode="decimal"
              placeholder="0,00"
              invalid={Boolean(errors.amount)}
              {...register("amount", { onChange: (event) => draft.changeAmount(event.target.value) })}
            />
          </FormField>
          <FormField label="Valor recebido (líquido)" htmlFor="payment-net" error={errors.netAmount?.message}>
            <Input
              id="payment-net"
              prefix="R$"
              inputMode="decimal"
              placeholder="0,00"
              invalid={Boolean(errors.netAmount)}
              {...register("netAmount")}
            />
          </FormField>
        </div>

        <div className={styles.row}>
          <FormField label="Data do pagamento" htmlFor="payment-date" error={errors.paidAt?.message}>
            <Input id="payment-date" type="date" invalid={Boolean(errors.paidAt)} {...register("paidAt")} />
          </FormField>
        </div>

        {draft.exceedsOpen && (
          <p className={styles.warning}>
            {draft.exceedsOpen.largestOpen > 0
              ? `O valor passa do maior em aberto (${formatCurrency(draft.exceedsOpen.largestOpen)}): o que passar fica como saldo credor.`
              : "Não há nada em aberto nesta conta: o valor fica como saldo credor."}
          </p>
        )}

        <p className={styles.hint}>Use vírgula para os centavos. O líquido é o que caiu na conta, depois da taxa.</p>

        <Summary preview={draft.preview} />
      </form>
    </Modal>
  );
}
