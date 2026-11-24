// Alteração dos valores de um plano no grupo, um campo por duração ou frequência. Entrega à page
// os valores do formulário; quem monta os pedidos (só os alterados) e salva é a page
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Modal } from "@/shared/components/Modal";
import { toMoneyInputValue } from "@/shared/utils/money";
import { formatAxisShort, formatAxisTitle } from "../display/priceAxis";
import { buildPlanPricesSchema } from "../schemas/priceSchema";
import { findPriceByAxis } from "../utils/planAxes";
import { axisFieldName } from "../utils/planPriceChanges";
import styles from "./PlanPriceFormModal.module.css";

const FORM_ID = "plan-price-form";

// "O valor de 60 min é opcional." ou "Os valores de 30 min e 60 min são opcionais."
function optionalHint(optionalAxes, pricingModel) {
  if (optionalAxes.length === 0) return "";
  const names = optionalAxes.map((axis) => formatAxisShort(axis, pricingModel)).join(" e ");
  return optionalAxes.length === 1 ? ` O valor de ${names} é opcional.` : ` Os valores de ${names} são opcionais.`;
}

export function PlanPriceFormModal({ plan, axes, pricingModel, groupName, isSaving, onSubmit, onCancel, onClose }) {
  // Eixo sem preço vigente pode ficar vazio; com preço, a API não remove, só substitui
  const optionalAxes = axes.filter((axis) => !findPriceByAxis(plan, axis));
  const fields = axes.map((axis) => ({ name: axisFieldName(axis), required: !optionalAxes.includes(axis) }));

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(buildPlanPricesSchema(fields)),
    defaultValues: Object.fromEntries(
      axes.map((axis) => [axisFieldName(axis), toMoneyInputValue(findPriceByAxis(plan, axis)?.sessionValue)])
    ),
  });

  return (
    <Modal
      variant="soft"
      title={plan.planName}
      eyebrow={<Badge>{groupName}</Badge>}
      onClose={onClose}
      closeOnBackdrop={false}
      footer={
        <>
          <Button variant="outline" onClick={onCancel} disabled={isSaving}>
            Cancelar
          </Button>
          <Button type="submit" form={FORM_ID} loading={isSaving}>
            <Check size={16} aria-hidden="true" /> Salvar alterações
          </Button>
        </>
      }
    >
      <form id={FORM_ID} className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className={styles.fields}>
          {axes.map((axis, index) => {
            const name = axisFieldName(axis);
            return (
              <FormField key={axis} label={formatAxisTitle(axis, pricingModel)} htmlFor={name} error={errors[name]?.message}>
                <Input
                  id={name}
                  prefix="R$"
                  inputMode="decimal"
                  placeholder={optionalAxes.includes(axis) ? "Sem preço" : "0,00"}
                  autoFocus={index === 0}
                  invalid={!!errors[name]}
                  {...register(name)}
                />
              </FormField>
            );
          })}
        </div>
        <p className={styles.hint}>
          Use vírgula para os centavos, por exemplo 132,00.{optionalHint(optionalAxes, pricingModel)}
        </p>
      </form>
    </Modal>
  );
}
