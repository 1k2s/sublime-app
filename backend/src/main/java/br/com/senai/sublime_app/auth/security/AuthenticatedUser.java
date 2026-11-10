package br.com.senai.sublime_app.auth.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// O usuário do sistema visto pelo Spring Security, no login. Guarda uma cópia dos dados
// (não a entidade), para não depender da sessão do Hibernate depois da consulta.
public record AuthenticatedUser(
        Long id,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        boolean firstAccessPending
) implements UserDetails {

    public static AuthenticatedUser from(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive(),
                user.isFirstAccessPending());
    }

    // Prefixo ROLE_: é o que o hasRole("ADMIN") da autorização procura
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // Usuário inativo (soft delete) não faz login
    @Override
    public boolean isEnabled() {
        return active;
    }
}
