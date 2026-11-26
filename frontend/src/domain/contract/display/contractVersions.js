// Versões do contrato na tela, a partir do que a API devolve (amendmentNumber e superseded), sem
// percorrer a cadeia no front. Ver "Versionamento (aditivos)" em Contract, no domain-model

// "Original" na primeira versão, "Aditivo N" nas seguintes
export function getVersionLabel(contract) {
  return contract.amendmentNumber > 0 ? `Aditivo ${contract.amendmentNumber}` : "Original";
}

// Selo do aditivo ao lado do estado (lista e detalhe): só quando houve aditivo
export function hasAmendment(contract) {
  return contract.amendmentNumber > 0;
}

// Selo da versão no histórico. badge: a variante do Badge
export function getVersionBadge(version) {
  return version.superseded ? { label: "Substituída", badge: "default" } : { label: "Atual", badge: "active" };
}
