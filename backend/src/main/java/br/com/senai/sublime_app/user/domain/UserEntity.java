package br.com.senai.sublime_app.user.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.user.enums.Role;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // Sempre o hash (BCrypt), nunca a senha digitada: quem faz o hash é o UserService
    @Column(nullable = false)
    private String password;

    // varchar, não o enum nativo do MySQL: valor novo no enum não exige migration (ADR 0021)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    // Admin criado na primeira subida (ADMIN_EMAIL/ADMIN_PASSWORD): não pode ser inativado
    // nem mudar de perfil, então sempre existe um admin ativo. Marcado só pela fábrica
    // defaultAdmin, nunca pelo request.
    @Column(name = "default_admin", nullable = false)
    private boolean defaultAdmin = false;

    // Construtor publico para criar um usuário com os dados obrigatórios. Recebe a senha
    // já com hash (encodedPassword).
    public UserEntity(String email, String encodedPassword, Role role) {
        validateEmailAndRole(email, role);
        validateEncodedPassword(encodedPassword);
        this.email = email;
        this.password = encodedPassword;
        this.role = role;
        this.active = true;
    }

    // Fábrica do admin padrão: mesmas regras de qualquer usuário, com a marca
    public static UserEntity defaultAdmin(String email, String encodedPassword) {
        UserEntity user = new UserEntity(email, encodedPassword, Role.ADMIN);
        user.defaultAdmin = true;
        return user;
    }

    // Alteração dos dados de acesso, sem a senha (ver changePassword)
    public void update(String email, Role role) {
        validateEmailAndRole(email, role);
        requireRoleUnchangedIfDefaultAdmin(role);
        this.email = email;
        this.role = role;
    }

    // Troca só do e-mail (Configurações do próprio usuário): o perfil não muda por aqui
    public void changeEmail(String email) {
        validateEmailAndRole(email, role);
        this.email = email;
    }

    // Troca de senha, separada do update: na alteração a senha é opcional
    public void changePassword(String encodedPassword) {
        validateEncodedPassword(encodedPassword);
        this.password = encodedPassword;
    }

    // Soft delete: o usuário pode estar vinculado a um prestador com atendimentos
    // lançados, então nunca é removido do banco
    public void deactivate() {
        requireNotDefaultAdmin();
        requireActive();
        this.active = false;
    }

    // Reativação: devolve o acesso ao sistema (o usuário volta a fazer login)
    public void activate() {
        requireInactive();
        this.active = true;
    }

    private void requireInactive() {
        if (active) {
            throw new ConflictException("O usuário já está ativo.");
        }
    }

    // Inativar duas vezes não é uma operação válida: 409 avisa que o estado já era esse
    private void requireActive() {
        if (!active) {
            throw new ConflictException("O usuário já está inativo.");
        }
    }

    private void requireNotDefaultAdmin() {
        if (defaultAdmin) {
            throw new ConflictException("O administrador padrão não pode ser inativado.");
        }
    }

    // Só recusa se o perfil MUDAR: o PUT manda o estado completo, e corrigir o e-mail do
    // admin padrão (mantendo ADMIN) precisa continuar funcionando
    private void requireRoleUnchangedIfDefaultAdmin(Role newRole) {
        if (defaultAdmin && newRole != role) {
            throw new ConflictException("O perfil do administrador padrão não pode ser alterado.");
        }
    }

    // Invariantes mínimas do usuário. Formato do e-mail fica no DTO (@Email)
    private static void validateEmailAndRole(String email, Role role) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("O e-mail é obrigatório.");
        }
        if (role == null) {
            throw new BusinessRuleException("O perfil do usuário é obrigatório.");
        }
    }

    // A entidade só recebe o hash, então só confere que ele existe. O tamanho mínimo
    // só existe na senha digitada e fica no DTO.
    private static void validateEncodedPassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new BusinessRuleException("A senha é obrigatória.");
        }
    }
}
