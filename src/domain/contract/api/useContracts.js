import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContract, listContracts, updateContract } from "./contractApi";

// status: "ALL", "ACTIVE" ou "CLOSED". search procura no nome do titular e do beneficiário
function filterContracts(contracts, { search, status }) {
  const term = search.trim().toLowerCase();

  return contracts.filter((contract) => {
    if (status !== "ALL" && contract.status !== status) return false;
    if (!term) return true;
    return [contract.patientName, contract.beneficiaryName].some((name) => name?.toLowerCase().includes(term));
  });
}

// Uma requisição só: os filtros rodam em memória sobre o cache, sem buscar de novo
export function useContracts({ search = "", status = "ALL" } = {}) {
  return useQuery({
    queryKey: ["contracts"],
    queryFn: listContracts,
    select: (contracts) => filterContracts(contracts, { search, status }),
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
