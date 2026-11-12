import { z } from "zod";

// Só o formato. As regras do contrato (plano × itens, prazo máximo, titular ≠ beneficiário,
// um contrato vigente por paciente) são do backend: ver "Contract e ContractItem" no
// domain-model. O fim não é campo: é calculado (ADR 0009)

const selectedPatient = z.object({ id: z.union([z.string(), z.number()]), name: z.string() }).nullable();

// Campos que dependem de outro select chegam undefined enquanto estão desabilitados: o error cobre esse caso
function requiredSelect(message) {
  return z.string({ error: message }).min(1, message);
}

// O item guarda o que o usuário escolhe (duração ou frequência semanal), não a linha de preço: a
// linha sai do plano do contrato (resolveItemLine), e trocar o plano não apaga as escolhas.
// pricingModel é da tela: o modelo do grupo da técnica diz quais campos o item tem
const contractItem = z
  .object({
    techniqueId: requiredSelect("Selecione a técnica."),
    pricingModel: z.string().optional(),
    durationMinutes: z.string().optional(),
    frequency: z.string().optional(),
    groupPlanId: z.string().optional(),
    weeklyFrequency: z.string().optional(),
  })
  .superRefine((item, ctx) => {
    if (item.pricingModel === "DURATION_BASED") {
      if (!item.durationMinutes) {
        ctx.addIssue({ code: "custom", path: ["durationMinutes"], message: "Selecione a duração." });
      }
      if (!item.frequency) {
        ctx.addIssue({ code: "custom", path: ["frequency"], message: "Selecione a frequência." });
      }
    }
    if (item.pricingModel === "FREQUENCY_BASED" && !item.weeklyFrequency) {
      ctx.addIssue({ code: "custom", path: ["weeklyFrequency"], message: "Selecione a frequência semanal." });
    }
  });

export const contractSchema = z
  .object({
    patient: selectedPatient.refine(Boolean, "Selecione o paciente titular."),
    beneficiary: selectedPatient,
    planId: z.string().min(1, "Selecione o plano."),
    // Tipo do plano escolhido (da tela): no pacote, o item do Grupo escolhe o plano do Grupo
    planType: z.string().optional(),
    startDate: z.string().min(1, "Informe a data de início."),
    paymentMethod: requiredSelect("Selecione a forma de pagamento."),
    // Vazio = o máximo do contrato (o backend calcula: os meses do contrato, até 12)
    installments: z.string().optional(),
    items: z.array(contractItem).min(1, "Inclua pelo menos um item."),
  })
  .superRefine((values, ctx) => {
    if (values.planType !== "PACKAGE") return;
    values.items.forEach((item, index) => {
      if (item.pricingModel === "FREQUENCY_BASED" && !item.groupPlanId) {
        ctx.addIssue({
          code: "custom",
          path: ["items", index, "groupPlanId"],
          message: "Selecione o plano do Grupo.",
        });
      }
    });
  });
