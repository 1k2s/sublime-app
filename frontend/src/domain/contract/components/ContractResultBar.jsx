import { ArrowRight, Check } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatAmendedInstallments, formatCreatedInstallments } from "../display/formatInstallments";
import styles from "./ContractResultBar.module.css";

// Rodapé fixo do cadastro e do aditivo: o resultado do contrato (o fim e o valor total com as
// parcelas e os vencimentos, da prévia do backend; no aditivo, também o já pago, descontado do
// total) e os botões. Fica sempre à vista: o efeito de cada escolha aparece
// sem rolar. Ver "Vigência e frequência" e "Valor e parcelas" em Contract, no domain-model.
// preview: { isEnabled, isLoading, isError, data } da prévia. previous: no aditivo, a versão atual
// (o fim e o total aparecem como "antes → depois" quando mudam). hasChanges: no aditivo, sem
// nenhuma alteração o "Prosseguir" (para a revisão) fica inativo
function getEndDate(preview) {
  if (!preview.isEnabled) return { value: "—", detail: "Escolha o plano e o início" };
  if (preview.isLoading) return { value: "Calculando...", detail: null };
  if (preview.isError || !preview.data) return { value: "—", detail: "Não calculado" };
  return { value: formatDate(preview.data.endDate), detail: `${preview.data.durationWeeks} semanas` };
}

// O valor anterior riscado, quando mudou (só no aditivo)
function Before({ value, current }) {
  if (value == null || value === current) return null;
  return (
    <>
      <s className={styles.before}>{value}</s> →{" "}
    </>
  );
}

export function ContractResultBar({ preview, previous = null, hasChanges = true, isSubmitting = false, onCancel }) {
  const endDate = getEndDate(preview);
  const data = preview.isError ? null : preview.data;
  const hasTotal = Boolean(data && data.items.length > 0);
  const isAmendment = Boolean(previous);
  const total = hasTotal ? formatCurrency(data.totalValue) : null;

  return (
    <div className={styles.bar}>
      <div className={styles.result}>
        <span className={styles.label}>Fim do contrato</span>
        <span className={styles.value}>
          {isAmendment && data && <Before value={formatDate(previous.endDate)} current={endDate.value} />}
          {endDate.value}
          {endDate.detail && <span className={styles.detail}> · {endDate.detail}</span>}
        </span>
      </div>

      <span className={styles.divider} aria-hidden />

      <div className={styles.result}>
        <span className={styles.label}>Valor total</span>
        {hasTotal ? (
          <span className={[styles.value, styles.total].join(" ")}>
            {isAmendment && <Before value={formatCurrency(previous.totalValue)} current={total} />}
            {total}
            <span className={styles.detail}>
              {" "}
              · {isAmendment ? formatAmendedInstallments(data) : formatCreatedInstallments(data)}
            </span>
          </span>
        ) : (
          <span className={styles.value}>
            —<span className={styles.detail}> · Escolha os itens</span>
          </span>
        )}
      </div>

      <div className={styles.actions}>
        {isAmendment && !hasChanges && <span className={styles.note}>Nenhuma alteração</span>}
        <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        {isAmendment ? (
          <Button type="submit" disabled={!hasChanges}>
            Prosseguir <ArrowRight size={18} aria-hidden />
          </Button>
        ) : (
          <Button type="submit" loading={isSubmitting}>
            <Check size={18} aria-hidden /> Criar contrato
          </Button>
        )}
      </div>
    </div>
  );
}
