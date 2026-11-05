// Hooks de leitura (com cache) e escrita (com invalidação) do módulo pricing
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createDurationPrice,
  createFrequencyPrice,
  getTechniquePrices,
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

// techniqueId nulo (grupo sem técnica) mantém a consulta desligada
export function useTechniquePrices(techniqueId) {
  return useQuery({
    queryKey: ["prices", techniqueId],
    queryFn: () => getTechniquePrices(techniqueId),
    enabled: techniqueId != null,
  });
}

export function useSavePrice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ pricingModel, ...payload }) =>
      pricingModel === "DURATION_BASED" ? createDurationPrice(payload) : createFrequencyPrice(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["prices"] }),
  });
}
