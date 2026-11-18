// Tabela de preços do grupo: uma linha por versão do plano (a aberta — vigente ou vencida — e as
// encerradas por reajuste), um eixo (duração ou frequência) por coluna. O olho abre o detalhe
import { Eye } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { formatCurrency } from "@/shared/utils/money";
import { formatAxisTitle } from "../display/priceAxis";
import { getPriceStatus } from "../display/priceStatus";
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
            {rows.map((row) => {
              const status = getPriceStatus(row.status);
              const isClosed = row.status === "CLOSED";
              return (
                <tr key={row.key} className={isClosed ? styles.closed : undefined}>
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
                    <Badge variant={status.badge}>{status.label}</Badge>
                  </td>
                  <td className={styles.actions}>
                    <button
                      type="button"
                      className={styles.action}
                      onClick={() => onView(row.key)}
                      aria-label={`Ver ${row.planName} (${status.label})`}
                      title="Ver detalhes"
                    >
                      <Eye size={18} />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
