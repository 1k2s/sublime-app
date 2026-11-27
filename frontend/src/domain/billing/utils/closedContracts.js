// Os contratos encerrados (terminados e com o saldo zerado: o status CLOSED, da API) ficam
// recolhidos na conta, para consulta: no Resumo, numa seção no fim; nas abas Parcelas e Sessões,
// atrás da pílula "Encerrados"; fora do "Registrar pagamento" (não há o que pagar). Funções puras
// sobre os contratos da conta (contracts da API)
export function isClosedContract(contract) {
  return contract.contractStatus === "CLOSED";
}

// O contrato que as abas Parcelas e Sessões abrem: o escolhido, se ainda está na conta; senão, o
// primeiro que não está encerrado (o vigente vem primeiro) ou, só com encerrados, o mais recente
export function defaultContractId(contracts, chosenContractId) {
  if (contracts.some((contract) => contract.contractId === chosenContractId)) return chosenContractId;
  const open = contracts.find((contract) => !isClosedContract(contract));
  return (open ?? contracts[0])?.contractId ?? null;
}
