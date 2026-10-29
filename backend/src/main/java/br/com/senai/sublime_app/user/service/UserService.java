package br.com.senai.sublime_app.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Unicidade do e-mail depende do banco (fica aqui); as regras do usuário ficam
    // no construtor da UserEntity
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        UserEntity user = new UserEntity(dto.email(), dto.password(), dto.role());
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
     * Atualiza e-mail, senha e perfil pelo método de domínio. Gravado pelo dirty
     * checking do Hibernate, pois a entidade foi carregada nesta transação.
     */
    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        UserEntity user = getUserOrThrow(id);

        if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new ConflictException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        user.update(dto.email(), dto.password(), dto.role());
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

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private UserEntity getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }
}
