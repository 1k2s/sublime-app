package br.com.senai.sublime_app.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // no construtor da UserEntity. A senha é gravada com hash, nunca em texto puro.
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
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
     * Atualiza e-mail e perfil e, se a requisição trouxer uma senha nova, troca a senha
     * (com hash). Sem senha, mantém a atual. Gravado pelo dirty checking do Hibernate.
     */
    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO dto) {
        UserEntity user = getUserOrThrow(id);

        if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        user.update(dto.email(), dto.role());
        if (dto.hasNewPassword()) {
            user.changePassword(passwordEncoder.encode(dto.password()));
        }
        return UserResponseDTO.fromEntity(user);
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
