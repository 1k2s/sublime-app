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
