package br.com.senai.sublime_app.auth.dto;

import java.time.Instant;

import br.com.senai.sublime_app.auth.security.AuthenticatedUser;
import br.com.senai.sublime_app.auth.service.TokenService.GeneratedToken;
import br.com.senai.sublime_app.user.enums.Role;

// Resposta do login: o token (enviado depois em "Authorization: Bearer <token>"), quando
// ele vence e os dados do usuário para a tela (nunca a senha)
public record LoginResponseDTO(

        String token,
        Instant expiresAt,
        LoggedUser user

) {
    public static LoginResponseDTO from(GeneratedToken token, AuthenticatedUser user) {
        return new LoginResponseDTO(token.value(), token.expiresAt(),
                new LoggedUser(user.id(), user.email(), user.role()));
    }

    public record LoggedUser(Long id, String email, Role role) {
    }
}
