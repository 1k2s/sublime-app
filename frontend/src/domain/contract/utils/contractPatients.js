// Cruzamento da lista de contratos com a de pacientes (as duas inteiras, do cache). Função pura:
// a page chama com as duas listas. A API do contrato devolve só os ids; a de pacientes traz os
// ativos e os inativos, então o nome aparece mesmo de quem já foi inativado

// Acrescenta a cada contrato o nome do titular e o do beneficiário (null quando não tem)
export function withPatientNames(contracts, patients) {
  const namesById = new Map(patients.map((patient) => [patient.id, patient.name]));
  return contracts.map((contract) => ({
    ...contract,
    patientName: namesById.get(contract.patientId),
    beneficiaryName: contract.beneficiaryId ? namesById.get(contract.beneficiaryId) : null,
  }));
}
