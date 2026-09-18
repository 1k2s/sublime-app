package br.com.senai.sublime_app.provider.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.provider.dto.ProviderRequestDTO;
import br.com.senai.sublime_app.provider.dto.ProviderResponseDTO;
import br.com.senai.sublime_app.provider.repository.ProviderRepository;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    /**
     * Cria um novo prestador de serviço vinculado a um usuário existente:
     * 1. Valida se o usuário informado no DTO realmente existe no banco.
     * 2. Garante a regra de negócio de unicidade (1 User para no máximo 1 Provider).
     * 3. Cria a entidade pelo construtor de domínio (mantendo o encapsulamento).
     * 4. Persiste no banco e retorna o DTO de resposta.
     */
    public ProviderResponseDTO create(ProviderRequestDTO dto) {
        // Busca o usuário associado
        UserEntity user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));

        // Valida se este usuário já possui vínculo com outro prestador
        if (providerRepository.existsByUserId(dto.getUserId())) {
            throw new RuntimeException("A provider is already linked to user id: " + dto.getUserId());
        }

        // Instancia a entidade pelo construtor de negócio
        ProviderEntity provider = new ProviderEntity(user, dto.getName(), dto.getCommissionPercentage());

        // Salva no banco e converte para DTO de resposta
        ProviderEntity saved = providerRepository.save(provider);
        return toResponse(saved);
    }

    /**
     * Lista todos os prestadores cadastrados, convertendo cada entidade para seu DTO de resposta.
     */
    public List<ProviderResponseDTO> findAll() {
        return providerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca um prestador por ID e retorna seu DTO correspondente.
     */
    public ProviderResponseDTO findById(Long id) {
        ProviderEntity provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));
        return toResponse(provider);
    }

    /**
     * Atualiza os dados de negócio do prestador (nome e comissão) usando o método de domínio:
     * O vínculo com o usuário original é preservado.
     */
    public ProviderResponseDTO update(Long id, ProviderRequestDTO dto) {
        ProviderEntity provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + id));

        // Atualização via método de domínio da entidade (encapsulado)
        provider.update(dto.getName(), dto.getCommissionPercentage());

        ProviderEntity saved = providerRepository.save(provider);
        return toResponse(saved);
    }

    /**
     * Remove um prestador pelo ID.
     */
    public void delete(Long id) {
        if (!providerRepository.existsById(id)) {
            throw new RuntimeException("Provider not found with id: " + id);
        }
        providerRepository.deleteById(id);
    }

    /**
     * Converte a entidade de banco ProviderEntity no DTO de resposta público ProviderResponseDTO.
     */
    private ProviderResponseDTO toResponse(ProviderEntity provider) {
        return new ProviderResponseDTO(provider);
    }
}
