package br.com.senai.sublime_app.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.dto.PlanEquivalentRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PlanRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // Unicidade do nome e do Avulso dependem do banco (ficam aqui); as regras do plano
    // (nome, coerência entre tipo e quantidades) ficam no construtor da PlanEntity
    @Transactional
    public PlanResponseDTO create(PlanRequestDTO dto) {
        if (planRepository.existsByName(dto.name())) {
            throw new ConflictException("Já existe um plano cadastrado com o nome: " + dto.name());
        }
        // Um único Avulso ativo: o lançamento sem contrato busca "o" preço avulso de
        // cada grupo; com dois, a busca ficaria ambígua
        if (dto.type() == PlanType.SINGLE_SESSION && planRepository.existsByTypeAndActiveTrue(PlanType.SINGLE_SESSION)) {
            throw new ConflictException("Já existe um plano avulso ativo. O avulso é único, com um preço em cada grupo.");
        }

        PlanEntity plan = new PlanEntity(dto.name(), dto.type(), dto.sessionCount(), dto.termWeeks());
        planRepository.save(plan);
        return PlanResponseDTO.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponseDTO> findAll() {
        return planRepository.findAll().stream()
                .map(PlanResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Define (ou troca) o equivalente do de-para deste plano. Só este lado muda:
     * o outro plano define o seu próprio equivalente (os dois lados podem diferir).
     * As regras (outra família, ativo, não ele mesmo) ficam na PlanEntity. Gravado
     * pelo dirty checking do Hibernate.
     */
    @Transactional
    public PlanResponseDTO defineEquivalent(Long id, PlanEquivalentRequestDTO dto) {
        PlanEntity plan = getPlanOrThrow(id);
        PlanEntity equivalent = planRepository.findById(dto.equivalentPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plano equivalente não encontrado com id: " + dto.equivalentPlanId()));

        plan.defineEquivalent(equivalent);
        return PlanResponseDTO.fromEntity(plan);
    }

    // Centraliza a busca + erro de "não encontrado"
    private PlanEntity getPlanOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado com id: " + id));
    }
}
