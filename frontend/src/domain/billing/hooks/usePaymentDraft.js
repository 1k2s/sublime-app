import { useWatch } from "react-hook-form";
import { toApiDecimal, toMoneyInputValue } from "@/shared/utils/money";
import { toDateInputValue } from "@/shared/utils/date";
import { usePaymentPreview } from "../api/usePayments";
import { formatContractLabelWithStatus } from "../display/accountLabels";
import { OTHER_AMOUNT } from "../display/paymentSuggestions";
import { isClosedContract } from "../utils/closedContracts";

const MONEY_PATTERN = /^\d+([.,]\d{1,2})?$/;
const AVULSO = "";

// As sugestões do "o que está sendo pago" da conta escolhida: as do contrato ou as do avulso (a API
// manda só as que valem)
function suggestionsOf(account, contractId) {
  if (contractId === AVULSO) return account?.singleSession?.paymentSuggestions ?? [];
  return account?.contracts.find((contract) => String(contract.contractId) === contractId)?.paymentSuggestions ?? [];
}

// Os contratos que podem receber um pagamento: os da conta, menos os encerrados (com o saldo zerado,
// não há o que pagar)
function payableContractsOf(account) {
  return (account?.contracts ?? []).filter((contract) => !isClosedContract(contract));
}

// Os valores iniciais: o primeiro contrato que pode receber (o vigente) ou, sem ele, o avulso; a
// primeira sugestão, com o valor no bruto e no líquido; Pix e hoje
export function toPaymentFormValues(account) {
  const [firstContract] = payableContractsOf(account);
  const contractId = firstContract ? String(firstContract.contractId) : AVULSO;
  const [first] = suggestionsOf(account, contractId);
  const amount = first ? toMoneyInputValue(first.amount) : "";
  return {
    contractId,
    suggestion: first?.type ?? OTHER_AMOUNT,
    paymentMethod: "PIX",
    cardInstallments: "1",
    amount,
    netAmount: amount,
    paidAt: toDateInputValue(new Date()),
  };
}

// O pagamento em montagem: as opções do contrato, as sugestões e a prévia da taxa e da previsão de
// recebimento (do backend). account: a conta do paciente (da API). setValue e getFieldState: os do
// useForm (o líquido acompanha o bruto enquanto o usuário não o edita)
export function usePaymentDraft({ control, setValue, getFieldState, account, patientId }) {
  const values = useWatch({ control });
  const contracts = payableContractsOf(account);

  const contractOptions = [
    ...contracts.map((contract) => ({
      value: String(contract.contractId),
      label: formatContractLabelWithStatus(contract, contract.contractStatus),
    })),
    { value: AVULSO, label: "Sem contrato (avulso)" },
  ];
  const suggestions = suggestionsOf(account, values.contractId ?? AVULSO);

  function fillAmount(amount) {
    setValue("amount", amount, { shouldValidate: Boolean(amount) });
    if (!getFieldState("netAmount").isDirty) setValue("netAmount", amount, { shouldValidate: Boolean(amount) });
  }

  // Escolher uma sugestão preenche o valor (editável: o pagamento é de valor livre); "Outro valor"
  // limpa o campo para digitar
  function selectSuggestion(type, contractId = values.contractId) {
    setValue("suggestion", type);
    const suggestion = suggestionsOf(account, contractId).find((option) => option.type === type);
    fillAmount(suggestion ? toMoneyInputValue(suggestion.amount) : "");
  }

  // Trocar o contrato escolhe a primeira sugestão dele
  function selectContract(contractId) {
    setValue("contractId", contractId);
    selectSuggestion(suggestionsOf(account, contractId)[0]?.type ?? OTHER_AMOUNT, contractId);
  }

  // O bruto digitado: o líquido acompanha enquanto não foi editado, e a opção vira "Outro valor"
  function changeAmount(amount) {
    setValue("suggestion", OTHER_AMOUNT);
    if (!getFieldState("netAmount").isDirty) setValue("netAmount", amount);
  }

  // O aviso: o valor passa do maior em aberto (a maior sugestão da API; sem sugestão, nada
  // está em aberto). Só compara: o que passar vira saldo credor, e o pagamento é aceito
  const typedAmount = MONEY_PATTERN.test(String(values.amount ?? "").trim()) ? Number(toApiDecimal(values.amount)) : 0;
  const largestOpen = suggestions.reduce((largest, suggestion) => Math.max(largest, Number(suggestion.amount)), 0);
  const exceedsOpen = typedAmount > largestOpen ? { largestOpen } : null;

  const isCredit = values.paymentMethod === "CREDIT_CARD";
  const payload = {
    amount: toApiDecimal(values.amount),
    netAmount: toApiDecimal(values.netAmount),
    paymentMethod: values.paymentMethod,
    cardInstallments: isCredit ? Number(values.cardInstallments) : null,
    paidAt: values.paidAt,
  };
  const isPreviewReady =
    MONEY_PATTERN.test(String(values.amount ?? "").trim()) &&
    MONEY_PATTERN.test(String(values.netAmount ?? "").trim()) &&
    Boolean(values.paymentMethod && values.paidAt);
  const previewQuery = usePaymentPreview(payload, { enabled: isPreviewReady });

  // O pedido do POST /api/payments (o formato do PaymentRequestDTO)
  function buildRequest(formValues) {
    const credit = formValues.paymentMethod === "CREDIT_CARD";
    return {
      patientId,
      contractId: formValues.contractId === AVULSO ? null : Number(formValues.contractId),
      amount: toApiDecimal(formValues.amount),
      netAmount: toApiDecimal(formValues.netAmount),
      cardInstallments: credit ? Number(formValues.cardInstallments) : null,
      paidAt: formValues.paidAt,
      paymentMethod: formValues.paymentMethod,
    };
  }

  return {
    contractOptions,
    suggestions,
    exceedsOpen,
    isCredit,
    preview: {
      isEnabled: isPreviewReady,
      isLoading: previewQuery.isLoading,
      isError: previewQuery.isError,
      error: previewQuery.error,
      data: previewQuery.data,
    },
    selectContract,
    selectSuggestion,
    changeAmount,
    buildRequest,
  };
}
