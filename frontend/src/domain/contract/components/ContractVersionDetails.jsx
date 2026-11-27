import { formatPriceAxis } from "@/domain/pricing/display/priceAxis";
import { Badge } from "@/shared/components/Badge";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatInstallments } from "../display/formatInstallments";
import { getPaymentMethodLabel } from "../display/paymentMethods";
import { getSessionFrequencyLabel } from "../display/sessionFrequencies";
import styles from "./ContractVersionDetails.module.css";

const ITEM_BADGES = { added: "Novo", changed: "Alterado" };

// Os dados, os itens e as parcelas (vencimento e valor) de uma versão do contrato (a atual, na
// aba Contrato, ou uma do histórico), só para ler. version: já com beneficiaryName (withPatientNames). changes: no histórico, o que
// mudou em relação à versão anterior (getVersionChanges): os dados e os itens que mudaram ficam
// marcados em azul, com o valor anterior embaixo, e os itens removidos aparecem no fim, riscados.
// Sem changes (a aba Contrato), nada é marcado. frame: os dados e os itens em molduras de fundo
// claro, cada um na sua; "solid" na versão atual (aba Contrato e a atual no histórico), "dashed"
// numa versão anterior (substituída)
export function ContractVersionDetails({ version, changes = null, frame = null, className }) {
  const frameClass = frame ? [styles.frame, frame === "dashed" && styles.frameDashed].filter(Boolean).join(" ") : null;
  const changedFields = changes?.fields ?? {};
  const itemStatus = changes?.itemStatus ?? new Map();
  const removedItems = changes?.removedItems ?? [];
  // Do último incluído para o primeiro: o que acabou de entrar (em azul, no histórico) aparece no
  // topo. Cada versão grava os itens na ordem do pedido (os mantidos e depois os novos), e o id
  // segue essa ordem
  const items = [...version.items].sort((a, b) => b.id - a.id);

  const fields = [
    { label: "Plano", value: version.planName },
    { key: "paymentMethod", label: "Forma de pagamento", value: getPaymentMethodLabel(version.paymentMethod) },
    { label: "Início", value: formatDate(version.startDate) },
    { key: "endDate", label: "Fim", value: formatDate(version.endDate) },
    { label: "Criado em", value: formatDate(version.createdAt) },
    { key: "beneficiary", label: "Beneficiário", value: version.beneficiaryName ?? "Nenhum" },
    // Calculados pela API a partir das linhas travadas nos itens
    { key: "totalValue", label: "Valor total", value: formatCurrency(version.totalValue) },
    { key: "installments", label: "Parcelas", value: formatInstallments(version) },
    { label: "1º vencimento", value: formatDate(version.firstDueDate) },
    // No aditivo, o valor parcelado é o total menos o já pago antes dele (a API manda pronto)
    Number(version.installmentBase) !== Number(version.totalValue) && {
      label: "Valor parcelado",
      value: `${formatCurrency(version.installmentBase)} (descontado o já pago)`,
    },
    // A versão substituída por aditivo também tem a data (a do aditivo)
    version.deactivatedOn && {
      label: version.superseded ? "Substituída em" : "Cancelado em",
      value: formatDate(version.deactivatedOn),
    },
  ].filter(Boolean);
  const schedule = version.installmentSchedule ?? [];

  return (
    <div className={[styles.details, className].filter(Boolean).join(" ")}>
      <dl className={[styles.grid, frameClass].filter(Boolean).join(" ")}>
        {fields.map((field) => {
          const before = field.key ? changedFields[field.key] : undefined;
          return (
            <div
              key={field.label}
              className={[styles.field, before !== undefined && styles.changed].filter(Boolean).join(" ")}
            >
              <dt className={styles.label}>{field.label}</dt>
              <dd className={styles.value}>{field.value || "--"}</dd>
              {before !== undefined && <dd className={styles.before}>antes: {before}</dd>}
            </div>
          );
        })}
      </dl>

      <section className={[styles.items, frameClass].filter(Boolean).join(" ")}>
        <h4 className={styles.sectionTitle}>Itens ({items.length})</h4>
        <ul className={styles.itemList}>
          {items.map((item) => (
            <ItemRow key={item.id} item={item} status={itemStatus.get(item.id)} />
          ))}
          {removedItems.map((item) => (
            <ItemRow key={`removed-${item.id}`} item={item} status="removed" />
          ))}
        </ul>
      </section>

      {/* O cronograma: o vencimento e o valor de cada parcela, sem a situação (o que foi pago fica
          no Financeiro: o contract não lê os pagamentos) */}
      {schedule.length > 0 && (
        <section className={[styles.items, styles.schedule, frameClass].filter(Boolean).join(" ")}>
          <h4 className={styles.sectionTitle}>Parcelas ({schedule.length})</h4>
          <ul className={styles.scheduleList}>
            {schedule.map((installment) => (
              <li key={installment.number} className={styles.scheduleRow}>
                <span>{installment.number}ª</span>
                <span>{formatDate(installment.dueDate)}</span>
                <span className={styles.scheduleValue}>{formatCurrency(installment.value)}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

// status: "added" | "changed" (marcado em azul), "removed" (riscado) ou nenhum
function ItemRow({ item, status }) {
  const className = [styles.item, (status === "added" || status === "changed") && styles.itemChanged, status === "removed" && styles.itemRemoved]
    .filter(Boolean)
    .join(" ");

  return (
    <li className={className}>
      <div className={styles.itemText}>
        <span className={styles.itemTitle}>
          {item.techniqueName} <span className={styles.itemGroup}>· {item.pricingGroupName}</span>
          {status === "removed" ? (
            <Badge variant="inactive">Removido</Badge>
          ) : (
            ITEM_BADGES[status] && <Badge variant="suggestion">{ITEM_BADGES[status]}</Badge>
          )}
        </span>
        <span className={styles.itemDetail}>
          {[item.planName, formatPriceAxis(item), item.frequency && getSessionFrequencyLabel(item.frequency)]
            .filter(Boolean)
            .join(" · ")}
        </span>
      </div>
      <div className={styles.itemValues}>
        <span className={styles.itemValue}>{formatCurrency(item.sessionValue)} por sessão</span>
        <span className={styles.itemTotal}>
          {item.sessionCount} {item.sessionFrequencyPriceId ? "aulas" : "sessões"} · {formatCurrency(item.totalValue)}
        </span>
      </div>
    </li>
  );
}
