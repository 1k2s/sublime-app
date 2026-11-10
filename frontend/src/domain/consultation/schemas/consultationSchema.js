// Validação de formato do item em edição no carrinho. As regras (contrato, preço, ativos) são da API
import { isFuture, isValid, parseISO, startOfDay } from "date-fns";
import { z } from "zod";
import { CONSULTATION_STATUSES } from "../consultationStatus";

// Paciente e técnica chegam como o objeto escolhido na busca (nulo enquanto não escolhido)
const selected = z.object({ id: z.number(), name: z.string() }).nullable();

export const consultationSchema = z.object({
  patient: selected.refine(Boolean, "Selecione um paciente"),
  technique: selected.refine(Boolean, "Selecione a técnica"),
  occurredAt: z
    .string()
    .min(1, "Informe a data do atendimento")
    .refine((value) => isValid(parseISO(value)), "Data inválida")
    .refine((value) => !isFuture(startOfDay(parseISO(value))), "A data do atendimento não pode ser futura"),
  durationMinutes: z.coerce
    .number({ error: "Informe a duração" })
    .int("A duração deve ser em minutos inteiros")
    .min(1, "Informe a duração"),
  status: z.enum(
    CONSULTATION_STATUSES.map((status) => status.value),
    { error: "Selecione o status" },
  ),
});
