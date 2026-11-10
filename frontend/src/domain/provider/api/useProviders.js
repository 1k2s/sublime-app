import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateProvider,
  createProviderWithUser,
  deactivateProvider,
  listProviders,
  listProvidersWithUsers,
  updateProviderWithUser,
} from "./providerApi";

export function useProviders({ search = "", status = "ALL" } = {}) {
  return useQuery({
    queryKey: ["providers"],
    queryFn: listProvidersWithUsers,
    select: (providers) => {
      const normalizedSearch = search.trim().toLowerCase();

      return providers.filter((provider) => {
        const matchesStatus =
          !status || status === "ALL" ? true : provider.status === status;

        if (!matchesStatus) return false;
        if (!normalizedSearch) return true;

        const matchesName = provider.name?.toLowerCase().includes(normalizedSearch);
        const matchesEmail = provider.email?.toLowerCase().includes(normalizedSearch);
        return matchesName || matchesEmail;
      });
    },
  });
}

// Prestador vinculado ao usuário logado (o vínculo é 1:1), ou undefined se não houver (ex: ADMIN).
// Busca a lista inteira porque a API ainda não expõe o prestador por usuário (ver roadmap.md)
export function useCurrentProvider(userId) {
  return useQuery({
    queryKey: ["providers", "by-user", userId],
    queryFn: listProviders,
    select: (providers) => providers.find((provider) => provider.userId === userId),
    enabled: userId != null,
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createProviderWithUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateProviderWithUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export function useToggleProviderStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ provider, nextStatus }) => {
      if (nextStatus === "INACTIVE") {
        await deactivateProvider(provider.id);
      } else {
        await activateProvider(provider.id);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["providers"] });
    },
  });
}
