package br.com.senai.sublime_app.contract.service;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.GroupPlanFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.GroupPlanPriceEntity;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.repository.GroupPlanFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.GroupPlanPriceRepository;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final PatientRepository patientRepository;
    private final TechniqueRepository techniqueRepository;
    private final PlanRepository planRepository;
    private final GroupPlanPriceRepository groupPlanPriceRepository;
    private final GroupPlanFrequencyPriceRepository groupPlanFrequencyPriceRepository;

    public ContractService(ContractRepository contractRepository,
            PatientRepository patientRepository,
            TechniqueRepository techniqueRepository,
            PlanRepository planRepository,
            GroupPlanPriceRepository groupPlanPriceRepository,
            GroupPlanFrequencyPriceRepository groupPlanFrequencyPriceRepository) {
        this.contractRepository = contractRepository;
        this.patientRepository = patientRepository;
        this.techniqueRepository = techniqueRepository;
        this.planRepository = planRepository;
        this.groupPlanPriceRepository = groupPlanPriceRepository;
        this.groupPlanFrequencyPriceRepository = groupPlanFrequencyPriceRepository;
    }

    /**
     * Cria um novo contrato aplicando as seguintes regras de negócio:
     * 
     * Exclusive Arc: o contrato referencia exatamente uma das duas tabelas
     * de preço —
     * nunca as duas, nunca nenhuma. A escolha depende do modelo de precificação
     **/
    @Transactional
    public ContractResponseDTO create(ContractRequestDTO dto) {

        // Valida exclusive arc antes de qualquer consulta ao banco (falha barata)
        if (!dto.isValidExclusiveArc()) {
            throw new IllegalArgumentException(
                    "Exactly one price reference must be provided: groupPlanPriceId or groupPlanFrequencyPriceId.");
        }

        // Garante que o paciente titular não possui contrato ativo
        contractRepository.findByPatientIdAndActiveTrue(dto.patientId()).ifPresent(c -> {
            throw new IllegalStateException("Patient already has an active contract.");
        });

        // Carrega as entidades relacionadas
        PatientEntity patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        PatientEntity beneficiary = null;
        if (dto.beneficiaryId() != null) {
            beneficiary = patientRepository.findById(dto.beneficiaryId())
                    .orElseThrow(() -> new EntityNotFoundException("Beneficiary not found"));
        }

        TechniqueEntity technique = techniqueRepository.findById(dto.techniqueId())
                .orElseThrow(() -> new EntityNotFoundException("Technique not found"));

        PlanEntity plan = planRepository.findById(dto.planId())
                .orElseThrow(() -> new EntityNotFoundException("Plan not found"));

        // Trava o preço vigente no momento da assinatura (snapshot)
        // Rejeita preços históricos (validTo != null significa que já foi encerrado)
        GroupPlanPriceEntity price = null;
        GroupPlanFrequencyPriceEntity frequencyPrice = null;

        if (dto.groupPlanPriceId() != null) {
            price = groupPlanPriceRepository.findById(dto.groupPlanPriceId())
                    .orElseThrow(() -> new EntityNotFoundException("GroupPlanPrice not found"));
            if (price.getValidTo() != null) {
                throw new IllegalArgumentException("GroupPlanPrice is no longer valid. Please use the current price.");
            }
        } else {
            frequencyPrice = groupPlanFrequencyPriceRepository.findById(dto.groupPlanFrequencyPriceId())
                    .orElseThrow(() -> new EntityNotFoundException("GroupPlanFrequencyPrice not found"));
            if (frequencyPrice.getValidTo() != null) {
                throw new IllegalArgumentException(
                        "GroupPlanFrequencyPrice is no longer valid. Please use the current price.");
            }
        }

        // O construtor de ContractEntity aplica a invariante de exclusive arc como
        // última barreira
        ContractEntity contract = new ContractEntity(
                patient, beneficiary, technique, plan,
                dto.weeklyFrequency(), dto.startDate(), dto.endDate(),
                dto.paymentMethod(), price, frequencyPrice);

        contractRepository.save(contract);
        return ContractResponseDTO.fromEntity(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> findAll() {
        return contractRepository.findAll().stream()
                .map(ContractResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO findById(Long id) {
        ContractEntity contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        return ContractResponseDTO.fromEntity(contract);
    }

    @Transactional
    public ContractResponseDTO update(Long id, ContractRequestDTO dto) {
        ContractEntity contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        // update permite ajustar apenas os campos de negociação
        // os campos de preço (exclusive arc) são imutáveis após a assinatura
        contract.update(dto.weeklyFrequency(), dto.startDate(), dto.endDate(), dto.paymentMethod());
        return ContractResponseDTO.fromEntity(contract);
    }

    @Transactional
    public void delete(Long id) {
        ContractEntity contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));
        contract.deactivate();
    }
}