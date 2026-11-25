// Detalhe de uma versão de preço do plano (aberto pelo olho da tabela): um card por duração ou
// frequência, com o valor. Versões abertas (vigente ou vencida) podem ser editadas ("Editar plano")
import { Pencil } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { hasMixedValidity } from "../display/formatPlanValidity";
import { formatAxisTitle } from "../display/priceAxis";
import { formatRowPeriod, getPriceStatus } from "../display/priceStatus";
import { findPriceByAxis } from "../utils/planAxes";
import styles from "./PlanPriceDetailModal.module.css";

export function PlanPriceDetailModal({ row, axes, pricingModel, groupName, onEdit, onClose }) {
  const status = getPriceStatus(row.status);
  const isOpen = row.status !== "CLOSED";
  // Versão aberta com datas diferentes ou vencida: mostra em cada card desde quando o valor vale
  const showDatePerCard = isOpen && (hasMixedValidity(row.prices) || row.status === "EXPIRED");

  return (
    <Modal
      variant="soft"
      title={row.planName}
      eyebrow={<Badge>{groupName}</Badge>}
      onClose={onClose}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Fechar
          </Button>
          {isOpen && (
            <Button onClick={onEdit}>
              <Pencil size={16} aria-hidden="true" /> Editar plano
            </Button>
          )}
        </>
      }
    >
      <div className={styles.cards}>
        {axes.map((axis) => {
          const price = findPriceByAxis(row, axis);
          return (
            <section key={axis} className={styles.card}>
              <span className={styles.label}>{formatAxisTitle(axis, pricingModel)}</span>
              <span className={styles.value}>{price ? formatCurrency(price.sessionValue) : "—"}</span>
              {!price && (
                <span className={styles.caption}>
                  Sem preço nesta {pricingModel === "DURATION_BASED" ? "duração" : "frequência"}.
                </span>
              )}
              {price && showDatePerCard && (
                <span className={styles.caption}>
                  Desde {formatDate(price.validFrom)}
                  {price.status === "EXPIRED" ? " · Vencido" : ""}
                </span>
              )}
            </section>
          );
        })}
      </div>

      <dl className={styles.details}>
        <div>
          <dt>Grupo de atendimento</dt>
          <dd>{groupName}</dd>
        </div>
        <div>
          <dt>Vigência</dt>
          <dd>{formatRowPeriod(row)}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <Badge variant={status.badge}>{status.label}</Badge>
          </dd>
        </div>
      </dl>
    </Modal>
  );
}
