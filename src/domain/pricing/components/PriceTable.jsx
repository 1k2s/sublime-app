// Tabela de preços vigentes de um grupo. Mostra duração ou frequência conforme o pricingModel
import { Pencil } from "lucide-react";
import { Badge } from "@/shared/ui/Badge";
import { formatCurrency } from "@/shared/utils/money";
import { formatDate } from "@/shared/utils/date";
import styles from "./PriceTable.module.css";

function formatAxis(price) {
  if (price.durationMinutes != null) return `${price.durationMinutes} min`;
  return `${price.weeklyFrequency}x por semana`;
}

export function PriceTable({ prices, pricingModel, onEdit }) {
  const axisLabel = pricingModel === "DURATION_BASED" ? "Duração" : "Frequência semanal";

  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Plano</th>
            <th>{axisLabel}</th>
            <th>Valor da sessão</th>
            <th>Vigência</th>
            <th>Status</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {prices.map((price) => (
            <tr key={price.id}>
              <td>{price.planName}</td>
              <td>{formatAxis(price)}</td>
              <td className={styles.value}>{formatCurrency(price.sessionValue)}</td>
              <td>Desde {formatDate(price.validFrom)}</td>
              <td>
                <Badge>Vigente</Badge>
              </td>
              <td>
                <button
                  type="button"
                  className={styles.action}
                  onClick={() => onEdit(price)}
                  aria-label={`Reajustar ${price.planName}`}
                  title="Reajustar"
                >
                  <Pencil size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
