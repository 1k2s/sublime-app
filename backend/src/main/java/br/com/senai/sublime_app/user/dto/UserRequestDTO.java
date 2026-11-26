package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.shared.validation.StrongPassword;
import br.com.senai.sublime_app.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Criação de usuário. A alteração usa o UserUpdateRequestDTO (senha opcional).
public record UserRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Gravada com hash, nunca devolvida
        @NotBlank(message = "A senha é obrigatória.")
        @StrongPassword
        String password,

        @NotNull(message = "O perfil do usuário é obrigatório.")
        Role role

) {
}
