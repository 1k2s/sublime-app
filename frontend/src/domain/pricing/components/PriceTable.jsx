// Tabela de preços vigentes de um grupo. Mostra duração ou frequência conforme o pricingModel
import { Pencil } from "lucide-react";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatPriceAxis, getPriceAxisLabel } from "../priceAxis";
import styles from "./PriceTable.module.css";

export function PriceTable({ prices, pricingModel, onEdit }) {
  const columns = [
    { key: "plan", header: "Plano", emphasized: true, cell: (price) => price.planName },
    { key: "axis", header: getPriceAxisLabel(pricingModel), cell: formatPriceAxis },
    {
      key: "value",
      header: "Valor da sessão",
      align: "right",
      cell: (price) => <span className={styles.value}>{formatCurrency(price.sessionValue)}</span>,
    },
    {
      key: "validFrom",
      header: "Vigência",
      // O endpoint só devolve linhas vigentes: a vigência mostra desde quando o valor vale
      cell: (price) => `Desde ${formatDate(price.validFrom)}`,
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (price) => (
        <IconButton label={`Reajustar ${price.planName}`} title="Reajustar" onClick={() => onEdit(price)}>
          <Pencil size={16} />
        </IconButton>
      ),
    },
  ];

  return <DataTable columns={columns} rows={prices} />;
}
