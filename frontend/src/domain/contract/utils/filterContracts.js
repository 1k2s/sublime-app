import { getContractStatus } from "../display/contractStatus";

// Busca e filtros de contratos, em memória sobre a lista inteira (lista pequena). Função pura:
// a page chama com o estado dos filtros. status: "ALL" ou um valor do CONTRACT_STATUS
// ("CURRENT", "EXPIRED", "INACTIVE"). search procura no nome do titular e do beneficiário
// (acrescentados pelo withPatientNames)
export function filterContracts(contracts, { search, status }) {
  const term = search.trim().toLowerCase();

  return contracts.filter((contract) => {
    if (status !== "ALL" && getContractStatus(contract).value !== status) return false;
    if (!term) return true;
    return [contract.patientName, contract.beneficiaryName].some((name) => name?.toLowerCase().includes(term));
  });
}
