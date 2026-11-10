package br.com.senai.sublime_app.user.domain;

import br.com.senai.sublime_app.user.enums.Role;

// Quem está fazendo a requisição: o id e o perfil do usuário logado. Os controllers o
// recebem como parâmetro (montado pelo CurrentUserArgumentResolver, no auth, a partir do
// token) e o passam ao service, que aplica as regras de acesso aos dados. Fica no user, e
// não no auth, para os módulos de negócio não dependerem do auth.
// Sem validação: só o resolver cria, a partir de um token já conferido pelo
// ActiveUserTokenValidator (usuário existe, está ativo e com o mesmo perfil).
public record CurrentUser(Long userId, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
