// Hooks de pacientes: leitura com cache e escrita com invalidação da chave ["patients"]
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { onlyDigits } from "@/shared/utils/masks";
import {
  listPatients,
  getPatientById,
  createPatient,
  updatePatient,
  deactivatePatient,
  activatePatient,
} from "./patientApi";

// Busca por nome ou dígitos do CPF e filtro por status ("ALL", "ACTIVE", "INACTIVE")
function filterPatients(patients, { search, status }) {
  const term = search.trim().toLowerCase();
  const digits = onlyDigits(term);

  return patients.filter((patient) => {
    if (status === "ACTIVE" && !patient.active) return false;
    if (status === "INACTIVE" && patient.active) return false;
    if (!term) return true;
    return patient.name.toLowerCase().includes(term) || (digits && patient.cpf.includes(digits));
  });
}

// A lista vem inteira da API; busca e status são filtrados em memória (select)
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

function usePatientMutation(mutationFn) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useCreatePatient() {
  return usePatientMutation(createPatient);
}

export function useUpdatePatient() {
  return usePatientMutation(({ id, data }) => updatePatient(id, data));
}

export function useDeactivatePatient() {
  return usePatientMutation(deactivatePatient);
}

export function useActivatePatient() {
  return usePatientMutation(activatePatient);
}
