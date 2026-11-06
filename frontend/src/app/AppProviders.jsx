import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/domain/auth/context/AuthContext";

// Padrão do TanStack Query (staleTime: 0): a tela mostra o cache na hora e busca de novo por baixo
// ao ser montada, quando a janela volta a ter foco e quando a internet reconecta. Depois de salvar,
// nada disso acontece: por isso toda mutation invalida a chave do módulo (invalidateQueries)
const queryClient = new QueryClient();

export function AppProviders({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}
