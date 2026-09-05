package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.user.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long id;
    private String email;
    private Role role;
}
