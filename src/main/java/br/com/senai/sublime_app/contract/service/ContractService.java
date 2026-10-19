package br.com.senai.sublime_app.contract.service;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemDraft;
import br.com.senai.sublime_app.contract.domain.ContractItemEntity;
import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractItemRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.dto.ContractTermPreviewResponseDTO;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
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
    private final PlanRepository planRepository;
    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;

    public ContractService(ContractRepository contractRepository,
            PatientRepository patientRepository,
            PlanRepository planRepository,
            TechniqueRepository techniqueRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository) {
        this.contractRepository = contractRepository;
        this.patientRepository = patientRepository;
        this.planRepository = planRepository;
        this.techniqueRepository = techniqueRepository;
        this.sessionDurationPriceRepository = sessionDurationPriceRepository;
        this.sessionFrequencyPriceRepository = sessionFrequencyPriceRepository;
    }

    /**
     * Cria um novo contrato. O service só orquestra: aplica as regras que dependem
     * do banco (contrato vigente, opções do plano do Grupo), carrega as entidades e
     * delega a criação ao ContractEntity, que concentra as regras do contrato e dos
     * itens. O formato do request (exclusive arc e frequência por item) já foi
     * validado no DTO.
     */
    @Transactional
    public ContractResponseDTO create(ContractRequestDTO dto) {

        // Regra que depende de outros registros no banco: fica no service.
        // Contrato ativo porém vencido não bloqueia um novo.
        if (contractRepository.existsByPatientIdAndActiveTrueAndEndDateGreaterThanEqual(
                dto.patientId(), LocalDate.now())) {
            throw new ConflictException("O paciente já possui um contrato vigente.");
        }

        PatientEntity patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com id: " + dto.patientId()));
        PatientEntity beneficiary = findBeneficiary(dto.beneficiaryId());
        PlanEntity plan = getPlanOrThrow(dto.planId());

        ContractEntity contract = ContractEntity.create(patient, beneficiary, plan,
                dto.startDate(), dto.paymentMethod(), toDrafts(dto.items()));
        validateGroupItemPlan(contract, null);

        // Os itens são salvos junto (cascade do agregado)
        contractRepository.save(contract);
        return ContractResponseDTO.fromEntity(contract);
    }

    /**
     * Gera uma nova versão do contrato (aditivo). O contrato nunca é editado no
     * lugar: a entidade cria a nova versão com as mesmas regras da criação (titular,
     * plano e início copiados), aponta para a atual (previousContract) e inativa a
     * atual. A regra de "contrato vigente" não se aplica aqui — a versão atual está
     * sendo substituída, não duplicada.
     */
    @Transactional
    public ContractResponseDTO amend(Long id, ContractAmendmentRequestDTO dto) {
        ContractEntity current = getContractOrThrow(id);
        PatientEntity beneficiary = findBeneficiary(dto.beneficiaryId());

        ContractEntity newVersion = current.amend(beneficiary, dto.paymentMethod(), dto.endDate(),
                toDrafts(dto.items()), LocalDate.now());
        validateGroupItemPlan(newVersion, current);

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
        return ContractResponseDTO.fromEntity(getContractOrThrow(id));
    }

    // Soft delete: a versão é inativada, nunca removida (o histórico de versões e
    // os atendimentos continuam apontando para ela). Gravado pelo dirty checking.
    @Transactional
    public void deactivate(Long id) {
        ContractEntity contract = getContractOrThrow(id);
        contract.deactivate();
    }

    /**
     * Prévia da tela de criação (nada é gravado): vigência calculada pelo plano e
     * pelos ritmos dos itens por duração — a mesma conta da entidade — e as opções do
     * plano do item do Pilates em Grupo.
     */
    @Transactional(readOnly = true)
    public ContractTermPreviewResponseDTO previewTerm(Long planId, LocalDate startDate,
            List<SessionFrequency> durationItemFrequencies) {
        PlanEntity plan = getPlanOrThrow(planId);
        int weeks = ContractEntity.durationWeeksFor(plan, durationItemFrequencies);
        return toTermPreview(plan, weeks, ContractEntity.endDateFor(startDate, weeks));
    }

    /**
     * Opções do plano do item do Pilates em Grupo num aditivo, pela vigência atual do
     * contrato (nada é gravado).
     */
    @Transactional(readOnly = true)
    public ContractTermPreviewResponseDTO groupPlanOptionsFor(Long contractId) {
        ContractEntity contract = getContractOrThrow(contractId);
        return toTermPreview(contract.getPlan(), contract.getDurationInWeeks(), contract.getEndDate());
    }

    // ---------------------------------------------------------------------------
    // Plano do item do Pilates em Grupo — regra que depende do catálogo de planos
    // (ver domain-model.md, "Plano do item do Grupo")
    // ---------------------------------------------------------------------------

    // Num contrato de pacote, o plano da linha do item do Grupo precisa estar nas
    // opções. No aditivo, item do Grupo sem mudança de linha é aceito como está.
    // Contrato só com Grupo (plano de mensalidade): a entidade já exige que a linha
    // seja do plano do contrato.
    private void validateGroupItemPlan(ContractEntity contract, ContractEntity previous) {
        if (contract.getPlan().getType() != PlanType.PACKAGE) {
            return;
        }
        contract.getItems().stream()
                .filter(item -> !item.isDurationBased())
                .filter(groupItem -> !isUnchangedFromPrevious(groupItem, previous))
                .findFirst()
                .ifPresent(groupItem -> validatePlanInOptions(
                        groupItem.getSessionFrequencyPrice().getPlan(),
                        groupPlanOptionsFor(contract.getPlan(), contract.getDurationInWeeks())));
    }

    private static boolean isUnchangedFromPrevious(ContractItemEntity groupItem, ContractEntity previous) {
        if (previous == null) {
            return false;
        }
        return previous.findItemOf(groupItem.getPricingGroup())
                .map(previousItem -> previousItem.getSessionFrequencyPrice() != null
                        && previousItem.getSessionFrequencyPrice().getId().equals(groupItem.getSessionFrequencyPrice().getId()))
                .orElse(false);
    }

    private static void validatePlanInOptions(PlanEntity groupPlan, List<PlanEntity> options) {
        boolean allowed = options.stream().anyMatch(option -> option.getId().equals(groupPlan.getId()));
        if (!allowed) {
            String names = options.stream().map(PlanEntity::getName).collect(Collectors.joining(", "));
            throw new BusinessRuleException(
                    "Para a vigência deste contrato, o plano do Pilates em Grupo precisa ser um destes: " + names + ".");
        }
    }

    // Contrato só com Grupo: a única opção é o próprio plano do contrato. Contrato de
    // pacote: mensalidades ativas até a primeira que cubra a vigência (inclui as de
    // mesmo prazo, ex: Semestral e Semestral Familiar). Se nenhuma cobrir (vigência
    // estendida por prorrogação), todas valem.
    private List<PlanEntity> groupPlanOptionsFor(PlanEntity contractPlan, int durationWeeks) {
        if (contractPlan.getType() == PlanType.MONTHLY_FEE) {
            return List.of(contractPlan);
        }
        List<PlanEntity> monthlyFees = planRepository.findByTypeAndActiveTrueOrderByTermWeeksAsc(PlanType.MONTHLY_FEE);
        int coveringTermWeeks = monthlyFees.stream()
                .mapToInt(PlanEntity::getTermWeeks)
                .filter(termWeeks -> termWeeks >= durationWeeks)
                .findFirst()
                .orElse(Integer.MAX_VALUE);
        return monthlyFees.stream()
                .filter(plan -> plan.getTermWeeks() <= coveringTermWeeks)
                .toList();
    }

    // Sugestão: o equivalente do de-para (contrato de pacote) ou o próprio plano
    // (contrato só com Grupo), desde que esteja nas opções
    private ContractTermPreviewResponseDTO toTermPreview(PlanEntity contractPlan, int weeks, LocalDate endDate) {
        List<PlanEntity> options = groupPlanOptionsFor(contractPlan, weeks);
        PlanEntity suggestion = contractPlan.getType() == PlanType.MONTHLY_FEE
                ? contractPlan
                : contractPlan.getEquivalentPlan();
        Long suggestedId = suggestion != null && options.stream().anyMatch(option -> option.getId().equals(suggestion.getId()))
                ? suggestion.getId()
                : null;
        return new ContractTermPreviewResponseDTO(weeks, endDate, suggestedId,
                options.stream().map(PlanResponseDTO::fromEntity).toList());
    }

    // ---------------------------------------------------------------------------
    // Buscas (404 se o id não existir)
    // ---------------------------------------------------------------------------

    // Centraliza a busca + erro de "não encontrado", usada por amend, findById, deactivate e opções
    private ContractEntity getContractOrThrow(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com id: " + id));
    }

    private PlanEntity getPlanOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado com id: " + id));
    }

    private PatientEntity findBeneficiary(Long beneficiaryId) {
        if (beneficiaryId == null) {
            return null;
        }
        return patientRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiário não encontrado com id: " + beneficiaryId));
    }

    // Monta os rascunhos dos itens com as entidades carregadas (técnica e linha de
    // preço). Quem cria os itens é o ContractEntity.
    private List<ContractItemDraft> toDrafts(List<ContractItemRequestDTO> items) {
        return items.stream().map(this::toDraft).toList();
    }

    private ContractItemDraft toDraft(ContractItemRequestDTO item) {
        TechniqueEntity technique = findTechnique(item.techniqueId());
        if (item.sessionDurationPriceId() != null) {
            return ContractItemDraft.withDurationPrice(technique,
                    findDurationPrice(item.sessionDurationPriceId()), item.frequency());
        }
        return ContractItemDraft.withFrequencyPrice(technique, findFrequencyPrice(item.sessionFrequencyPriceId()));
    }

    private TechniqueEntity findTechnique(Long techniqueId) {
        return techniqueRepository.findById(techniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Técnica não encontrada com id: " + techniqueId));
    }

    private SessionDurationPriceEntity findDurationPrice(Long priceId) {
        return sessionDurationPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preço por duração não encontrado com id: " + priceId));
    }

    private SessionFrequencyPriceEntity findFrequencyPrice(Long priceId) {
        return sessionFrequencyPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preço por frequência não encontrado com id: " + priceId));
    }
}
