// Formas de pagamento do contrato, com os valores do enum PaymentMethod do backend

// Rótulo de cada uma, para exibir (inclui o boleto: um contrato pode vir da API com ele)
const PAYMENT_METHOD_LABELS = {
  PIX: "Pix",
  CREDIT_CARD: "Cartão de crédito",
  DEBIT_CARD: "Cartão de débito",
  CASH: "Dinheiro",
  BANK_SLIP: "Boleto",
};

// As que o formulário oferece: o boleto fica de fora por enquanto (em aberto)
export const PAYMENT_METHOD_OPTIONS = ["PIX", "CREDIT_CARD", "DEBIT_CARD", "CASH"].map((value) => ({
  value,
  label: PAYMENT_METHOD_LABELS[value],
}));

export function getPaymentMethodLabel(value) {
  return PAYMENT_METHOD_LABELS[value] ?? value ?? "--";
}
