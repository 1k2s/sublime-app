// Hooks de leitura (com cache) e escrita (com invalidação) do módulo pricing
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createDurationPrice,
  createFrequencyPrice,
  getPricingGroupPriceHistory,
  listPlans,
  listPricingGroups,
  listTechniques,
} from "./pricingApi";

export function usePricingGroups() {
  return useQuery({ queryKey: ["pricing-groups"], queryFn: listPricingGroups });
}

export function usePlans() {
  return useQuery({ queryKey: ["plans"], queryFn: listPlans });
}

export function useTechniques() {
  return useQuery({ queryKey: ["techniques"], queryFn: listTechniques });
}

// Chave sob ["prices"] de propósito: a invalidação das mutations de preço cobre este histórico
// e as tabelas por técnica do contrato (["prices", techniqueId], no useContractDraft)
export function usePricingGroupPriceHistory(pricingGroupId) {
  return useQuery({
    queryKey: ["prices", "group", pricingGroupId, "history"],
    queryFn: () => getPricingGroupPriceHistory(pricingGroupId),
    enabled: pricingGroupId != null,
  });
}

function savePrice({ pricingModel, ...payload }) {
  return pricingModel === "DURATION_BASED" ? createDurationPrice(payload) : createFrequencyPrice(payload);
}

export function useSavePrice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: savePrice,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["prices"] }),
  });
}

// Valores alterados de um plano (ex: 30 e 60 min), um POST por valor, em sequência: o backend
// grava uma linha por vez. changes: [{ label, payload }], o label vai na mensagem de erro.
// Invalida no onSettled, não só no onSuccess: se o segundo falhar, o primeiro já vale e a
// tabela precisa mostrá-lo (salvar de novo é seguro, só reenvia o que ainda difere)
export function useSavePlanPrices() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (changes) => {
      for (const { label, payload } of changes) {
        try {
          await savePrice(payload);
        } catch (error) {
          error.message = `${label}: ${error.message}`;
          throw error;
        }
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ["prices"] }),
  });
}
