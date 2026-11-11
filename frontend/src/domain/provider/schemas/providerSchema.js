import { z } from "zod";

// Só o formato do Percentage da API: 0 a 100, com até 2 casas decimais (aceita vírgula).
// A regra "indicação nunca menor que o padrão" é do backend (ver Provider, no domain-model)
const PERCENTAGE_PATTERN = /^\d{1,3}([.,]\d{1,2})?$/;

function isValidPercentage(value) {
  return PERCENTAGE_PATTERN.test(value) && Number(value.replace(",", ".")) <= 100;
}

const percentage = z
  .string()
  .trim()
  .min(1, "Informe o percentual")
  .refine(isValidPercentage, "Informe um percentual de 0 a 100, com até 2 casas decimais");

const name = z
  .string()
  .trim()
  .min(3, "O nome deve ter pelo menos 3 caracteres")
  .max(100, "O nome deve ter no máximo 100 caracteres");

// No cadastro, percentual vazio = padrão da clínica (aplicado pelo backend)
export const createProviderSchema = z.object({
  userId: z.string().min(1, "Selecione o usuário"),
  name,
  commissionPercentage: z.union([z.literal(""), percentage]),
  referralCommissionPercentage: z.union([z.literal(""), percentage]),
});

// Na alteração o usuário é fixo e os dois percentuais são obrigatórios
export const updateProviderSchema = z.object({
  name,
  commissionPercentage: percentage,
  referralCommissionPercentage: percentage,
});
