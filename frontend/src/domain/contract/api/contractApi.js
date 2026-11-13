import { apiClient } from "@/shared/http/apiClient";

// GET /api/contracts — todos os contratos (ativos e inativos), com os itens e o current
export async function listContracts() {
  const { data } = await apiClient.get("/contracts");
  return data;
}

// POST /api/contracts — no formato do ContractRequestDTO: { patientId, beneficiaryId, planId,
// startDate, paymentMethod, items: [{ techniqueId, sessionDurationPriceId + frequency |
// sessionFrequencyPriceId }] }. O fim é calculado pelo backend
export async function createContract(payload) {
  const { data } = await apiClient.post("/contracts", payload);
  return data;
}

// DELETE /api/contracts/{id} — inativa o contrato (sem reativação)
export async function deactivateContract(id) {
  await apiClient.delete(`/contracts/${id}`);
}

// POST /api/contracts/preview — sem gravar e com as contas do cadastro: fim, opções do plano do
// Grupo, valor de cada item, total e parcelas. { planId, startDate, items (os já escolhidos, no
// formato do cadastro), installments (opcional: vazio = o máximo do contrato) }
export async function getContractPreview(payload) {
  const { data } = await apiClient.post("/contracts/preview", payload);
  return data;
}
