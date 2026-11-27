import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";

// Texto das parcelas, com os valores que a API calcula (a primeira leva os centavos que sobram):
// "3x: 1ª de R$ 1.238,36 + 2 × R$ 1.238,35", "3x de R$ 300,00" ou "À vista (1x)". Recebe a
// resposta do contrato ou da prévia ({ installments, firstInstallmentValue, installmentValue })
export function formatInstallments({ installments, firstInstallmentValue, installmentValue }) {
  if (installments === 1) return "À vista (1x)";
  if (firstInstallmentValue === installmentValue) {
    return `${installments}x de ${formatCurrency(installmentValue)}`;
  }
  return `${installments}x: 1ª de ${formatCurrency(firstInstallmentValue)} + ${installments - 1} × ${formatCurrency(installmentValue)}`;
}

// Os vencimentos do cronograma da API (installmentSchedule): até 3, todos ("02/11, 02/12 e
// 02/01"); com mais, o primeiro e o último ("1ª 02/11/2026 · última 02/10/2027"). O cronograma
// inteiro fica no detalhe do contrato e no Financeiro
export function formatInstallmentDates(schedule = []) {
  if (schedule.length === 0) return "";
  if (schedule.length > 3) {
    return `1ª ${formatDate(schedule[0].dueDate)} · última ${formatDate(schedule.at(-1).dueDate)}`;
  }
  const dates = schedule.map((installment) => formatDate(installment.dueDate, "dd/MM"));
  return dates.length === 1 ? dates[0] : `${dates.slice(0, -1).join(", ")} e ${dates.at(-1)}`;
}

// As parcelas com os vencimentos: "3x de R$ 668,00 · 02/11, 02/12 e 02/01"
export function formatInstallmentsWithDates(data) {
  return [formatInstallments(data), formatInstallmentDates(data.installmentSchedule)].filter(Boolean).join(" · ");
}

// As parcelas do cadastro, da prévia: com o crédito de contratos anteriores aplicado, ele antes
// ("crédito aplicado R$ 334,00 · 3x de R$ 556,67 · 02/11, 02/12 e 02/01"); com o crédito cobrindo
// o total, nada a parcelar
export function formatCreatedInstallments(preview) {
  const credit = Number(preview.creditApplied) > 0 ? `crédito aplicado ${formatCurrency(preview.creditApplied)}` : null;
  if (credit && Number(preview.installmentBase) === 0) return "O crédito cobre o total: nada a parcelar";
  return [credit, formatInstallmentsWithDates(preview)].filter(Boolean).join(" · ");
}

// As parcelas do aditivo, da prévia: o já pago (descontado do total) e as novas parcelas com os
// vencimentos ("já pago R$ 668,00 · 2x de R$ 1.460,00 · 02/12 e 02/01"). Com o valor parcelado
// zero, o pago cobre o total
export function formatAmendedInstallments(preview) {
  if (Number(preview.installmentBase) === 0) return "O pago cobre o total: nada a parcelar";
  const paid = Number(preview.paidSoFar) > 0 ? `já pago ${formatCurrency(preview.paidSoFar)}` : null;
  return [paid, formatInstallmentsWithDates(preview)].filter(Boolean).join(" · ");
}
