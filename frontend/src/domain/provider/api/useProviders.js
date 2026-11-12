import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateProvider,
  createProvider,
  deactivateProvider,
  listProviders,
  updateProvider,
} from "./providerApi";

// A lista inteira, numa requisição só (cache ["providers"]). Quem lista filtra com o
// filterProviders: a lista inteira continua disponível (ex: o modal lê o prestador dela)
export function useProviders() {
  return useQuery({
    queryKey: ["providers"],
    queryFn: listProviders,
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createProvider,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
    },
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => updateProvider(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
    },
  });
}

// Um hook por endpoint (DELETE e PATCH .../activate), como em pacientes e usuários
export function useDeactivateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivateProvider,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
    },
  });
}

export function useActivateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: activateProvider,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
    },
  });
}
