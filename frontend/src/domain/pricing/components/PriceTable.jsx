// Tabela de preços: uma linha por técnica e versão do plano (a aberta — vigente ou vencida — e as
// encerradas por reajuste), um eixo (duração ou frequência) por coluna. Com grupos dos dois modelos
// (filtro "Todos"), entram as colunas dos dois, com "—" onde o grupo da linha não tem aquele eixo.
// O olho (ou o clique na linha) abre o detalhe; a linha aberta fica marcada (selectedRowKey)
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
        return <span className={styles.value}>{formatCurrency(price.sessionValue)}</span>;
      },
    }));
  });
}

export function PriceTable({ rows, showGroup, onView, selectedRowKey = null, className }) {
  const groupColumn = {
    key: "group",
    header: "Grupo",
    cell: (row) => row.group.name,
  };

  const columns = [
    ...(showGroup ? [groupColumn] : []),
    {
      key: "technique",
      header: "Técnica",
      cell: (row) => (row.technique ? row.technique.name : <span aria-label="Sem técnica ativa">—</span>),
    },
    { key: "plan", header: "Plano", cell: (row) => row.planName },
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

  return (
    <DataTable
      columns={columns}
      rows={rows}
      getRowKey={(row) => row.key}
      onRowClick={onView}
      selectedRowKey={selectedRowKey}
      // Muitas colunas (uma por duração ou frequência): cabe na largura, com os títulos quebrando linha
      compact
      className={className}
    />
  );
}
