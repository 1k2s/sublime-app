// Hooks de leitura (com cache) e escrita (com invalidação) do módulo patient
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activatePatient,
  createPatient,
  deactivatePatient,
  listPatients,
  updatePatient,
} from "@/domain/patient/api/patientApi";

// A lista inteira, numa requisição só (cache ["patients"]). Quem lista filtra com o
// filterPatients: a lista inteira continua disponível (ex: o modal lê o paciente dela)
export function usePatients({ enabled = true } = {}) {
  return useQuery({
    queryKey: ["patients"],
    queryFn: listPatients,
    enabled,
  });
}

export function useCreatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createPatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useUpdatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => updatePatient(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useDeactivatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivatePatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useActivatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: activatePatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}
