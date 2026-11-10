package br.com.senai.sublime_app.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Configurações do próprio usuário (PUT /api/users/me): e-mail e senha, exigindo a senha
// atual. Sem o perfil: ele nunca muda por aqui.
public record UserSelfUpdateRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Vazia ou ausente = mantém a senha atual; preenchida = troca
        String newPassword,

        @NotBlank(message = "A senha atual é obrigatória.")
        String currentPassword

) {
    // Um @Size puro recusaria a string vazia, que aqui significa "manter a senha"
    @AssertTrue(message = "A nova senha deve ter no mínimo 8 caracteres.")
    public boolean isNewPasswordValid() {
        return !hasNewPassword() || newPassword.length() >= 8;
    }

    // Se a requisição traz uma senha nova para trocar
    public boolean hasNewPassword() {
        return newPassword != null && !newPassword.isBlank();
    }
}
