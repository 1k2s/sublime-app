import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/domain/auth/context/AuthContext";

const queryClient = new QueryClient();

export function AppProviders({ children }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}
