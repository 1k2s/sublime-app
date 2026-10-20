// Mock até o backend expor /consultations. Contrato vigente por paciente; o paciente 3 fica sem contrato
// para exercitar o caminho de erro
const MOCK_CONTRACTS = {
  1: { id: 10, planName: "Pilates 2x por semana", baseValue: "150.00", commissionPercentage: "40.00", providerPayoutValue: "60.00" },
  2: { id: 11, planName: "RPG sessão avulsa", baseValue: "200.00", commissionPercentage: "35.00", providerPayoutValue: "70.00" },
  4: { id: 12, planName: "Fisioterapia ortopédica 1x por semana", baseValue: "120.00", commissionPercentage: "50.00", providerPayoutValue: "60.00" },
  5: { id: 13, planName: "Pilates em grupo 3x por semana", baseValue: "95.50", commissionPercentage: "45.00", providerPayoutValue: "42.98" },
};

// Lançamentos gravados pelo mock. Somem ao recarregar a página
const mockConsultations = [];

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// Depois: httpClient.post("/consultations/preview", { patientId, techniqueId, arrivalTime })
export async function previewConsultation({ patientId, arrivalTime }) {
  await delay(300);

  const contract = MOCK_CONTRACTS[patientId];
  if (!contract) {
    throw new Error("Paciente sem contrato ativo. Procure a administração.");
  }

  const { baseValue, commissionPercentage, providerPayoutValue, ...contractInfo } = contract;
  return { arrivalTime, contract: contractInfo, baseValue, commissionPercentage, providerPayoutValue };
}

// Grava todos os itens do carrinho de uma vez
// Depois: httpClient.post("/consultations/batch", payloads) (endpoint a combinar com o backend)
export async function createConsultations(payloads) {
  await delay(500);

  const created = payloads.map((payload, index) => ({
    id: Date.now() + index,
    ...payload,
    providerPayoutValue: MOCK_CONTRACTS[payload.patientId]?.providerPayoutValue ?? "0.00",
  }));
  mockConsultations.push(...created);
  return created;
}

// Lançamentos do prestador logado em uma data ("2026-10-16")
// Depois: httpClient.get("/consultations", { params: { date } })
export async function listConsultations({ date }) {
  await delay(300);
  return mockConsultations.filter((consultation) => consultation.arrivalTime.startsWith(date));
}
