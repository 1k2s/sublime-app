import { formatDate } from "@/shared/utils/date";

// O rótulo de cada opção do "o que está sendo pago", com os tipos do PaymentSuggestion do backend.
// O valor e a data vêm prontos da API (paymentSuggestions da conta)
const SUGGESTION_LABELS = {
  OPEN_INSTALLMENT: ({ dueDate }) => `Parcela em aberto (${formatDate(dueDate, "dd/MM")})`,
  ALL_OPEN_INSTALLMENTS: () => "Todas as parcelas em aberto",
  BALANCE_DUE: () => "Saldo devedor",
  SINGLE_SESSION_DUE: () => "Débito em aberto",
};

export const OTHER_AMOUNT = "OTHER";

export function getSuggestionLabel(suggestion) {
  return SUGGESTION_LABELS[suggestion.type]?.(suggestion) ?? suggestion.type;
}
