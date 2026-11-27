import { z } from "zod";

// Só o formato. As regras do pagamento (valor maior que zero, líquido até o bruto, parcelado só no
// crédito, data não futura, contrato do paciente) são do backend: ver "Payment" no domain-model.
// Os valores são digitados como texto ("1670,00") e seguem como string

const MONEY_PATTERN = /^\d+([.,]\d{1,2})?$/;
const moneyText = (message) =>
  z.string().trim().min(1, message).regex(MONEY_PATTERN, "Informe um valor com no máximo duas casas decimais.");

export const paymentSchema = z.object({
  // "" = sem contrato (avulso)
  contractId: z.string(),
  // a opção do "o que está sendo pago" (o tipo da sugestão, ou "OTHER")
  suggestion: z.string(),
  paymentMethod: z.string().min(1, "Selecione a forma de pagamento."),
  // só no crédito (de 1 a 12)
  cardInstallments: z.string(),
  amount: moneyText("Informe o valor pago."),
  netAmount: moneyText("Informe o valor recebido."),
  paidAt: z.string().min(1, "Informe a data do pagamento."),
});
