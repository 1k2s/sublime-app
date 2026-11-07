package br.com.senai.sublime_app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Requisição de definição de senha pessoal no primeiro acesso (ADR 0025).
public record FirstAccessRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        @NotBlank(message = "A senha provisória é obrigatória.")
        String provisionalPassword,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres.")
        String newPassword

) {
}
