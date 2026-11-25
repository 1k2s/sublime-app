// Validação dos formulários de preço. O valor é digitado como texto ("95,00") e segue como string
import { z } from "zod";

// Valor da sessão: obrigatório, até duas casas, maior que zero (só formato: quem valida é a API)
const sessionValueText = z
  .string()
  .trim()
  .min(1, "Informe o valor.")
  .regex(/^\d+([.,]\d{1,2})?$/, "Informe um valor com no máximo duas casas decimais.")
  .refine((value) => Number(value.replace(",", ".")) > 0, "O valor deve ser maior que zero.");

export const priceSchema = z.object({
  pricingGroupId: z.string().min(1, "Selecione o grupo."),
  planId: z.string().min(1, "Selecione o plano."),
  axis: z.string().regex(/^[1-9]\d*$/, "Informe um número inteiro maior que zero."),
  sessionValue: sessionValueText,
});

// Valores de um plano, um campo por eixo (fields: [{ name, required }]). Eixo com preço vigente é
// obrigatório (a API não remove um preço, só o substitui); eixo sem preço pode ficar vazio
export function buildPlanPricesSchema(fields) {
  return z.object(
    Object.fromEntries(
      fields.map(({ name, required }) => [name, required ? sessionValueText : z.literal("").or(sessionValueText)])
    )
  );
}
