import { apiClient } from "@/shared/http/apiClient";

// Pagamentos dos pacientes, de valor livre (ver "Payment" no domain-model)

// GET /api/payments?patientId — os pagamentos do paciente, ativos e inativos, do mais recente
// para o mais antigo, com o plano e o aditivo do contrato (contrato nulo = avulso)
export async function listPayments(patientId) {
  const { data } = await apiClient.get("/payments", { params: { patientId } });
  return data;
}

// POST /api/payments/preview — sem gravar: a taxa (valor e %) e a previsão de recebimento.
// { amount, netAmount, paymentMethod, cardInstallments, paidAt }
export async function getPaymentPreview(payload) {
  const { data } = await apiClient.post("/payments/preview", payload);
  return data;
}

// POST /api/payments — no formato do PaymentRequestDTO: { patientId, contractId (nulo = avulso),
// amount, netAmount, cardInstallments, paidAt, paymentMethod }
export async function registerPayment(payload) {
  const { data } = await apiClient.post("/payments", payload);
  return data;
}

// DELETE /api/payments/{id} — inativa o pagamento (corrige um registro errado)
export async function deactivatePayment(id) {
  await apiClient.delete(`/payments/${id}`);
}
