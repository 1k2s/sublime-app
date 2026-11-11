// Busca e filtros de contratos, em memória sobre a lista inteira (lista pequena). Função pura:
// a page chama com o estado dos filtros. status: "ALL", "ACTIVE" ou "CLOSED". search procura no
// nome do titular e do beneficiário
export function filterContracts(contracts, { search, status }) {
  const term = search.trim().toLowerCase();

  return contracts.filter((contract) => {
    if (status !== "ALL" && contract.status !== status) return false;
    if (!term) return true;
    return [contract.patientName, contract.beneficiaryName].some((name) => name?.toLowerCase().includes(term));
  });
}
