package br.com.senai.sublime_app.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.dto.UserSelfUpdateRequestDTO;
import br.com.senai.sublime_app.user.dto.UserUpdateRequestDTO;
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
        requireEmailAvailable(dto.email());

        UserEntity user = UserEntity.withProvisionalPassword(
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.role());
        userRepository.save(user);
        return UserResponseDTO.fromEntity(user);
    }

    // Admin padrão, criado na primeira subida pelo AdminInitializer (que já validou o
    // formato do e-mail e o tamanho da senha). Mesmas regras do cadastro, com a marca e a
    // senha definitiva (sem primeiro acesso: a credencial vem do ambiente da clínica;
    // ADRs 0023 e 0025).
    @Transactional
    public UserResponseDTO createDefaultAdmin(String email, String rawPassword) {
        requireEmailAvailable(email);

        UserEntity user = UserEntity.defaultAdmin(email, passwordEncoder.encode(rawPassword));
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

        requireEmailAvailableForUser(dto.email(), id);

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

    // Configurações: o próprio usuário logado
    @Transactional(readOnly = true)
    public UserResponseDTO findMe(CurrentUser currentUser) {
        return UserResponseDTO.fromEntity(getUserOrThrow(currentUser.userId()));
    }

    /**
     * Configurações: o próprio usuário troca o e-mail e, se informar, a senha, sempre
     * confirmando a senha atual. A senha é conferida antes do e-mail, para quem não a tem
     * não usar o endpoint para descobrir quais e-mails existem. Gravado pelo dirty checking.
     */
    @Transactional
    public UserResponseDTO updateMe(CurrentUser currentUser, UserSelfUpdateRequestDTO dto) {
        UserEntity user = getUserOrThrow(currentUser.userId());
        requireCurrentPassword(dto.currentPassword(), user);
        requireEmailAvailableForUser(dto.email(), user.getId());

        user.changeEmail(dto.email());
        if (dto.hasNewPassword()) {
            user.changePassword(passwordEncoder.encode(dto.newPassword()));
        }
        return UserResponseDTO.fromEntity(user);
    }

    // Reativação (ex: prestador que volta à clínica). Gravado pelo dirty checking.
    @Transactional
    public void activate(Long id) {
        UserEntity user = getUserOrThrow(id);
        user.activate();
    }

    // O admin padrão já foi criado? (primeira subida da aplicação)
    @Transactional(readOnly = true)
    public boolean existsDefaultAdmin() {
        return userRepository.existsByDefaultAdminTrue();
    }

    // 400, não 401: com 401 o front encerraria a sessão
    private void requireCurrentPassword(String currentPassword, UserEntity user) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessRuleException("Senha atual incorreta.");
        }
    }

    // E-mail único (a garantia real é o unique da coluna; aqui, a mensagem clara)
    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw emailInUse(email);
        }
    }

    // Na alteração, ignorando o próprio usuário (senão manter o e-mail seria recusado)
    private void requireEmailAvailableForUser(String email, Long userId) {
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw emailInUse(email);
        }
    }

    private static ConflictException emailInUse(String email) {
        return new ConflictException("Já existe um usuário cadastrado com o e-mail: " + email);
    }

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private UserEntity getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }
}
