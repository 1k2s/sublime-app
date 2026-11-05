package br.com.senai.sublime_app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Sem tamanho mínimo aqui: no login só importa se confere com o hash
        @NotBlank(message = "A senha é obrigatória.")
        String password

) {
}
