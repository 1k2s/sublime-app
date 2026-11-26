// Valores iniciais do formulário do aditivo: a versão atual do contrato, nos campos do cadastro.
// Função pura. contract: a versão atual, com patientName e beneficiaryName (withPatientNames).
// planType: o tipo do plano do contrato (do catálogo), que o schema usa no item do Grupo
export function toAmendmentFormValues(contract, planType) {
  return {
    patient: { id: contract.patientId, name: contract.patientName ?? "" },
    beneficiary: contract.beneficiaryId ? { id: contract.beneficiaryId, name: contract.beneficiaryName ?? "" } : null,
    planId: String(contract.planId),
    planType: planType ?? "",
    startDate: contract.startDate,
    paymentMethod: contract.paymentMethod,
    installments: String(contract.installments),
    // Prorrogação: vazio = mantém o fim (ou o recalculado por um item novo)
    endDate: "",
    items: contract.items.map(toItemValues),
  };
}

// O item nas escolhas do formulário (a linha de preço sai delas, pelo resolveAmendedItemLine)
function toItemValues(item) {
  const isDuration = item.sessionDurationPriceId != null;
  return {
    techniqueId: String(item.techniqueId),
    pricingModel: isDuration ? "DURATION_BASED" : "FREQUENCY_BASED",
    durationMinutes: isDuration ? String(item.durationMinutes) : "",
    frequency: item.frequency ?? "",
    groupPlanId: isDuration ? "" : String(item.planId),
    weeklyFrequency: isDuration ? "" : String(item.weeklyFrequency),
  };
}
