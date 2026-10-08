package br.com.senai.sublime_app.provider.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.provider.domain.Percentage;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.provider.dto.ProviderRequestDTO;
import br.com.senai.sublime_app.provider.dto.ProviderResponseDTO;
import br.com.senai.sublime_app.provider.dto.ProviderUpdateRequestDTO;
import br.com.senai.sublime_app.provider.repository.ProviderRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    public ProviderService(ProviderRepository providerRepository, UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Cria um novo prestador de serviço vinculado a um usuário existente:
     * 1. Valida se o usuário informado no DTO realmente existe no banco.
     * 2. Garante a regra de negócio de unicidade (1 User para no máximo 1 Provider).
     * 3. Cria a entidade pelo construtor de domínio (mantendo o encapsulamento).
     * 4. Persiste no banco e retorna o DTO de resposta.
     */
    @Transactional
    public ProviderResponseDTO create(ProviderRequestDTO dto) {
        UserEntity user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + dto.userId()));

        if (providerRepository.existsByUserId(dto.userId())) {
            throw new ConflictException("Já existe um prestador vinculado ao usuário de id: " + dto.userId());
        }

        ProviderEntity provider = new ProviderEntity(user, dto.name(), Percentage.of(dto.commissionPercentage()));
        providerRepository.save(provider);
        return ProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Lista todos os prestadores cadastrados, ativos e inativos.
     */
    @Transactional(readOnly = true)
    public List<ProviderResponseDTO> findAll() {
        return providerRepository.findAll().stream()
                .map(ProviderResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca um prestador por ID, mesmo que esteja inativo.
     */
    @Transactional(readOnly = true)
    public ProviderResponseDTO findById(Long id) {
        return ProviderResponseDTO.fromEntity(getProviderOrThrow(id));
    }

    /**
     * Atualiza os dados de negócio do prestador (nome e comissão) pelo método de
     * domínio. O vínculo com o usuário é fixo: o DTO de alteração nem o recebe.
     * Gravado pelo dirty checking do Hibernate.
     */
    @Transactional
    public ProviderResponseDTO update(Long id, ProviderUpdateRequestDTO dto) {
        ProviderEntity provider = getProviderOrThrow(id);
        provider.update(dto.name(), Percentage.of(dto.commissionPercentage()));
        return ProviderResponseDTO.fromEntity(provider);
    }

    /**
     * Soft delete: o prestador é desativado, não removido do banco, para preservar
     * o histórico de atendimentos que o referenciam. Gravado pelo dirty checking.
     */
    @Transactional
    public void deactivate(Long id) {
        ProviderEntity provider = getProviderOrThrow(id);
        provider.deactivate();
    }

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private ProviderEntity getProviderOrThrow(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado com id: " + id));
    }
}
