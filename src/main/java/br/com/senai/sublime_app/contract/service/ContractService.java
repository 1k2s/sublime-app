package br.com.senai.sublime_app.contract.service;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final PatientRepository patientRepository;
    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;

    public ContractService(ContractRepository contractRepository,
            PatientRepository patientRepository,
            TechniqueRepository techniqueRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository) {
        this.contractRepository = contractRepository;
        this.patientRepository = patientRepository;
        this.techniqueRepository = techniqueRepository;
        this.sessionDurationPriceRepository = sessionDurationPriceRepository;
        this.sessionFrequencyPriceRepository = sessionFrequencyPriceRepository;
    }

    /**
     * Cria um novo contrato. O service só orquestra: aplica a regra que depende
     * do banco (contrato vigente), carrega as entidades e delega a criação às
     * fábricas da ContractEntity, que concentram as regras de negócio do contrato.
     * O formato do request (exclusive arc, weeklyFrequency) já foi validado no DTO.
     */
    @Transactional
    public ContractResponseDTO create(ContractRequestDTO dto) {

        // Regra que depende de outros registros no banco: fica no service.
        // Contrato ativo porém vencido não bloqueia um novo.
        if (contractRepository.existsByPatientIdAndActiveTrueAndEndDateGreaterThanEqual(
                dto.patientId(), LocalDate.now())) {
            throw new ConflictException("Patient already has a current contract.");
        }

        PatientEntity patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        PatientEntity beneficiary = findBeneficiary(dto.beneficiaryId());
        TechniqueEntity technique = findTechnique(dto.techniqueId());

        // O tipo de preço informado define qual fábrica cria o contrato
        ContractEntity contract;
        if (dto.sessionDurationPriceId() != null) {
            contract = ContractEntity.withDurationPrice(patient, beneficiary, technique,
                    findDurationPrice(dto.sessionDurationPriceId()),
                    dto.weeklyFrequency(), dto.startDate(), dto.endDate(), dto.paymentMethod());
        } else {
            contract = ContractEntity.withFrequencyPrice(patient, beneficiary, technique,
                    findFrequencyPrice(dto.sessionFrequencyPriceId()),
                    dto.startDate(), dto.endDate(), dto.paymentMethod());
        }

        contractRepository.save(contract);
        return ContractResponseDTO.fromEntity(contract);
    }

    /**
     * Gera uma nova versão do contrato (aditivo). O contrato nunca é editado no
     * lugar: a entidade cria a nova versão com as mesmas regras da criação, aponta
     * para a atual (previousContract) e inativa a atual. A regra de "contrato
     * vigente" não se aplica aqui — a versão atual está sendo substituída, não
     * duplicada.
     */
    @Transactional
    public ContractResponseDTO amend(Long id, ContractAmendmentRequestDTO dto) {
        ContractEntity current = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        PatientEntity beneficiary = findBeneficiary(dto.beneficiaryId());
        TechniqueEntity technique = findTechnique(dto.techniqueId());

        ContractEntity newVersion;
        if (dto.sessionDurationPriceId() != null) {
            newVersion = current.amendWithDurationPrice(beneficiary, technique,
                    findDurationPrice(dto.sessionDurationPriceId()),
                    dto.weeklyFrequency(), dto.startDate(), dto.endDate(), dto.paymentMethod());
        } else {
            newVersion = current.amendWithFrequencyPrice(beneficiary, technique,
                    findFrequencyPrice(dto.sessionFrequencyPriceId()),
                    dto.startDate(), dto.endDate(), dto.paymentMethod());
        }

        // A versão atual (inativada) é salva pelo dirty checking do Hibernate, pois
        // foi carregada nesta transação; a nova versão precisa de save explícito.
        contractRepository.save(newVersion);
        return ContractResponseDTO.fromEntity(newVersion);
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
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        return ContractResponseDTO.fromEntity(contract);
    }

    @Transactional
    public void delete(Long id) {
        ContractEntity contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        contract.deactivate();
    }

    // Buscas compartilhadas por create e amend (404 se o id não existir)

    private PatientEntity findBeneficiary(Long beneficiaryId) {
        if (beneficiaryId == null) {
            return null;
        }
        return patientRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));
    }

    private TechniqueEntity findTechnique(Long techniqueId) {
        return techniqueRepository.findById(techniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Technique not found"));
    }

    private SessionDurationPriceEntity findDurationPrice(Long priceId) {
        return sessionDurationPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionDurationPrice not found"));
    }

    private SessionFrequencyPriceEntity findFrequencyPrice(Long priceId) {
        return sessionFrequencyPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("SessionFrequencyPrice not found"));
    }
}