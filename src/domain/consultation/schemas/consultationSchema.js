// Validação do item em edição no carrinho (paciente, técnica e horário de chegada)
import { isFuture, isValid, parseISO } from "date-fns";
import { z } from "zod";

export const consultationSchema = z.object({
  patientId: z.number({ error: "Selecione um paciente" }),
  techniqueId: z.number({ error: "Selecione a técnica" }),
  arrivalTime: z
    .string()
    .min(1, "Informe o horário de chegada")
    .refine((value) => isValid(parseISO(value)), "Horário inválido")
    .refine((value) => !isFuture(parseISO(value)), "O horário de chegada não pode estar no futuro"),
});
