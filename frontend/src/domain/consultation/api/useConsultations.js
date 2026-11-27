// Hooks do módulo consultation. Tudo sob a chave ["consultations"]: lançar ou desfazer
// invalida a lista, o resumo do dia e os recentes de uma vez
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createConsultation,
  deactivateConsultation,
  getDaySummary,
  getEarnings,
  getEarningsSummary,
  getLaunchOptions,
  getRecentPatients,
  listConsultations,
  listLaunchPatients,
} from "./consultationApi";

export function useLaunchPatients() {
  return useQuery({ queryKey: ["consultations", "patients"], queryFn: listLaunchPatients });
}

// Ligado só com o paciente, a técnica e a data
export function useLaunchOptions({ patientId, techniqueId, occurredAt }) {
  return useQuery({
    queryKey: ["consultations", "launch-options", { patientId, techniqueId, occurredAt }],
    queryFn: () => getLaunchOptions({ patientId, techniqueId, occurredAt }),
    enabled: Boolean(patientId && techniqueId && occurredAt),
  });
}

// enabled: falso para o ADMIN sem prestador escolhido
export function useRecentPatients(providerId, enabled = true) {
  return useQuery({
    queryKey: ["consultations", "recent-patients", providerId],
    queryFn: () => getRecentPatients({ providerId }),
    enabled,
  });
}

export function useDaySummary(providerId, enabled = true) {
  return useQuery({
    queryKey: ["consultations", "day-summary", providerId],
    queryFn: () => getDaySummary({ providerId }),
    enabled,
  });
}

// Os lançamentos de um dia (o de hoje vem do resumo do dia, no fuso da clínica)
export function useDayConsultations(providerId, date, enabled = true) {
  return useQuery({
    queryKey: ["consultations", "list", { providerId, date }],
    queryFn: () => listConsultations({ providerId, from: date, to: date }),
    enabled: enabled && Boolean(date),
  });
}

// providerId: só o ADMIN informa (vazio = todos)
export function useEarningsSummary(providerId) {
  return useQuery({
    queryKey: ["consultations", "earnings-summary", providerId],
    queryFn: () => getEarningsSummary({ providerId }),
  });
}

// Ligado só com o período inteiro (o "Personalizado" pode estar pela metade)
export function useEarnings({ providerId, from, to }) {
  return useQuery({
    queryKey: ["consultations", "earnings", { providerId, from, to }],
    queryFn: () => getEarnings({ providerId, from, to }),
    enabled: Boolean(from && to),
  });
}

// Os lançamentos de um período, pela data do lançamento (a do rendimento)
export function useLaunchedConsultations({ providerId, from, to }) {
  return useQuery({
    queryKey: ["consultations", "list", { providerId, launchedFrom: from, launchedTo: to }],
    queryFn: () => listConsultations({ providerId, launchedFrom: from, launchedTo: to }),
    enabled: Boolean(from && to),
  });
}

export function useCreateConsultation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createConsultation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["consultations"] }),
  });
}

export function useDeactivateConsultation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivateConsultation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["consultations"] }),
  });
}
