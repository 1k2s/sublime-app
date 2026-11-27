import { apiClient } from "@/shared/http/apiClient";

// A conta dos pacientes, calculada pelo backend na leitura (ver "Payment" no domain-model)

// GET /api/billing/accounts — uma linha por paciente com conta, já na ordem da lista (os
// atrasados, os devendo e os outros por nome): saldo, situação, próximo vencimento e avisos
export async function listAccounts() {
  const { data } = await apiClient.get("/billing/accounts");
  return data;
}

// GET /api/billing/patients/{id}/account — cada contrato na conta (o vigente primeiro) e o
// avulso (singleSession, nulo sem avulso)
export async function getPatientAccount(patientId) {
  const { data } = await apiClient.get(`/billing/patients/${patientId}/account`);
  return data;
}

// GET /api/billing/patients/{id}/transfers — o crédito de contratos anteriores aplicado nos
// contratos do titular (as transferências de saldo), da mais recente para a mais antiga
export async function listPatientTransfers(patientId) {
  const { data } = await apiClient.get(`/billing/patients/${patientId}/transfers`);
  return data;
}

// GET /api/billing/contracts/{id}/installments — as parcelas de cada versão, da mais nova para a
// mais antiga, com a situação de cada uma
export async function listContractInstallments(contractId) {
  const { data } = await apiClient.get(`/billing/contracts/${contractId}/installments`);
  return data;
}

// GET /api/billing/contracts/{id}/sessions — os atendimentos ativos do contrato, numerados
// ("7ª de 6"), com a etiqueta (tag) e o hasBeneficiary
export async function getContractSessions(contractId) {
  const { data } = await apiClient.get(`/billing/contracts/${contractId}/sessions`);
  return data;
}
