// Monta os pedidos de preço (um por valor alterado) a partir do formulário do plano
import Decimal from "decimal.js";
import { formatAxisTitle } from "../display/priceAxis";
import { findPriceByAxis } from "./planAxes";

// Nome do campo do formulário de um eixo ("axis30"): chave numérica viraria índice de array no
// React Hook Form
export function axisFieldName(axis) {
  return `axis${axis}`;
}

function toApiValue(text) {
  return text.trim().replace(",", ".");
}

// Só os eixos preenchidos com valor diferente do vigente: reenviar o mesmo valor criaria um
// reajuste à toa no histórico. Cada pedido vira um POST /prices/duration ou /prices/frequency
export function buildPlanPriceChanges({ plan, axes, pricingModel, pricingGroupId, values }) {
  return axes.flatMap((axis) => {
    const text = values[axisFieldName(axis)] ?? "";
    if (text.trim() === "") return [];
    const current = findPriceByAxis(plan, axis);
    const sessionValue = toApiValue(text);
    if (current && new Decimal(sessionValue).equals(current.sessionValue)) return [];

    return [
      {
        label: formatAxisTitle(axis, pricingModel),
        payload: {
          pricingModel,
          pricingGroupId,
          planId: plan.planId,
          [pricingModel === "DURATION_BASED" ? "durationMinutes" : "weeklyFrequency"]: axis,
          sessionValue,
        },
      },
    ];
  });
}
