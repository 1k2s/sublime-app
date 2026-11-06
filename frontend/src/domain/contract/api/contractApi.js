// Mock no localStorage até o backend expor /contracts. Para integrar, trocar cada função por uma chamada ao apiClient
// import { apiClient } from "@/shared/http/apiClient";

const STORAGE_KEY = "sublime.contracts";

const INITIAL_CONTRACTS = [
  {
    id: "1",
    patientId: "1",
    patientName: "Maria Eduarda Santos",
    beneficiaryId: null,
    beneficiaryName: null,
    techniqueName: "RPG",
    planName: "Mensal",
    weeklyFrequency: 1,
    durationMinutes: null,
    sessionValue: "95.00",
    paymentMethod: "CREDIT_CARD",
    validFrom: "2026-01-05",
    validTo: "2027-01-05",
    status: "ACTIVE",
    groupPlanPriceId: null,
    groupPlanFrequencyPriceId: "1",
  },
  {
    id: "2",
    patientId: "2",
    patientName: "João Pedro Almeida",
    beneficiaryId: null,
    beneficiaryName: null,
    techniqueName: "Pilates individual",
    planName: "Trimestral",
    weeklyFrequency: 2,
    durationMinutes: null,
    sessionValue: "80.00",
    paymentMethod: "PIX",
    validFrom: "2026-03-10",
    validTo: "2026-12-10",
    status: "ACTIVE",
    groupPlanPriceId: null,
    groupPlanFrequencyPriceId: "2",
  },
  {
    id: "3",
    patientId: "3",
    patientName: "Fernanda Costa Lima",
    beneficiaryId: null,
    beneficiaryName: null,
    techniqueName: "Massoterapia",
    planName: "Avulso",
    weeklyFrequency: null,
    durationMinutes: 60,
    sessionValue: "120.00",
    paymentMethod: "DEBIT_CARD",
    validFrom: "2026-07-15",
    validTo: null,
    status: "ACTIVE",
    groupPlanPriceId: "3",
    groupPlanFrequencyPriceId: null,
  },
  {
    id: "4",
    patientId: "4",
    patientName: "Roberto Carlos Silva",
    beneficiaryId: null,
    beneficiaryName: null,
    techniqueName: "Quiropraxia",
    planName: "Mensal",
    weeklyFrequency: 1,
    durationMinutes: null,
    sessionValue: "110.00",
    paymentMethod: "CASH",
    validFrom: "2025-06-05",
    validTo: "2026-06-05",
    status: "CLOSED",
    groupPlanPriceId: null,
    groupPlanFrequencyPriceId: "4",
  },
];

function getStoredContracts() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    return INITIAL_CONTRACTS;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_CONTRACTS));
  return INITIAL_CONTRACTS;
}

function saveStoredContracts(contracts) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(contracts));
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function listContracts() {
  await delay(200);
  return getStoredContracts();
}

export async function createContract(payload) {
  await delay(300);
  const contract = { ...payload, id: String(Date.now()), status: "ACTIVE" };
  saveStoredContracts([contract, ...getStoredContracts()]);
  return contract;
}

export async function updateContract(id, payload) {
  await delay(300);
  const contracts = getStoredContracts();
  const index = contracts.findIndex((contract) => contract.id === String(id));
  if (index === -1) throw new Error("Contrato não encontrado");

  contracts[index] = { ...contracts[index], ...payload, id: String(id) };
  saveStoredContracts(contracts);
  return contracts[index];
}
