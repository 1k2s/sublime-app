// Tabela de preços vigentes de um grupo. Mostra duração ou frequência conforme o pricingModel
import { Pencil } from "lucide-react";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatPriceAxis, getPriceAxisLabel } from "../priceAxis";
import styles from "./PriceTable.module.css";

export function PriceTable({ prices, pricingModel, onEdit }) {
  const axisLabel = getPriceAxisLabel(pricingModel);

  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Plano</th>
            <th>{axisLabel}</th>
            <th>Valor da sessão</th>
            <th>Vigência</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {prices.map((price) => (
            <tr key={price.id}>
              <td>{price.planName}</td>
              <td>{formatPriceAxis(price)}</td>
              <td className={styles.value}>{formatCurrency(price.sessionValue)}</td>
              <td>
                {/* O endpoint só devolve linhas vigentes: a vigência mostra desde quando o valor vale */}
                Desde {formatDate(price.validFrom)}
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
