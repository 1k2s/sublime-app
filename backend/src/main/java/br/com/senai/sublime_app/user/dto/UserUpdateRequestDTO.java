package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.shared.validation.StrongPassword;
import br.com.senai.sublime_app.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Alteração de usuário. Separado do UserRequestDTO porque a senha é opcional: com hash,
// a senha atual não volta para o formulário, e exigir uma senha nova obrigaria o admin
// a inventar uma só para corrigir um e-mail.
public record UserUpdateRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Vazia ou ausente = mantém a senha atual; preenchida = troca (o @StrongPassword aceita a vazia)
        @StrongPassword
        String password,

        @NotNull(message = "O perfil do usuário é obrigatório.")
        Role role

) {
    // Se a requisição traz uma senha nova para trocar
    public boolean hasNewPassword() {
        return password != null && !password.isBlank();
    }
}
