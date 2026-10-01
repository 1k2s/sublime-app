package br.com.senai.sublime_app.user.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.user.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public UserEntity(String email, String password, Role role) {
        validate(email, password, role);
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public void update(String email, String password, Role role) {
        validate(email, password, role);
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Invariantes mínimas do usuário. Formato do e-mail fica no DTO (@Email);
    // regras de senha (tamanho, hash) virão com o módulo de autenticação.
    private static void validate(String email, String password, Role role) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("User email is required.");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("User password is required.");
        }
        if (role == null) {
            throw new BusinessRuleException("User role is required.");
        }
    }
}
