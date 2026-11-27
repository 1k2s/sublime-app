import { normalizeText } from "@/shared/utils/text";

// Filtros da lista do Financeiro, pelos campos que a API manda (sem regra no front). Um paciente
// pode estar em dois (ex: atrasado com saldo credor: Atrasados e Com crédito)
export const ACCOUNT_FILTERS = {
  ALL: { value: "ALL", label: "Todos", matches: () => true },
  OWING: { value: "OWING", label: "Devendo", matches: (account) => Number(account.balance) < 0 },
  LATE: { value: "LATE", label: "Atrasados", matches: (account) => account.hasOverdue },
  CREDIT: { value: "CREDIT", label: "Com crédito", matches: (account) => Number(account.balance) > 0 },
};

// Busca e filtro em memória sobre a lista inteira (lista pequena). A ordem é a da API
export function filterAccounts(accounts, { search, filter }) {
  const term = normalizeText(search);
  const matches = ACCOUNT_FILTERS[filter]?.matches ?? ACCOUNT_FILTERS.ALL.matches;
  return accounts.filter(
    (account) => matches(account) && (!term || normalizeText(account.patientName).includes(term)),
  );
}

// A contagem de cada filtro (na lista inteira, sem a busca): { ALL: 9, OWING: 5, ... }
export function countAccounts(accounts) {
  return Object.fromEntries(
    Object.values(ACCOUNT_FILTERS).map((filter) => [filter.value, accounts.filter(filter.matches).length]),
  );
}
