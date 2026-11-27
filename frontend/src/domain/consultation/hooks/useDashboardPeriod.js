// O período do dashboard: Hoje, Esta semana e Este mês têm os limites definidos pelo backend
// (o `from`/`to` de cada um no `earnings-summary`, no fuso da clínica), e o front não calcula
// datas; no Personalizado, o usuário digita os dois. O padrão é o mês de hoje
import { useState } from "react";

export const DEFAULT_PERIOD = "month";
export const CUSTOM_PERIOD = "custom";

// summary: a resposta do earnings-summary (today, week e month, cada um com from e to)
export function useDashboardPeriod(summary) {
  const [periodKey, setPeriodKey] = useState(DEFAULT_PERIOD);
  const [custom, setCustom] = useState({ from: "", to: "" });

  // O Personalizado abre com o período que estava escolhido, para o usuário só ajustar
  function selectPeriod(key) {
    if (key === CUSTOM_PERIOD && !custom.from && !custom.to && summary?.[periodKey]) {
      setCustom({ from: summary[periodKey].from, to: summary[periodKey].to });
    }
    setPeriodKey(key);
  }

  function changeCustom(field, value) {
    setCustom((current) => ({ ...current, [field]: value }));
  }

  const range = periodKey === CUSTOM_PERIOD ? custom : summary?.[periodKey];

  return {
    periodKey,
    selectPeriod,
    custom,
    changeCustom,
    from: range?.from || undefined,
    to: range?.to || undefined,
  };
}
