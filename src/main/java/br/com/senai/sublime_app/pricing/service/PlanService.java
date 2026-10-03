package br.com.senai.sublime_app.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.dto.PlanRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // As regras do plano (nome, sessionCount) ficam no construtor da PlanEntity
    @Transactional
    public PlanResponseDTO create(PlanRequestDTO dto) {
        PlanEntity plan = new PlanEntity(dto.name(), dto.sessionCount());
        planRepository.save(plan);
        return PlanResponseDTO.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponseDTO> findAll() {
        return planRepository.findAll().stream()
                .map(PlanResponseDTO::fromEntity)
                .toList();
    }
}
