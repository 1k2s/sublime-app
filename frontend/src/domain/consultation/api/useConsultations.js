// Hooks de leitura e escrita do módulo consultation
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createConsultation, listConsultations, listEligibleContracts } from "./consultationApi";

export function useConsultations(filters) {
  return useQuery({
    queryKey: ["consultations", filters],
    queryFn: () => listConsultations(filters),
    enabled: filters.providerId != null,
  });
}

// Não grava nada, mas é disparada pelo "Adicionar" e não ao montar a tela: por isso useMutation
export function useEligibleContracts() {
  return useMutation({ mutationFn: listEligibleContracts });
}

// Grava os itens do carrinho um por um (a API não tem lançamento em lote). Um item que falha
// não interrompe os outros: cada resultado volta com o atendimento criado ou a mensagem de erro.
// A mutation só termina depois do último POST, então o isPending cobre o laço inteiro.
export function useCreateConsultations() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (items) => {
      const results = [];
      for (const { key, payload } of items) {
        try {
          results.push({ key, created: await createConsultation(payload) });
        } catch (error) {
          results.push({ key, error: error.message });
        }
      }
      return results;
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ["consultations"] }),
  });
}
