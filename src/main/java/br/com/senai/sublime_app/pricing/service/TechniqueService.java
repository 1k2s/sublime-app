package br.com.senai.sublime_app.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.dto.TechniqueRequestDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueResponseDTO;
import br.com.senai.sublime_app.pricing.repository.PricingGroupRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

@Service
public class TechniqueService {

    private final TechniqueRepository techniqueRepository;
    private final PricingGroupRepository pricingGroupRepository;

    public TechniqueService(TechniqueRepository techniqueRepository,
            PricingGroupRepository pricingGroupRepository) {
        this.techniqueRepository = techniqueRepository;
        this.pricingGroupRepository = pricingGroupRepository;
    }

    // Unicidade do nome depende do banco (fica aqui); as regras da técnica ficam
    // no construtor da TechniqueEntity
    @Transactional
    public TechniqueResponseDTO create(TechniqueRequestDTO dto) {
        if (techniqueRepository.existsByName(dto.name())) {
            throw new ConflictException("Já existe uma técnica cadastrada com o nome: " + dto.name());
        }

        PricingGroupEntity pricingGroup = pricingGroupRepository.findById(dto.pricingGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de preço não encontrado com id: " + dto.pricingGroupId()));

        TechniqueEntity technique = new TechniqueEntity(dto.name(), pricingGroup);
        techniqueRepository.save(technique);
        return TechniqueResponseDTO.fromEntity(technique);
    }

    @Transactional(readOnly = true)
    public List<TechniqueResponseDTO> findActive() {
        return techniqueRepository.findByActiveTrue().stream()
                .map(TechniqueResponseDTO::fromEntity)
                .toList();
    }

    // Soft delete: a técnica sai da listagem de novos contratos, mas contratos e
    // atendimentos antigos continuam apontando para ela. A mudança é gravada pelo
    // dirty checking do Hibernate, pois a entidade foi carregada nesta transação.
    @Transactional
    public void deactivate(Long id) {
        TechniqueEntity technique = techniqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnica não encontrada com id: " + id));
        technique.deactivate();
    }
}
