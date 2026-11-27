import { apiClient } from "@/shared/http/apiClient";

// GET /api/contracts — um contrato por linha: a versão atual de cada um (inclui a inativada), com
// os itens, o current e o amendmentNumber. As versões anteriores vêm do listContractVersions
export async function listContracts() {
  const { data } = await apiClient.get("/contracts");
  return data;
}

// POST /api/contracts — no formato do ContractRequestDTO: { patientId, beneficiaryId, planId,
// startDate, paymentMethod, installments, firstDueDate (opcional: vazio = o início), items:
// [{ techniqueId, sessionDurationPriceId + frequency | sessionFrequencyPriceId }] }. O fim e o
// cronograma das parcelas são calculados pelo backend
export async function createContract(payload) {
  const { data } = await apiClient.post("/contracts", payload);
  return data;
}

// DELETE /api/contracts/{id} — inativa o contrato (sem reativação)
export async function deactivateContract(id) {
  await apiClient.delete(`/contracts/${id}`);
}

// POST /api/contracts/preview — sem gravar e com as contas do cadastro: fim, opções do plano do
// Grupo, valor de cada item, total, parcelas e o cronograma. { planId, startDate, items (os já
// escolhidos, no formato do cadastro), installments (opcional: vazio = o máximo do contrato),
// firstDueDate (opcional: vazio = o início) }
export async function getContractPreview(payload) {
  const { data } = await apiClient.post("/contracts/preview", payload);
  return data;
}

// GET /api/contracts/{id}/versions — a versão e as anteriores, da mais nova para a mais antiga,
// completas (com os itens e o superseded): o histórico do detalhe
export async function listContractVersions(id) {
  const { data } = await apiClient.get(`/contracts/${id}/versions`);
  return data;
}

// POST /api/contracts/{id}/amendments — gera a nova versão (a atual é inativada). No formato do
// ContractAmendmentRequestDTO, o estado completo: { beneficiaryId, paymentMethod, installments,
// endDate (opcional, prorrogação), firstDueDate (opcional: vazio = o sugerido), items (item que
// não vier é removido) }
export async function amendContract({ id, payload }) {
  const { data } = await apiClient.post(`/contracts/${id}/amendments`, payload);
  return data;
}

// POST /api/contracts/{id}/amendments/preview — sem gravar e com as contas do aditivo: o novo fim,
// as opções do plano do Grupo, os valores, o total, o já pago, o valor parcelado, as parcelas com
// o cronograma e o 1º vencimento sugerido, pela nova vigência. { endDate, items, installments,
// firstDueDate }, todos opcionais (parcelas vazias = o máximo; vencimento vazio = o sugerido)
export async function getAmendmentPreview({ id, payload }) {
  const { data } = await apiClient.post(`/contracts/${id}/amendments/preview`, payload);
  return data;
}

// GET /api/consultations/session-balance?contractId — saldo de sessões de cada item por duração,
// somando as versões do contrato. É do módulo consultation (conta os atendimentos): o contract só
// LÊ o dado publicado pelo endpoint, sem importar o módulo nem repetir a conta
export async function getSessionBalance(contractId) {
  const { data } = await apiClient.get("/consultations/session-balance", { params: { contractId } });
  return data;
}
