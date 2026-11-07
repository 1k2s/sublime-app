import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateProvider,
  createProviderWithUser,
  deactivateProvider,
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
