// Hooks de leitura e escrita do módulo consultation
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createConsultations, listConsultations, previewConsultation } from "./consultationApi";

export function useConsultations(filters) {
  return useQuery({
    queryKey: ["consultations", filters],
    queryFn: () => listConsultations(filters),
  });
}

// A prévia não grava nada, mas é disparada pelo Enter e não ao montar a tela: por isso useMutation
export function usePreviewConsultation() {
  return useMutation({ mutationFn: previewConsultation });
}

export function useCreateConsultations() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createConsultations,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["consultations"] }),
  });
}
