import { ClipboardCheck } from "lucide-react";
import { formatPriceAxis, getPriceAxisLabel } from "@/domain/pricing/priceAxis";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { ContractSection } from "./ContractSection";
import styles from "./ContractSummary.module.css";

function formatValidity(validFrom, validTo) {
  if (!validFrom) return "";
  return `${formatDate(validFrom)} – ${validTo ? formatDate(validTo) : "Sem término"}`;
}

export function ContractSummary({
  className,
  patient,
  beneficiary,
  technique,
  plan,
  price,
  pricingModel,
  paymentLabel,
  validFrom,
  validTo,
}) {
  const items = [
    { label: "Paciente titular", value: patient?.name },
    { label: "Beneficiário", value: beneficiary?.name ?? "Nenhum" },
    { label: "Técnica", value: technique?.name },
    { label: "Plano", value: plan?.name },
    { label: getPriceAxisLabel(pricingModel), value: price && formatPriceAxis(price) },
    { label: "Forma de pagamento", value: paymentLabel },
    { label: "Valor da sessão", value: price && formatCurrency(price.sessionValue), highlight: true },
    { label: "Vigência", value: formatValidity(validFrom, validTo) },
  ];

  return (
    <ContractSection
      icon={ClipboardCheck}
      title="Informações selecionadas"
      description="Confira os dados antes de criar o contrato."
      className={className}
    >
      <dl className={styles["summary-grid"]}>
        {items.map((item) => (
          <div key={item.label} className={styles["summary-item"]}>
            <dt className={styles["summary-label"]}>{item.label}</dt>
            <dd
              className={[styles["summary-value"], item.highlight && styles["summary-value-highlight"]]
                .filter(Boolean)
                .join(" ")}
              title={item.value || undefined}
            >
              {item.value || "—"}
            </dd>
          </div>
        ))}
      </dl>
    </ContractSection>
  );
}
