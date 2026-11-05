import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/domain/auth/context/AuthContext";

// Dados só voltam a ser buscados no F5 ou quando uma escrita invalida a chave
const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: Infinity } },
});

export function AppProviders({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}
