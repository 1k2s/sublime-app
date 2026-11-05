import { z } from "zod";

const selectedPatient = z.object({ id: z.union([z.string(), z.number()]), name: z.string() }).nullable();

// Campos que dependem de outro select chegam undefined enquanto estão desabilitados: o error cobre esse caso
function requiredSelect(message) {
  return z.string({ error: message }).min(1, message);
}

export const contractSchema = z
  .object({
    patient: selectedPatient.refine(Boolean, "Selecione o paciente titular."),
    beneficiary: selectedPatient,
    techniqueId: requiredSelect("Selecione a técnica."),
    planId: requiredSelect("Selecione o plano."),
    priceId: requiredSelect("Selecione uma opção."),
    paymentMethod: requiredSelect("Selecione a forma de pagamento."),
    validFrom: z.string().min(1, "Informe a data de início."),
    validTo: z.string(),
  })
  .refine((values) => !values.beneficiary || String(values.beneficiary.id) !== String(values.patient?.id), {
    message: "O beneficiário deve ser outro paciente.",
    path: ["beneficiary"],
  })
  .refine((values) => !values.validTo || values.validTo >= values.validFrom, {
    message: "O término deve ser depois do início.",
    path: ["validTo"],
  });
