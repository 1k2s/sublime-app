// Barra do período do dashboard: Hoje, Esta semana, Este mês e Personalizado num controle
// segmentado e, ao lado, o intervalo do período como o backend devolveu. No Personalizado, as
// duas datas entram no lugar do intervalo. Os valores ficam nos cartões, não nos botões
import { Input } from "@/shared/components/Input";
import { formatDate } from "@/shared/utils/date";
import { CUSTOM_PERIOD } from "../hooks/useDashboardPeriod";
import styles from "./PeriodSelector.module.css";

const PERIOD_OPTIONS = [
  { value: "today", label: "Hoje" },
  { value: "week", label: "Esta semana" },
  { value: "month", label: "Este mês" },
  { value: CUSTOM_PERIOD, label: "Personalizado" },
];

function formatRange(from, to) {
  if (!from || !to) return "";
  return from === to ? formatDate(from) : `${formatDate(from)} a ${formatDate(to)}`;
}

// period: o retorno do useDashboardPeriod
export function PeriodSelector({ period, className }) {
  const isCustom = period.periodKey === CUSTOM_PERIOD;

  return (
    <div className={[styles.bar, className].filter(Boolean).join(" ")}>
      <div className={styles.segmented} role="group" aria-label="Período">
        {PERIOD_OPTIONS.map((option) => {
          const isActive = period.periodKey === option.value;
          return (
            <button
              key={option.value}
              type="button"
              aria-pressed={isActive}
              className={[styles.option, isActive && styles.active].filter(Boolean).join(" ")}
              onClick={() => period.selectPeriod(option.value)}
            >
              {option.label}
            </button>
          );
        })}
      </div>

      {isCustom ? (
        <div className={styles.dates}>
          <label className={styles.dateField}>
            <span className={styles.dateLabel}>De</span>
            <Input
              id="dashboard-from"
              type="date"
              className={styles.dateInput}
              value={period.custom.from}
              onChange={(event) => period.changeCustom("from", event.target.value)}
            />
          </label>
          <label className={styles.dateField}>
            <span className={styles.dateLabel}>até</span>
            <Input
              id="dashboard-to"
              type="date"
              className={styles.dateInput}
              value={period.custom.to}
              onChange={(event) => period.changeCustom("to", event.target.value)}
            />
          </label>
        </div>
      ) : (
        <span className={styles.range}>{formatRange(period.from, period.to)}</span>
      )}
    </div>
  );
}
