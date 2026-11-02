import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContract, listContracts, updateContract } from "./contractApi";

export function useContracts({ search = "", status = "ALL" } = {}) {
  return useQuery({
    queryKey: ["contracts", { search, status }],
    queryFn: () => listContracts({ search, status }),
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
