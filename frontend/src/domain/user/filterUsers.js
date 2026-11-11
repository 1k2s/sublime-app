// Busca e filtros da tela de Usuários, em memória sobre a lista inteira (lista pequena).
// Função pura: a page chama com o estado dos filtros; o modal lê da lista sem filtro
export function filterUsers(users, { search, role, status }) {
  const term = search.trim().toLowerCase();
  return users.filter((user) => {
    if (role !== "ALL" && user.role !== role) return false;
    if (status === "ACTIVE" && !user.active) return false;
    if (status === "INACTIVE" && user.active) return false;
    return !term || user.email.toLowerCase().includes(term);
  });
}
