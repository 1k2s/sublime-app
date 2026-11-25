// Tabela de preços: uma linha por técnica e versão do plano (a aberta — vigente ou vencida — e as
// encerradas por reajuste), um eixo (duração ou frequência) por coluna. Com grupos dos dois modelos
// (filtro "Todos"), entram as colunas dos dois, com "—" onde o grupo da linha não tem aquele eixo.
// O olho abre o detalhe
import { Eye } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { formatCurrency } from "@/shared/utils/money";
import { formatAxisTitle } from "../display/priceAxis";
import { getPriceStatus } from "../display/priceStatus";
import { findPriceByAxis, listGroupAxes } from "../utils/planAxes";
import styles from "./PriceTable.module.css";

const PRICING_MODELS = ["DURATION_BASED", "FREQUENCY_BASED"];

function isClosed(row) {
  return row.status === "CLOSED";
}

// Versão encerrada em cinza, para a vigente se destacar quando as duas aparecem
function emphasisClass(row, className) {
  return isClosed(row) ? styles.closed : className;
}

const noPrice = <span aria-label="Sem preço">—</span>;

// Colunas de valor: os eixos com preço nas linhas, primeiro os de duração e depois os de frequência
function buildAxisColumns(rows) {
  return PRICING_MODELS.flatMap((pricingModel) => {
    const modelRows = rows.filter((row) => row.group.pricingModel === pricingModel);
    return listGroupAxes(modelRows).map((axis) => ({
      key: `axis-${pricingModel}-${axis}`,
      header: formatAxisTitle(axis, pricingModel),
      align: "right",
      cell: (row) => {
        const price = row.group.pricingModel === pricingModel ? findPriceByAxis(row, axis) : undefined;
        if (!price) return noPrice;
        return <span className={emphasisClass(row, styles.value)}>{formatCurrency(price.sessionValue)}</span>;
      },
    }));
  });
}

export function PriceTable({ rows, showGroup, onView, className }) {
  const groupColumn = {
    key: "group",
    header: "Grupo",
    cell: (row) => <span className={emphasisClass(row, styles.text)}>{row.group.name}</span>,
  };

  const columns = [
    ...(showGroup ? [groupColumn] : []),
    {
      key: "technique",
      header: "Técnica",
      cell: (row) =>
        row.technique ? (
          <span className={emphasisClass(row, styles.technique)}>{row.technique.name}</span>
        ) : (
          <span aria-label="Sem técnica ativa">—</span>
        ),
    },
    {
      key: "plan",
      header: "Plano",
      cell: (row) => <span className={emphasisClass(row, styles.plan)}>{row.planName}</span>,
    },
    ...buildAxisColumns(rows),
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
          onClick={() => onView(row)}
        >
          <Eye size={16} />
        </IconButton>
      ),
    },
  ];

  return <DataTable columns={columns} rows={rows} getRowKey={(row) => row.key} className={className} />;
}
