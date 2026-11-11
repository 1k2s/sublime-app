// Busca e filtros de prestadores, em memória sobre a lista inteira (lista pequena). Função pura:
// a page chama com o estado dos filtros. status: "ALL", "ACTIVE" ou "INACTIVE". search procura
// no nome ou no e-mail do usuário vinculado
export function filterProviders(providers, { search, status }) {
  const term = search.trim().toLowerCase();

  return providers.filter((provider) => {
    if (status !== "ALL" && provider.status !== status) return false;
    if (!term) return true;

    const matchName = provider.name?.toLowerCase().includes(term);
    const matchEmail = provider.email?.toLowerCase().includes(term);
    return matchName || matchEmail;
  });
}
