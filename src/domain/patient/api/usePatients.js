/**
 * ======================================================================================
 * HOOKS REACT QUERY (TANSTACK QUERY): DOMÍNIO PATIENT
 * ======================================================================================
 *
 * Responsável por gerenciar o ciclo de vida dos dados assíncronos de pacientes:
 * - Leitura com cache automático e refetch (`useQuery`).
 * - Mutações (criação e atualização) com invalidação da chave `["patients"]` (`useMutation`).
 *
 * ⚠️ DICA DE ARQUITETURA:
 * Components e Pages NUNCA chamam `patientApi.js` diretamente.
 * Eles consomem apenas estes hooks, garantindo isolamento da camada de estado da interface.
 */

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listPatients, getPatientById, createPatient, updatePatient } from "./patientApi";

/**
 * Hook de listagem de pacientes.
 * Armazena os dados em cache vinculados aos parâmetros de busca e status.
 *
 * @param {Object} [params]
 * @param {string} [params.search=""] - Filtro textual por nome ou CPF
 * @param {string} [params.status="ALL"] - Filtro por status ("ALL", "ACTIVE", "INACTIVE")
 * @returns {import("@tanstack/react-query").UseQueryResult<Array<Object>>}
 */
export function usePatients({ search = "", status = "ALL" } = {}) {
  return useQuery({
    queryKey: ["patients", { search, status }],
    queryFn: () => listPatients({ search, status }),
  });
}

/**
 * Hook para carregar um único paciente pelo ID.
 *
 * @param {string|number} id - Identificador do paciente
 * @returns {import("@tanstack/react-query").UseQueryResult<Object>}
 */
export function usePatient(id) {
  return useQuery({
    queryKey: ["patients", id],
    queryFn: () => getPatientById(id),
    enabled: Boolean(id), // Só dispara a requisição se o ID for válido
  });
}

/**
 * Hook de mutação para cadastro de novos pacientes.
 * Após o sucesso, invalida a chave `["patients"]` para atualizar a tabela na tela.
 *
 * @returns {import("@tanstack/react-query").UseMutationResult}
 */
export function useCreatePatient() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createPatient,
    onSuccess: () => {
      // Invalida o cache de pacientes para disparar recarregamento automático
      queryClient.invalidateQueries({ queryKey: ["patients"] });
    },
  });
}

/**
 * Hook de mutação para atualização de dados de um paciente existente.
 * Após o sucesso, invalida a chave `["patients"]` para sincronizar os dados na tela.
 *
 * @returns {import("@tanstack/react-query").UseMutationResult}
 */
export function useUpdatePatient() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }) => updatePatient(id, data),
    onSuccess: () => {
      // Invalida o cache de pacientes para atualizar a listagem
      queryClient.invalidateQueries({ queryKey: ["patients"] });
    },
  });
}
