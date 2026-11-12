import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContract, deactivateContract, getContractPreview, listContracts } from "./contractApi";

// A lista inteira, numa requisição só (cache ["contracts"]). Quem lista filtra com o
// filterContracts
export function useContracts() {
  return useQuery({
    queryKey: ["contracts"],
    queryFn: listContracts,
  });
}

export function useCreateContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}

export function useDeactivateContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivateContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}

// Prévia do contrato no cadastro: busca de novo a cada mudança do plano, do início, dos itens ou
// das parcelas (a chave muda). Só com plano e início preenchidos. retry: false, porque o 400
// (ex: "passa do prazo máximo de 52 semanas") é a resposta, não uma falha a repetir.
// keepPreviousData: enquanto recalcula, a tela continua com a prévia anterior (o fim e o total
// não piscam, e as opções do plano do Grupo continuam lá)
export function useContractPreview(payload) {
  return useQuery({
    queryKey: ["contracts", "preview", payload],
    queryFn: () => getContractPreview(payload),
    enabled: Boolean(payload.planId && payload.startDate),
    retry: false,
    placeholderData: keepPreviousData,
  });
}
