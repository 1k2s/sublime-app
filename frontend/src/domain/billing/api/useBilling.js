import { useQuery } from "@tanstack/react-query";
import {
  getContractSessions,
  getPatientAccount,
  listAccounts,
  listContractInstallments,
  listPatientTransfers,
} from "./billingApi";

// Tudo sob ["billing"]: um pagamento registrado ou inativado invalida a chave inteira (a lista, a
// conta, as parcelas e os pagamentos). O lançamento e o aditivo são de outros módulos: a conta
// busca de novo ao abrir (staleTime 0, ver "Camada api/" no architecture)

// A lista inteira, numa requisição só, já na ordem. Quem lista filtra com o filterAccounts
export function useAccounts() {
  return useQuery({
    queryKey: ["billing", "accounts"],
    queryFn: listAccounts,
  });
}

// A conta do paciente aberto (patientId null = não busca)
export function usePatientAccount(patientId) {
  return useQuery({
    queryKey: ["billing", "patients", patientId, "account"],
    queryFn: () => getPatientAccount(patientId),
    enabled: Boolean(patientId),
  });
}

// As transferências de saldo do titular (a aba Pagamentos)
export function usePatientTransfers(patientId) {
  return useQuery({
    queryKey: ["billing", "patients", patientId, "transfers"],
    queryFn: () => listPatientTransfers(patientId),
    enabled: Boolean(patientId),
  });
}

export function useContractInstallments(contractId) {
  return useQuery({
    queryKey: ["billing", "contracts", contractId, "installments"],
    queryFn: () => listContractInstallments(contractId),
    enabled: Boolean(contractId),
  });
}

export function useContractSessions(contractId) {
  return useQuery({
    queryKey: ["billing", "contracts", contractId, "sessions"],
    queryFn: () => getContractSessions(contractId),
    enabled: Boolean(contractId),
  });
}
