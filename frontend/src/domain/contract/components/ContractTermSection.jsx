import { Controller } from "react-hook-form";
import { CalendarDays } from "lucide-react";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { PAYMENT_METHOD_OPTIONS } from "../paymentMethods";
import { ContractSection } from "./ContractSection";
import styles from "./ContractTermSection.module.css";

// Do máximo para baixo: o máximo (os meses do contrato) é o caso comum
function installmentOptions(maxInstallments) {
  return Array.from({ length: maxInstallments }, (_, index) => maxInstallments - index);
}

// Card de vigência e pagamento: início, forma de pagamento e parcelas. O fim e o valor total
// aparecem no rodapé fixo (ContractResultBar); aqui fica a mensagem da prévia quando ela falha.
// As parcelas vêm da prévia do backend (o máximo são os meses do contrato): ver "Valor e
// parcelas" em Contract, no domain-model. preview: { isEnabled, isError, error, data }
export function ContractTermSection({ className, control, register, errors = {}, preview }) {
  const data = preview.isError ? null : preview.data;

  return (
    <ContractSection
      icon={CalendarDays}
      title="Vigência e pagamento"
      description="O fim e o valor são calculados pelo plano e pelos itens."
      className={className}
    >
      <div className={styles.term}>
        <div className={styles["term-fields"]}>
          <FormField label="Data de início *" htmlFor="contract-start-date" error={errors.startDate?.message} reserveErrorSpace>
            <Input id="contract-start-date" type="date" invalid={Boolean(errors.startDate)} {...register("startDate")} />
          </FormField>

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
        </div>

        {/* A mensagem da API (ex: "passa do prazo máximo de 52 semanas") por inteiro, sem travar a edição */}
        {preview.isEnabled && preview.isError && <p className={styles["term-note"]}>{preview.error?.message}</p>}
      </div>
    </ContractSection>
  );
}
