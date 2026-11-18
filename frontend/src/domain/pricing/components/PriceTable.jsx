// Tabela de preços do grupo: uma linha por versão do plano (a vigente e as encerradas por
// reajuste), um eixo (duração ou frequência) por coluna. O olho abre o detalhe da linha
import { Eye } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { formatCurrency } from "@/shared/utils/money";
import { formatAxisTitle } from "../display/priceAxis";
import { getPriceStatusLabel } from "../display/priceStatus";
import { findPriceByAxis } from "../utils/planAxes";
import styles from "./PriceTable.module.css";

export function PriceTable({ rows, axes, pricingModel, onView }) {
  return (
    <section className={styles.card}>
      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Plano</th>
              {axes.map((axis) => (
                <th key={axis}>{formatAxisTitle(axis, pricingModel)}</th>
              ))}
              <th>Status</th>
              <th className={styles.actions}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.key} className={row.isCurrent ? undefined : styles.closed}>
                <td>
                  <span className={styles.plan}>{row.planName}</span>
                </td>
                {axes.map((axis) => {
                  const price = findPriceByAxis(row, axis);
                  return (
                    <td key={axis}>
                      {price ? (
                        <span className={styles.value}>{formatCurrency(price.sessionValue)}</span>
                      ) : (
                        <span className={styles.muted} aria-label="Sem preço">
                          —
                        </span>
                      )}
                    </td>
                  );
                })}
                <td>
                  <Badge variant={row.isCurrent ? "active" : "inactive"}>{getPriceStatusLabel(row.isCurrent)}</Badge>
                </td>
                <td className={styles.actions}>
                  <button
                    type="button"
                    className={styles.action}
                    onClick={() => onView(row.key)}
                    aria-label={`Ver ${row.planName} (${getPriceStatusLabel(row.isCurrent)})`}
                    title="Ver detalhes"
                  >
                    <Eye size={18} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
