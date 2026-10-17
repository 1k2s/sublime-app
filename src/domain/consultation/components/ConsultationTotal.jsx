// Total de repasse dos itens do carrinho, em um bloco separado
import { Card } from "@/shared/ui/Card";
import { formatCurrency, sumMoney } from "@/shared/utils/money";
import styles from "./ConsultationTotal.module.css";

export function ConsultationTotal({ className, items }) {
  const total = sumMoney(items.map((item) => item.providerPayoutValue));
  const countLabel = items.length === 1 ? "1 lançamento" : `${items.length} lançamentos`;

  return (
    <Card title="Total de repasse" className={className}>
      <div className={styles.content}>
        <p className={styles.value} aria-live="polite">
          {formatCurrency(total)}
        </p>
        <p className={styles.count}>{countLabel} no carrinho</p>
      </div>
    </Card>
  );
}
