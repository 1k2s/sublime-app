import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  amendContract,
  createContract,
  deactivateContract,
  getAmendmentPreview,
  getContractPreview,
  getSessionBalance,
  listContractVersions,
  listContracts,
} from "./contractApi";

// A lista inteira, numa requisição só (cache ["contracts"]). Quem lista filtra com o
// filterContracts
export function useContracts() {
  return useQuery({
    queryKey: ["contracts"],
    queryFn: listContracts,
  });
}

export function useCreateContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}

export function useDeactivateContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deactivateContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}

// Aditivo: mutate({ id, payload }). A invalidação de ["contracts"] também atualiza o histórico
// (as versões ficam sob a mesma chave)
export function useAmendContract() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: amendContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
}

// Histórico da versão (id null = não busca). Sob ["contracts"]: o aditivo e a inativação o invalidam
export function useContractVersions(id) {
  return useQuery({
    queryKey: ["contracts", id, "versions"],
    queryFn: () => listContractVersions(id),
    enabled: Boolean(id),
  });
}

// Saldo de sessões dos itens, lido ao abrir o aditivo. Sem invalidação pelo lançamento (outro
// módulo): na próxima abertura, busca de novo
export function useContractSessionBalance(contractId) {
  return useQuery({
    queryKey: ["contracts", contractId, "session-balance"],
    queryFn: () => getSessionBalance(contractId),
    enabled: Boolean(contractId),
  });
}

// Prévia do contrato no cadastro: busca de novo a cada mudança do plano, do início, dos itens ou
// das parcelas (a chave muda). Só com plano e início preenchidos. retry: false, porque o 400
// (ex: "passa do prazo máximo de 52 semanas") é a resposta, não uma falha a repetir.
// keepPreviousData: enquanto recalcula, a tela continua com a prévia anterior (o fim e o total
// não piscam, e as opções do plano do Grupo continuam lá)
export function useContractPreview(payload, { enabled = true } = {}) {
  return useQuery({
    queryKey: ["contracts", "preview", payload],
    queryFn: () => getContractPreview(payload),
    enabled: enabled && Boolean(payload.planId && payload.startDate),
    retry: false,
    placeholderData: keepPreviousData,
  });
}

// Prévia do aditivo: o mesmo comportamento da prévia do cadastro, pela versão atual (id). null =
// não busca (o formulário está no modo de cadastro)
export function useAmendmentPreview(id, payload) {
  return useQuery({
    queryKey: ["contracts", id, "amendment-preview", payload],
    queryFn: () => getAmendmentPreview({ id, payload }),
    enabled: Boolean(id),
    retry: false,
    placeholderData: keepPreviousData,
  });
}
