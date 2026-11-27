import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deactivatePayment, getPaymentPreview, listPayments, registerPayment } from "./paymentApi";

// Os pagamentos ficam sob ["billing"]: registrar ou inativar muda a conta toda (saldo, parcelas,
// situação), então a invalidação é da chave do módulo

export function usePayments(patientId) {
  return useQuery({
    queryKey: ["billing", "payments", patientId],
    queryFn: () => listPayments(patientId),
    enabled: Boolean(patientId),
  });
}

// A taxa e a previsão de recebimento: busca de novo a cada mudança do valor, do líquido, da forma,
// do "parcelado em" ou da data (a chave muda). retry: false, porque o 400 (ex: "Só o cartão de
// crédito pode ser parcelado.") é a resposta, não uma falha a repetir. keepPreviousData: enquanto
// recalcula, a conferência não pisca (o mesmo do useContractPreview)
export function usePaymentPreview(payload, { enabled = true } = {}) {
  return useQuery({
    queryKey: ["billing", "payments", "preview", payload],
    queryFn: () => getPaymentPreview(payload),
    enabled,
    retry: false,
    placeholderData: keepPreviousData,
  });
}

export function useRegisterPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: registerPayment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["billing"] }),
  });
}

export function useDeactivatePayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivatePayment,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["billing"] }),
  });
}
