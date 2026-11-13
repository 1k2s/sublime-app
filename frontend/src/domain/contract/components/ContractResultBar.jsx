import { Check } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatInstallments } from "../display/formatInstallments";
import styles from "./ContractResultBar.module.css";

// Rodapé fixo do cadastro: o resultado do contrato (o fim e o valor total com as parcelas, da
// prévia do backend) e os botões. Fica sempre à vista: o efeito de cada escolha aparece sem rolar.
// Ver "Vigência e frequência" e "Valor e parcelas" em Contract, no domain-model.
// preview: { isEnabled, isLoading, isError, data } do useContractPreview
function getEndDate(preview) {
  if (!preview.isEnabled) return { value: "—", detail: "Escolha o plano e o início" };
  if (preview.isLoading) return { value: "Calculando...", detail: null };
  if (preview.isError || !preview.data) return { value: "—", detail: "Não calculado" };
  return { value: formatDate(preview.data.endDate), detail: `${preview.data.durationWeeks} semanas` };
}

export function ContractResultBar({ preview, isSubmitting = false, onCancel }) {
  const endDate = getEndDate(preview);
  const data = preview.isError ? null : preview.data;
  const hasTotal = Boolean(data && data.items.length > 0);

  return (
    <div className={styles.bar}>
      <div className={styles.result}>
        <span className={styles.label}>Fim do contrato</span>
        <span className={styles.value}>
          {endDate.value}
          {endDate.detail && <span className={styles.detail}> · {endDate.detail}</span>}
        </span>
      </div>

      <span className={styles.divider} aria-hidden />

      <div className={styles.result}>
        <span className={styles.label}>Valor total</span>
        {hasTotal ? (
          <span className={[styles.value, styles.total].join(" ")}>
            {formatCurrency(data.totalValue)}
            <span className={styles.detail}> · {formatInstallments(data)}</span>
          </span>
        ) : (
          <span className={styles.value}>
            —<span className={styles.detail}> · Escolha os itens</span>
          </span>
        )}
      </div>

      <div className={styles.actions}>
        <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
          Cancelar
        </Button>
        <Button type="submit" loading={isSubmitting}>
          <Check size={18} aria-hidden /> Criar contrato
        </Button>
      </div>
    </div>
  );
}
