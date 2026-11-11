import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContract, listContracts, updateContract } from "./contractApi";

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

export function useUpdateContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => updateContract(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}
