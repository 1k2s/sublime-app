// Detalhe de uma versão de preço do plano (aberto pelo olho da tabela): um card por duração ou
// frequência, com o valor. Só a versão vigente pode ser editada ("Editar plano")
import { Pencil } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { hasMixedValidity } from "../display/formatPlanValidity";
import { formatAxisTitle } from "../display/priceAxis";
import { formatRowPeriod, getPriceStatusLabel } from "../display/priceStatus";
import { findPriceByAxis } from "../utils/planAxes";
import styles from "./PlanPriceDetailModal.module.css";

export function PlanPriceDetailModal({ row, axes, pricingModel, groupName, onEdit, onClose }) {
  // Vigente reajustada em datas diferentes: cada card mostra desde quando o seu valor vale
  const showDatePerCard = row.isCurrent && hasMixedValidity(row.prices);

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
          {row.isCurrent && (
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
              {price && showDatePerCard && <span className={styles.caption}>Desde {formatDate(price.validFrom)}</span>}
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
            <Badge variant={row.isCurrent ? "active" : "inactive"}>{getPriceStatusLabel(row.isCurrent)}</Badge>
          </dd>
        </div>
      </dl>
    </Modal>
  );
}
