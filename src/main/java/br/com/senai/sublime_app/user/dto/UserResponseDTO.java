package br.com.senai.sublime_app.user.dto;

import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// A senha nunca é devolvida
public record UserResponseDTO(

        Long id,
        String email,
        Role role,
        boolean active

) {
    public static UserResponseDTO fromEntity(UserEntity entity) {
        return new UserResponseDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getRole(),
                entity.isActive());
    }
}
