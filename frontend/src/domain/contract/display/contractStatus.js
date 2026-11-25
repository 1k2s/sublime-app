// Status do contrato na tela, a partir do que a API resolve (active e current), sem comparar
// datas no front. Ver "Vigente, vencimento e prorrogação" em Contract, no domain-model.
// badge: a variante do Badge
export const CONTRACT_STATUS = {
  // o mesmo verde do selo "Aditivo N" (variante default), que se destaca na linha com o mouse em cima
  CURRENT: { value: "CURRENT", label: "Vigente", badge: "default" },
  // ativo, com o fim antes de hoje: aguarda a decisão do administrador
  EXPIRED: { value: "EXPIRED", label: "Vencido", badge: "warning" },
  // inativado. A versão substituída por aditivo não aparece na lista (só a atual de cada
  // contrato); no histórico, ela tem o selo "Substituída"
  INACTIVE: { value: "INACTIVE", label: "Inativo", badge: "inactive" },
};

export function getContractStatus(contract) {
  if (!contract.active) return CONTRACT_STATUS.INACTIVE;
  return contract.current ? CONTRACT_STATUS.CURRENT : CONTRACT_STATUS.EXPIRED;
}
