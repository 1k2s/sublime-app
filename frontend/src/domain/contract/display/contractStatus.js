// Status do contrato na tela, como a API resolve (o campo status: depende da vigência e, depois do
// fim, do saldo da conta, que é do billing), sem regra no front. Ver "Status do contrato" em
// Contract, no domain-model. badge: a variante do Badge
export const CONTRACT_STATUS = {
  CURRENT: { value: "CURRENT", label: "Vigente", badge: "active" },
  // passou do fim com pendência (dívida ou crédito): aguarda a decisão do administrador
  EXPIRED: { value: "EXPIRED", label: "Vencido", badge: "warning" },
  // passou do fim com o saldo zerado
  CLOSED: { value: "CLOSED", label: "Encerrado", badge: "neutral" },
  // inativado à mão (o "Cancelar contrato")
  CANCELED: { value: "CANCELED", label: "Cancelado", badge: "inactive" },
  // a versão substituída por aditivo (só no histórico)
  SUPERSEDED: { value: "SUPERSEDED", label: "Substituída", badge: "default" },
};

export function getContractStatusByValue(status) {
  return CONTRACT_STATUS[status] ?? CONTRACT_STATUS.CURRENT;
}

// contract: a resposta da API (ContractResponseDTO), com o status
export function getContractStatus(contract) {
  return getContractStatusByValue(contract.status);
}
