package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Criação de usuário. A alteração usa o UserUpdateRequestDTO (senha opcional).
public record UserRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Só o tamanho mínimo, sem exigir maiúscula ou símbolo (NIST SP 800-63B: o que
        // protege é o tamanho). Gravada com hash, nunca devolvida.
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        String password,

        @NotNull(message = "O perfil do usuário é obrigatório.")
        Role role

) {
}
