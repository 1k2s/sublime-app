import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateProvider,
  createProvider,
  deactivateProvider,
  getRetroactiveGrant,
  grantRetroactiveLaunches,
  listProviders,
  updateProvider,
} from "./providerApi";

// A lista inteira, numa requisição só (cache ["providers"]). Quem lista filtra com o
// filterProviders: a lista inteira continua disponível (ex: o modal lê o prestador dela).
// enabled: falso onde só o ADMIN pode listar (ex: a tela de lançamento do PROVIDER)
export function useProviders({ enabled = true } = {}) {
  return useQuery({
    queryKey: ["providers"],
    queryFn: listProviders,
    enabled,
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

// A liberação de lançamentos retroativos de um prestador (null = sem liberação hoje).
// providerId nulo (modal fechado) mantém a consulta desligada
export function useRetroactiveGrant(providerId) {
  return useQuery({
    queryKey: ["providers", providerId, "retroactive-grant"],
    queryFn: () => getRetroactiveGrant(providerId),
    enabled: providerId != null,
  });
}

export function useGrantRetroactiveLaunches() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ providerId, allowedCount }) => grantRetroactiveLaunches(providerId, allowedCount),
    onSuccess: (_, { providerId }) => {
      queryClient.invalidateQueries({ queryKey: ["providers", providerId, "retroactive-grant"] });
    },
  });
}
