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

    // Prefixo aplicado ao hash BCrypt quando a senha é provisória e exige troca no
    // primeiro login, sem adicionar nova coluna/atributo na entidade (ADR 0025)
    public static final String FIRST_ACCESS_PREFIX = "FIRST_ACCESS:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Sempre o hash (BCrypt), nunca a senha digitada: quem faz o hash é o UserService.
    // Quando provisória, começa com FIRST_ACCESS_PREFIX (ADR 0025).
    @Column(nullable = false)
    private String password;

    // varchar, não o enum nativo do MySQL: valor novo no enum não exige migration (ADR 0021)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor público para criar um usuário com senha já definitiva (ex: AdminInitializer).
    // Recebe a senha já com hash (encodedPassword).
    public UserEntity(String email, String encodedPassword, Role role) {
        validateEmailAndRole(email, role);
        validateEncodedPassword(encodedPassword);
        this.email = email;
        this.password = encodedPassword;
        this.role = role;
        this.active = true;
    }

    // Fábrica para usuários criados pelo administrador (POST /api/users), que nascem com
    // senha provisória e precisam definir a própria senha no primeiro acesso (ADR 0025).
    public static UserEntity withProvisionalPassword(String email, String encodedPassword, Role role) {
        validateEmailAndRole(email, role);
        validateEncodedPassword(encodedPassword);
        return new UserEntity(email, FIRST_ACCESS_PREFIX + encodedPassword, role);
    }

    // Alteração dos dados de acesso, sem a senha (ver changePassword)
    public void update(String email, Role role) {
        validateEmailAndRole(email, role);
        this.email = email;
        this.role = role;
    }

    // Troca para uma senha definitiva (conclusão do primeiro acesso pelo próprio usuário)
    public void changePassword(String encodedPassword) {
        validateEncodedPassword(encodedPassword);
        this.password = encodedPassword;
    }

    // Redefinição de senha pelo administrador (PUT /api/users/{id}): grava como provisória
    // para que o usuário defina sua própria senha pessoal no próximo login (ADR 0025)
    public void changeToProvisionalPassword(String encodedPassword) {
        validateEncodedPassword(encodedPassword);
        this.password = FIRST_ACCESS_PREFIX + encodedPassword;
    }

    // Indica se o usuário ainda está com senha provisória pendente de troca
    public boolean isFirstAccessPending() {
        return password != null && password.startsWith(FIRST_ACCESS_PREFIX);
    }

    // Devolve apenas o hash BCrypt puro (sem o prefixo FIRST_ACCESS:), para conferência
    // pelo PasswordEncoder
    public String extractPasswordHash() {
        return isFirstAccessPending()
                ? password.substring(FIRST_ACCESS_PREFIX.length())
                : password;
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
