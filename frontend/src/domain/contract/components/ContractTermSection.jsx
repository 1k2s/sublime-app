import { Controller } from "react-hook-form";
import { CalendarDays } from "lucide-react";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { addDaysToIsoDate, formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { PAYMENT_METHOD_OPTIONS } from "../display/paymentMethods";
import { ContractSection } from "./ContractSection";
import styles from "./ContractTermSection.module.css";

// A dica do 1º vencimento vazio: no cadastro, o início; no aditivo, o sugerido pela prévia (o dia
// do vencimento no mês em aberto, calculado pelo backend)
function firstDueDateHint(isAmendment, data) {
  if (!isAmendment) return "Vazio: o início do contrato";
  return data?.suggestedFirstDueDate ? `Vazio: ${formatDate(data.suggestedFirstDueDate)} (sugerido)` : "Vazio: o sugerido";
}

// Do máximo para baixo: o máximo (os meses do contrato) é o caso comum
function installmentOptions(maxInstallments) {
  return Array.from({ length: maxInstallments }, (_, index) => maxInstallments - index);
}

// Card de vigência e pagamento: início, forma de pagamento, parcelas e o 1º vencimento. O fim e o valor total
// aparecem no rodapé fixo (ContractResultBar); aqui fica a mensagem da prévia quando ela falha.
// As parcelas vêm da prévia do backend (o máximo são os meses do contrato): ver "Valor e
// parcelas" em Contract, no domain-model. preview: { isEnabled, isError, error, data }.
// isAmendment: no aditivo, o início fica travado e entra o "Prorrogar até" (opcional).
// currentEndDate: no aditivo, o fim atual. O calendário do "Prorrogar até" começa no dia seguinte
// (min): abre perto do fim, e não em hoje. A regra (passar do fim atual) é do backend
export function ContractTermSection({
  className,
  control,
  register,
  errors = {},
  preview,
  isAmendment = false,
  currentEndDate,
}) {
  const data = preview.isError ? null : preview.data;

  return (
    <ContractSection
      icon={CalendarDays}
      title="Vigência e pagamento"
      description={
        isAmendment
          ? "O fim só aumenta: por um item novo ou pela prorrogação."
          : "O fim e o valor são calculados pelo plano e pelos itens."
      }
      className={className}
    >
      <div className={styles.term}>
        <div className={styles["term-fields"]}>
          {isAmendment ? (
            <FormField label="Prorrogar até (opcional)" htmlFor="contract-end-date" error={errors.endDate?.message} reserveErrorSpace>
              <Input
                id="contract-end-date"
                type="date"
                min={addDaysToIsoDate(currentEndDate, 1) || undefined}
                invalid={Boolean(errors.endDate)}
                {...register("endDate")}
              />
            </FormField>
          ) : (
            <FormField label="Data de início *" htmlFor="contract-start-date" error={errors.startDate?.message} reserveErrorSpace>
              <Input id="contract-start-date" type="date" invalid={Boolean(errors.startDate)} {...register("startDate")} />
            </FormField>
          )}

          <FormField
            label="Forma de pagamento *"
            htmlFor="contract-payment"
            error={errors.paymentMethod?.message}
            reserveErrorSpace
          >
            <Select id="contract-payment" invalid={Boolean(errors.paymentMethod)} {...register("paymentMethod")}>
              <option value="">Selecione</option>
              {PAYMENT_METHOD_OPTIONS.map((method) => (
                <option key={method.value} value={method.value}>
                  {method.label}
                </option>
              ))}
            </Select>
          </FormField>

          {/* Mostra as parcelas que a prévia aplicou (nunca acima do máximo do contrato) */}
          <FormField label="Parcelas" htmlFor="contract-installments" error={errors.installments?.message} reserveErrorSpace>
            <Controller
              name="installments"
              control={control}
              render={({ field }) => (
                <Select
                  id="contract-installments"
                  disabled={!data}
                  invalid={Boolean(errors.installments)}
                  value={data ? String(data.installments) : ""}
                  onChange={(event) => field.onChange(event.target.value)}
                >
                  {!data && <option value="">Calculado com plano e início</option>}
                  {data &&
                    installmentOptions(data.maxInstallments).map((count) => (
                      <option key={count} value={String(count)}>
                        {count}x{count === data.maxInstallments ? " (máximo)" : ""}
                      </option>
                    ))}
                </Select>
              )}
            />
          </FormField>

          {/* Opcional: vazio, o backend aplica o padrão (o início, no cadastro; o sugerido pela prévia,
              no aditivo). O intervalo aceito é regra do backend: a mensagem dele aparece no erro */}
          <FormField
            label="1º vencimento"
            htmlFor="contract-first-due-date"
            error={errors.firstDueDate?.message}
            hint={firstDueDateHint(isAmendment, data)}
            reserveErrorSpace
          >
            <Input
              id="contract-first-due-date"
              type="date"
              invalid={Boolean(errors.firstDueDate)}
              {...register("firstDueDate")}
            />
          </FormField>
        </div>

        {/* Só no cadastro, com crédito disponível do titular (contratos anteriores com saldo credor,
            pela prévia): a caixa vem desmarcada. Aplicado, entra o crédito inteiro, e o valor
            parcelado é o total menos ele (o rodapé mostra) */}
        {!isAmendment && Number(data?.availableCredit) > 0 && (
          <label className={styles.credit}>
            <input type="checkbox" {...register("applyCredit")} />
            <span>
              Aplicar o crédito de <strong>{formatCurrency(data.availableCredit)}</strong> dos contratos anteriores
              <span className={styles["credit-hint"]}> (o valor parcelado fica o total menos o crédito)</span>
            </span>
          </label>
        )}

        {/* A mensagem da API (ex: "passa do prazo máximo de 52 semanas") por inteiro, sem travar a edição */}
        {preview.isEnabled && preview.isError && <p className={styles["term-note"]}>{preview.error?.message}</p>}
      </div>
    </ContractSection>
  );
}
