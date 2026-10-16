// Hooks de leitura e escrita do módulo consultation
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createConsultations, previewConsultation } from "./consultationApi";

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
