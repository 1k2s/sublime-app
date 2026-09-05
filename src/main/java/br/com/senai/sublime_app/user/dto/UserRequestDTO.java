package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role;
}
