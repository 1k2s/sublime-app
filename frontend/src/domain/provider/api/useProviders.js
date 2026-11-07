// Hooks de leitura (com cache) e escrita (com invalidação) do módulo provider
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateProvider,
  createProvider,
  deactivateProvider,
  getProviderById,
  listProviders,
  updateProvider,
} from "@/domain/provider/api/providerApi";

export function useProviders() {
  return useQuery({
    queryKey: ["providers"],
    queryFn: listProviders,
  });
}

export function useProvider(id) {
  return useQuery({
    queryKey: ["providers", id],
    queryFn: () => getProviderById(id),
    enabled: Boolean(id),
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProvider,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["providers"] }),
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => updateProvider(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["providers"] }),
  });
}

export function useDeactivateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivateProvider,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["providers"] }),
  });
}

export function useActivateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: activateProvider,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["providers"] }),
  });
}
