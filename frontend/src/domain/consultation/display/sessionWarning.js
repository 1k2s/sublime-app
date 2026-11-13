// Aviso de saldo do item do contrato, a partir do remainingSessions da API: só quando está
// acabando (2 ou menos) ou acabou. O saldo em si não aparece na tela do prestador.
// Devolve { text, level } ("warning" | "danger") ou null
const LOW_BALANCE_LIMIT = 2;

export function getSessionWarning(remainingSessions) {
  if (remainingSessions == null || remainingSessions > LOW_BALANCE_LIMIT) return null;
  if (remainingSessions <= 0) return { text: "sessões esgotadas", level: "danger" };
  return { text: remainingSessions === 1 ? "resta 1 sessão" : `restam ${remainingSessions} sessões`, level: "warning" };
}
