import { useQuery } from "@tanstack/react-query";
import { listTechniques } from "./pricingApi";

export function useTechniques() {
  return useQuery({
    queryKey: ["pricing", "techniques"],
    queryFn: listTechniques,
    // Catálogo muda pouco: não precisa buscar de novo a cada tela
    staleTime: 5 * 60 * 1000,
  });
}
