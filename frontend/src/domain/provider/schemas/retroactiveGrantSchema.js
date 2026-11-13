import { z } from "zod";

// Só o formato da quantidade; as regras da liberação (prestador ativo, vale até o fim do dia,
// substitui a anterior) são do backend: ver "Liberação de lançamento retroativo" em Provider,
// no domain-model
export const allowedCountSchema = z.coerce
  .number({ error: "Informe a quantidade." })
  .int("A quantidade deve ser um número inteiro.")
  .min(1, "A quantidade deve ser de 1 a 50.")
  .max(50, "A quantidade deve ser de 1 a 50.");
