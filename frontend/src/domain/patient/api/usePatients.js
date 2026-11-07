// Hooks de leitura (com cache e filtro em memória) e escrita (com invalidação) do módulo patient
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { onlyDigits } from "@/shared/utils/masks";
import {
  activatePatient,
  createPatient,
  deactivatePatient,
  getPatientById,
  listPatients,
  updatePatient,
} from "@/domain/patient/api/patientApi";

// status: "ALL", "ACTIVE" ou "INACTIVE". search procura no nome ou nos dígitos do CPF
function filterPatients(patients, { search, status }) {
  const term = search.trim().toLowerCase();
  const digits = onlyDigits(term);

  return patients.filter((patient) => {
    if (status === "ACTIVE" && !patient.active) return false;
    if (status === "INACTIVE" && patient.active) return false;
    if (!term) return true;

    const matchName = patient.name?.toLowerCase().includes(term);
    const matchCpf = digits ? onlyDigits(patient.cpf).includes(digits) : false;
    return matchName || matchCpf;
  });
}

// Uma requisição só: os filtros rodam em memória no select sobre o cache ["patients"]
export function usePatients({ search = "", status = "ALL" } = {}, { enabled = true } = {}) {
  return useQuery({
    queryKey: ["patients"],
    queryFn: listPatients,
    select: (patients) => filterPatients(patients, { search, status }),
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
