// Os números do período, como o backend devolve (o front não soma): o rendimento (só os
// realizados, com a quantidade), as faltas cobradas à parte e o repasse total.
// stacked: dois em cima e o repasse total embaixo, na largura toda (o ADMIN, com a lista dos
// prestadores ao lado); sem ele, os três numa linha (o PROVIDER)
import { PageLoader } from "@/shared/components/PageLoader";
import { formatCurrency } from "@/shared/utils/money";
import { formatChargedAbsenceCount, formatConsultationCount } from "../display/earningsLabels";
import styles from "./EarningsNumbers.module.css";

// errorMessage: a mensagem da API (ex: a data inicial depois da final, no Personalizado)
export function EarningsNumbers({ earnings, isLoading = false, errorMessage, stacked = false, className }) {
  const classes = [styles.numbers, stacked && styles.stacked, className].filter(Boolean).join(" ");

  if (errorMessage) {
    return (
      <div className={classes}>
        <p className={styles.error} role="alert">
          {errorMessage}
        </p>
      </div>
    );
  }

  if (isLoading || !earnings) {
    return (
      <div className={classes}>
        <PageLoader variant="compact" label="Carregando o rendimento do período..." className={styles.loading} />
      </div>
    );
  }

  const tiles = [
    {
      key: "payout",
      caption: "Rendimento",
      value: formatCurrency(earnings.payoutTotal),
      detail: `${formatConsultationCount(earnings.consultationCount)} realizados`,
      highlighted: true,
    },
    {
      key: "absences",
      caption: "Faltas cobradas",
      value: formatCurrency(earnings.chargedAbsencePayoutTotal),
      detail: formatChargedAbsenceCount(earnings.chargedAbsenceCount),
    },
    {
      key: "total",
      caption: "Repasse total",
      value: formatCurrency(earnings.totalPayout),
      detail: "rendimento + faltas cobradas",
      wide: true,
    },
  ];

  return (
    <div className={classes} aria-live="polite">
      {tiles.map((tile) => (
        <div key={tile.key} className={[styles.tile, tile.wide && styles.wide].filter(Boolean).join(" ")}>
          <span className={styles.caption}>{tile.caption}</span>
          <span className={[styles.value, tile.highlighted && styles.highlighted].filter(Boolean).join(" ")}>
            {tile.value}
          </span>
          <span className={styles.detail}>{tile.detail}</span>
        </div>
      ))}
    </div>
  );
}
