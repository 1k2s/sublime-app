// Cruzamento da lista de prestadores com a de usuários (as duas inteiras, do cache). Funções
// puras: a page chama com as duas listas. O vínculo é pelo userId (1:1, fixo após a criação)

// Acrescenta a cada prestador o e-mail, o perfil e o status do usuário vinculado
export function withLinkedUser(providers, users) {
  const usersById = new Map(users.map((user) => [user.id, user]));
  return providers.map((provider) => {
    const user = usersById.get(provider.userId);
    return { ...provider, email: user?.email, role: user?.role, userActive: user?.active };
  });
}

// Usuários que podem virar prestador: ativos e ainda sem prestador. A API confere de novo
export function getAvailableUsers(users, providers) {
  const linkedUserIds = new Set(providers.map((provider) => provider.userId));
  return users.filter((user) => user.active && !linkedUserIds.has(user.id));
}
