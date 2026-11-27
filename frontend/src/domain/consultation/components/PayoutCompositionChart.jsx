// Panorama: o anel da composição do repasse do período (os atendimentos realizados e as faltas
// cobradas), com a legenda dos valores ao lado, em texto. Carregado só na primeira visita ao
// Panorama (React.lazy na page): o ECharts não entra no bundle do Resumo.
// Efeitos: o anel se desenha ao abrir e o total do centro sobe do zero; o hover numa fatia ou
// numa linha da legenda destaca a fatia, esmaece a outra e mostra o valor dela no centro.
// Os números vêm prontos do earnings; período sem repasse mostra a mensagem no lugar do anel
import { useMemo, useRef, useState } from "react";
import { Card } from "@/shared/components/Card";
import { Chart } from "@/shared/components/Chart";
import { PageLoader } from "@/shared/components/PageLoader";
import { formatCurrency } from "@/shared/utils/money";
import { formatChargedAbsenceCount, formatConsultationCount } from "../display/earningsLabels";
import { buildPayoutCompositionOption, PAYOUT_SERIES_INDEX } from "../display/payoutCompositionOption";
import { useAnimatedNumber } from "../hooks/useAnimatedNumber";
import styles from "./PayoutCompositionChart.module.css";

// As cores da paleta (index.css), lidas na hora: o canvas do ECharts não entende var()
function readChartColors() {
  const root = getComputedStyle(document.documentElement);
  const read = (name) => root.getPropertyValue(name).trim();
  return {
    attended: read("--color-primary"),
    attendedStrong: read("--color-primary-strong"),
    chargedAbsence: read("--color-sand"),
    chargedAbsenceStrong: read("--color-sand-strong"),
    track: read("--color-row-line"),
  };
}

function hasPayout(earnings) {
  return Number(earnings.totalPayout) !== 0;
}

// A parte de cada fatia no total, só para a largura da barra da legenda (nenhum valor exibido)
function shareOf(value, total) {
  return Number(total) > 0 ? Math.min(100, (Number(value) / Number(total)) * 100) : 0;
}

function buildSlices(earnings) {
  return [
    {
      name: "Atendidos",
      value: earnings.payoutTotal,
      detail: formatConsultationCount(earnings.consultationCount),
      tone: "attended",
    },
    {
      name: "Faltas cobradas",
      value: earnings.chargedAbsencePayoutTotal,
      detail: formatChargedAbsenceCount(earnings.chargedAbsenceCount),
      tone: "chargedAbsence",
    },
  ];
}

export function PayoutCompositionChart({ earnings, isLoading = false, errorMessage, className }) {
  const chartRef = useRef(null);
  const [hoveredIndex, setHoveredIndex] = useState(null);
  const animatedTotal = useAnimatedNumber(earnings?.totalPayout);

  const option = useMemo(
    () => (earnings ? buildPayoutCompositionOption(earnings, readChartColors()) : null),
    [earnings]
  );
  const chartEvents = useMemo(
    () => ({
      mouseover: (params) => params.seriesIndex === PAYOUT_SERIES_INDEX && setHoveredIndex(params.dataIndex),
      mouseout: (params) => params.seriesIndex === PAYOUT_SERIES_INDEX && setHoveredIndex(null),
    }),
    []
  );

  // O hover na legenda destaca a fatia no anel, como o hover no próprio anel
  function highlight(index) {
    setHoveredIndex(index);
    chartRef.current?.dispatchAction({ type: "highlight", seriesIndex: PAYOUT_SERIES_INDEX, dataIndex: index });
  }

  function downplay(index) {
    setHoveredIndex(null);
    chartRef.current?.dispatchAction({ type: "downplay", seriesIndex: PAYOUT_SERIES_INDEX, dataIndex: index });
  }

  function renderCenter(slices) {
    const hovered = hoveredIndex === null ? null : slices[hoveredIndex];
    if (!hovered) {
      return (
        <div className={styles.center}>
          <span className={styles.centerValue}>{formatCurrency(animatedTotal)}</span>
          <span className={styles.centerLabel}>Repasse total</span>
        </div>
      );
    }
    return (
      <div className={styles.center}>
        <span className={`${styles.centerValue} ${styles[`${hovered.tone}Text`]}`}>{formatCurrency(hovered.value)}</span>
        <span className={styles.centerLabel}>{hovered.name}</span>
        <span className={styles.centerDetail}>{hovered.detail}</span>
      </div>
    );
  }

  function renderContent() {
    if (errorMessage) {
      return (
        <p className={styles.error} role="alert">
          {errorMessage}
        </p>
      );
    }
    if (isLoading || !earnings) {
      return <PageLoader variant="section" label="Carregando a composição do repasse..." />;
    }
    if (!hasPayout(earnings)) {
      return <p className={styles.empty}>Nenhum repasse no período.</p>;
    }

    const slices = buildSlices(earnings);
    return (
      <div className={styles.body}>
        <div className={styles.chartBox}>
          <Chart
            ref={chartRef}
            option={option}
            onEvents={chartEvents}
            label={`Composição do repasse: ${formatCurrency(earnings.payoutTotal)} dos atendidos e ${formatCurrency(
              earnings.chargedAbsencePayoutTotal
            )} das faltas cobradas`}
            className={styles.chart}
          />
          {renderCenter(slices)}
        </div>

        <ul className={styles.legend}>
          {slices.map((slice, index) => (
            <li
              key={slice.name}
              className={[
                styles.item,
                hoveredIndex === index && styles.hot,
                hoveredIndex !== null && hoveredIndex !== index && styles.dim,
              ]
                .filter(Boolean)
                .join(" ")}
              onMouseEnter={() => highlight(index)}
              onMouseLeave={() => downplay(index)}
            >
              <span className={`${styles.swatch} ${styles[slice.tone]}`} aria-hidden="true" />
              <span className={styles.name}>{slice.name}</span>
              <span className={styles.value}>{formatCurrency(slice.value)}</span>
              <span className={styles.count}>{slice.detail}</span>
              <span className={styles.track} aria-hidden="true">
                <span
                  className={`${styles.fill} ${styles[slice.tone]}`}
                  style={{ width: `${shareOf(slice.value, earnings.totalPayout)}%` }}
                />
              </span>
            </li>
          ))}
          <li className={styles.total}>
            <span className={styles.totalLabel}>Repasse total</span>
            <span className={styles.totalValue}>{formatCurrency(earnings.totalPayout)}</span>
          </li>
        </ul>
      </div>
    );
  }

  return (
    <Card title="Composição do repasse" className={[styles.card, className].filter(Boolean).join(" ")}>
      {renderContent()}
    </Card>
  );
}
