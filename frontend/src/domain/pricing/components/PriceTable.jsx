// Tabela de preços do grupo: uma linha por versão do plano (a aberta — vigente ou vencida — e as
// encerradas por reajuste), um eixo (duração ou frequência) por coluna. O olho abre o detalhe
import { Eye } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { formatCurrency } from "@/shared/utils/money";
import { formatAxisTitle } from "../display/priceAxis";
import { getPriceStatus } from "../display/priceStatus";
import { findPriceByAxis } from "../utils/planAxes";
import styles from "./PriceTable.module.css";

function isClosed(row) {
  return row.status === "CLOSED";
}

// Versão encerrada em cinza, para a vigente se destacar quando as duas aparecem
function emphasisClass(row, className) {
  return isClosed(row) ? styles.closed : className;
}

export function PriceTable({ rows, axes, pricingModel, onView }) {
  const axisColumns = axes.map((axis) => ({
    key: `axis-${axis}`,
    header: formatAxisTitle(axis, pricingModel),
    align: "right",
    cell: (row) => {
      const price = findPriceByAxis(row, axis);
      if (!price) return <span aria-label="Sem preço">—</span>;
      return <span className={emphasisClass(row, styles.value)}>{formatCurrency(price.sessionValue)}</span>;
    },
  }));

  const columns = [
    {
      key: "plan",
      header: "Plano",
      cell: (row) => <span className={emphasisClass(row, styles.plan)}>{row.planName}</span>,
    },
    ...axisColumns,
    {
      key: "status",
      header: "Status",
      cell: (row) => {
        const status = getPriceStatus(row.status);
        return <Badge variant={status.badge}>{status.label}</Badge>;
      },
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (row) => (
        <IconButton
          label={`Ver ${row.planName} (${getPriceStatus(row.status).label})`}
          title="Ver detalhes"
          onClick={() => onView(row.key)}
        >
          <Eye size={16} />
        </IconButton>
      ),
    },
  ];

  return <DataTable columns={columns} rows={rows} getRowKey={(row) => row.key} />;
}
