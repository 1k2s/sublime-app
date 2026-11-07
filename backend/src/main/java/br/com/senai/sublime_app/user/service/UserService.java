package br.com.senai.sublime_app.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.dto.UserUpdateRequestDTO;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    // Interface do Spring Security (o bean BCrypt é declarado no módulo auth): o user
    // faz o hash sem importar nada do auth
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Unicidade do e-mail depende do banco (fica aqui); as regras do usuário ficam
    // na UserEntity. A senha criada pelo admin nasce como provisória (FIRST_ACCESS: + hash)
    // para que o próprio titular defina sua senha no primeiro acesso (ADR 0025).
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        UserEntity user = UserEntity.withProvisionalPassword(
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.role());
        userRepository.save(user);
        return UserResponseDTO.fromEntity(user);
    }

    // Criação do primeiro admin ao subir a aplicação (AdminInitializer): já nasce com
    // senha definitiva (sem o prefixo FIRST_ACCESS:), pois a própria credencial vem do
    // ambiente da clínica (ADR 0023, ADR 0025).
    @Transactional
    public UserResponseDTO createDefinitiveAdmin(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        UserEntity user = new UserEntity(dto.email(), passwordEncoder.encode(dto.password()), dto.role());
        userRepository.save(user);
        return UserResponseDTO.fromEntity(user);
    }

    /**
     * Lista todos os usuários, ativos e inativos.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca um usuário por ID, mesmo que esteja inativo.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        return UserResponseDTO.fromEntity(getUserOrThrow(id));
    }

    /**
     * Atualiza e-mail e perfil e, se a requisição trouxer uma senha nova, redefine a senha
     * como provisória (FIRST_ACCESS: + hash), exigindo que o titular escolha sua própria
     * senha no próximo login. Sem senha, mantém a atual. Gravado pelo dirty checking.
     */
    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO dto) {
        UserEntity user = getUserOrThrow(id);

        if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        user.update(dto.email(), dto.role());
        if (dto.hasNewPassword()) {
            user.changeToProvisionalPassword(passwordEncoder.encode(dto.password()));
        }
        return UserResponseDTO.fromEntity(user);
    }

    /**
     * Conclui o primeiro acesso trocando a senha provisória por uma senha definitiva
     * escolhida pelo próprio usuário (ADR 0025). Exige que a nova senha seja diferente da
     * senha provisória.
     */
    @Transactional
    public void completeFirstAccess(Long id, String newPassword) {
        UserEntity user = getUserOrThrow(id);
        if (passwordEncoder.matches(newPassword, user.extractPasswordHash())) {
            throw new BusinessRuleException("A nova senha deve ser diferente da senha provisória.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    /**
     * Soft delete: o usuário é desativado, não removido do banco, porque pode estar
     * vinculado a um prestador com atendimentos lançados. Gravado pelo dirty checking.
     */
    @Transactional
    public void deactivate(Long id) {
        UserEntity user = getUserOrThrow(id);
        user.deactivate();
    }

    // Existe algum usuário ativo com este perfil? (primeiro admin ao subir a aplicação)
    @Transactional(readOnly = true)
    public boolean existsActiveUserWithRole(Role role) {
        return userRepository.existsByRoleAndActiveTrue(role);
    }

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private UserEntity getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }
}
