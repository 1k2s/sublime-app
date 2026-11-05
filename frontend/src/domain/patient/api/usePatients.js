// Hooks de pacientes: leitura com cache e escrita com invalidação da chave ["patients"]
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listPatients, getPatientById, createPatient, updatePatient } from "./patientApi";

// Listagem por nome/CPF e status ("ALL", "ACTIVE", "INACTIVE"). enabled permite esperar o usuário digitar
export function usePatients({ search = "", status = "ALL" } = {}, { enabled = true } = {}) {
  return useQuery({
    queryKey: ["patients", { search, status }],
    queryFn: () => listPatients({ search, status }),
    enabled,
  });
}

export function usePatient(id) {
  return useQuery({
    queryKey: ["patients", id],
    queryFn: () => getPatientById(id),
    enabled: Boolean(id),
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
