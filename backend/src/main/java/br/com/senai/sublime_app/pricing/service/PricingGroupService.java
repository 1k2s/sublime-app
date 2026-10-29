package br.com.senai.sublime_app.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.dto.PricingGroupRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PricingGroupResponseDTO;
import br.com.senai.sublime_app.pricing.repository.PricingGroupRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;

@Service
public class PricingGroupService {

    private final PricingGroupRepository pricingGroupRepository;

    public PricingGroupService(PricingGroupRepository pricingGroupRepository) {
        this.pricingGroupRepository = pricingGroupRepository;
    }

    // Unicidade do nome depende do banco (fica aqui); as regras do grupo ficam
    // no construtor da PricingGroupEntity
    @Transactional
    public PricingGroupResponseDTO create(PricingGroupRequestDTO dto) {
        if (pricingGroupRepository.existsByName(dto.name())) {
            throw new ConflictException("Já existe um grupo de preço cadastrado com o nome: " + dto.name());
        }

        PricingGroupEntity pricingGroup = new PricingGroupEntity(dto.name(), dto.pricingModel());
        pricingGroupRepository.save(pricingGroup);
        return PricingGroupResponseDTO.fromEntity(pricingGroup);
    }

    @Transactional(readOnly = true)
    public List<PricingGroupResponseDTO> findAll() {
        return pricingGroupRepository.findAll().stream()
                .map(PricingGroupResponseDTO::fromEntity)
                .toList();
    }
}
