// Status do contrato na tela, a partir do que a API resolve (active e current), sem comparar
// datas no front. Ver "Vigente, vencimento e prorrogação" em Contract, no domain-model.
// badge: a variante do Badge
export const CONTRACT_STATUS = {
  CURRENT: { value: "CURRENT", label: "Vigente", badge: "active" },
  // ativo, com o fim antes de hoje: aguarda a decisão do administrador
  EXPIRED: { value: "EXPIRED", label: "Vencido", badge: "warning" },
  // inativado ou substituído por aditivo
  INACTIVE: { value: "INACTIVE", label: "Inativo", badge: "inactive" },
};

export function getContractStatus(contract) {
  if (!contract.active) return CONTRACT_STATUS.INACTIVE;
  return contract.current ? CONTRACT_STATUS.CURRENT : CONTRACT_STATUS.EXPIRED;
}
