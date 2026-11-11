// Busca e filtros de prestadores, em memória sobre a lista inteira (lista pequena). Função pura:
// a page chama com o estado dos filtros. status: "ALL", "ACTIVE" ou "INACTIVE". search procura
// no nome ou no e-mail do usuário vinculado (acrescentado pelo withLinkedUser)
export function filterProviders(providers, { search, status }) {
  const term = search.trim().toLowerCase();
  return providers.filter((provider) => {
    if (status === "ACTIVE" && !provider.active) return false;
    if (status === "INACTIVE" && provider.active) return false;
    if (!term) return true;
    return provider.name.toLowerCase().includes(term) || provider.email?.toLowerCase().includes(term);
  });
}
