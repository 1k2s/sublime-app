package br.com.senai.sublime_app.user.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
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

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um usuário com os dados obrigatórios
    public UserEntity(String email, String password, Role role) {
        validateEmailPasswordAndRole(email, password, role);
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = true;
    }

    public void update(String email, String password, Role role) {
        validateEmailPasswordAndRole(email, password, role);
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Soft delete: o usuário pode estar vinculado a um prestador com atendimentos
    // lançados, então nunca é removido do banco
    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // Inativar duas vezes não é uma operação válida: 409 avisa que o estado já era esse
    private void requireActive() {
        if (!active) {
            throw new ConflictException("O usuário já está inativo.");
        }
    }

    // Invariantes mínimas do usuário. Formato do e-mail fica no DTO (@Email);
    // regras de senha (tamanho, hash) virão com o módulo de autenticação.
    private static void validateEmailPasswordAndRole(String email, String password, Role role) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("O e-mail é obrigatório.");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("A senha é obrigatória.");
        }
        if (role == null) {
            throw new BusinessRuleException("O perfil do usuário é obrigatório.");
        }
    }
}
