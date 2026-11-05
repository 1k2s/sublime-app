// Validação do formulário de preço. O valor é digitado como texto ("95,00") e segue como string
import { z } from "zod";

export const priceSchema = z.object({
  pricingGroupId: z.string().min(1, "Selecione o grupo."),
  planId: z.string().min(1, "Selecione o plano."),
  axis: z.string().regex(/^[1-9]\d*$/, "Informe um número inteiro maior que zero."),
  sessionValue: z
    .string()
    .regex(/^\d+([.,]\d{1,2})?$/, "Informe um valor com no máximo duas casas decimais.")
    .refine((value) => Number(value.replace(",", ".")) > 0, "O valor deve ser maior que zero."),
});
