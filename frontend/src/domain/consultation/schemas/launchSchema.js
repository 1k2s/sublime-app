import { z } from "zod";

// Só o formato do lançamento. As regras (contrato, preço, data por perfil, liberação) são do
// backend: ver "Consultation" no domain-model
function required(message) {
  return z.string({ error: message }).min(1, message);
}

export const launchSchema = z.object({
  patientId: required("Selecione o paciente."),
  techniqueId: required("Selecione a técnica."),
  durationMinutes: required("Informe a duração.").regex(/^\d+$/, "Duração em minutos."),
  status: required("Selecione o status."),
});

// O ADMIN lança por um prestador: o prestador também é obrigatório
export const adminLaunchSchema = launchSchema.extend({
  providerId: required("Selecione o prestador."),
});
