// O `option` do ECharts do anel da composição do repasse: um trilho claro por trás e, por cima,
// duas fatias em degradê, os atendimentos realizados (o rendimento) e as faltas cobradas. Só
// desenha os números do `earnings` (o backend soma). O centro e a legenda são HTML, no
// componente. As cores chegam prontas: o canvas não lê `var()`
export const PAYOUT_SERIES_INDEX = 1;

function gradient(from, to) {
  return {
    type: "linear",
    x: 0,
    y: 0,
    x2: 1,
    y2: 1,
    colorStops: [
      { offset: 0, color: from },
      { offset: 1, color: to },
    ],
  };
}

// colors: { attended, attendedStrong, chargedAbsence, chargedAbsenceStrong, track }, das variáveis CSS
export function buildPayoutCompositionOption(earnings, colors) {
  return {
    animationDuration: 1100,
    animationEasing: "cubicOut",
    animationDurationUpdate: 700,
    animationEasingUpdate: "cubicInOut",
    series: [
      {
        // o trilho: um anel cheio, sem interação
        type: "pie",
        radius: ["66%", "88%"],
        silent: true,
        animation: false,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { color: colors.track },
        data: [{ value: 1 }],
      },
      {
        type: "pie",
        radius: ["70%", "84%"],
        padAngle: 4,
        label: { show: false },
        labelLine: { show: false },
        itemStyle: { borderRadius: 12 },
        emphasis: {
          focus: "self",
          scale: true,
          scaleSize: 6,
          itemStyle: { shadowBlur: 24, shadowColor: "rgba(31, 41, 55, 0.28)" },
        },
        blur: { itemStyle: { opacity: 0.35 } },
        data: [
          {
            name: "Atendidos",
            value: earnings.payoutTotal,
            itemStyle: { color: gradient(colors.attended, colors.attendedStrong) },
          },
          {
            name: "Faltas cobradas",
            value: earnings.chargedAbsencePayoutTotal,
            itemStyle: { color: gradient(colors.chargedAbsence, colors.chargedAbsenceStrong) },
          },
        ],
      },
    ],
  };
}
