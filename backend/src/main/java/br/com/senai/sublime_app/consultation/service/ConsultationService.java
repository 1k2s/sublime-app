package br.com.senai.sublime_app.consultation.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ContractSessionBalanceResponseDTO;
import br.com.senai.sublime_app.consultation.dto.EligibleContractResponseDTO;
import br.com.senai.sublime_app.consultation.repository.ConsultationRepository;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemEntity;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.provider.repository.ProviderRepository;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

@Service
public class ConsultationService {

    // Caso 2 no Pilates em Grupo: aula experimental, sempre pelo preço de 1x por semana
    private static final int TRIAL_CLASS_WEEKLY_FREQUENCY = 1;

    private final ConsultationRepository consultationRepository;
    private final ContractRepository contractRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;

    public ConsultationService(ConsultationRepository consultationRepository,
            ContractRepository contractRepository,
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            TechniqueRepository techniqueRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository) {
        this.consultationRepository = consultationRepository;
        this.contractRepository = contractRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.techniqueRepository = techniqueRepository;
        this.sessionDurationPriceRepository = sessionDurationPriceRepository;
        this.sessionFrequencyPriceRepository = sessionFrequencyPriceRepository;
    }

    /**
     * Lança um atendimento. O service faz o que depende do banco: busca os registros,
     * confere o contrato escolhido entre os vinculados ao paciente na data e resolve
     * qual linha de preço usar (casos 1 a 3). A entidade valida a coerência e calcula
     * o caso do preço, o valor, a comissão e o repasse.
     */
    @Transactional
    public ConsultationResponseDTO create(ConsultationRequestDTO dto) {
        PatientEntity patient = getPatientOrThrow(dto.patientId());
        ProviderEntity provider = getProviderOrThrow(dto.providerId());
        TechniqueEntity technique = getTechniqueOrThrow(dto.techniqueId());

        ContractEntity contract = resolveContract(dto.contractId(), patient, dto.occurredAt());
        PriceLine price = resolvePrice(contract, technique, dto.durationMinutes());

        ConsultationEntity consultation = new ConsultationEntity(patient, contract, provider, technique,
                dto.durationMinutes(), dto.occurredAt(), dto.status(), price.durationPrice(), price.frequencyPrice());
        consultationRepository.save(consultation);
        return ConsultationResponseDTO.fromEntity(consultation, remainingSessionsAfter(consultation));
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> search(Long providerId, Long patientId, LocalDate from, LocalDate to) {
        return consultationRepository.search(providerId, patientId, from, to).stream()
                .map(ConsultationResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationResponseDTO findById(Long id) {
        return ConsultationResponseDTO.fromEntity(getConsultationOrThrow(id));
    }

    // Soft delete: o lançamento nunca é editado; para corrigir, inativa e lança de
    // novo. Inativo não conta no saldo nem no repasse. Gravado pelo dirty checking.
    @Transactional
    public void deactivate(Long id) {
        getConsultationOrThrow(id).deactivate();
    }

    /**
     * Contratos vinculados ao paciente na data do atendimento, para o modal do
     * lançamento (nada é gravado): cada um com o caso do preço que ele daria e o saldo
     * do item do grupo da técnica, quando houver.
     */
    @Transactional(readOnly = true)
    public List<EligibleContractResponseDTO> findEligibleContracts(Long patientId, Long techniqueId,
            LocalDate occurredAt) {
        PatientEntity patient = getPatientOrThrow(patientId);
        TechniqueEntity technique = getTechniqueOrThrow(techniqueId);
        return contractRepository.findActiveCoveringDateByHolderOrBeneficiary(patient.getId(), occurredAt).stream()
                .map(contract -> toEligibleContract(contract, patient, technique.getPricingGroup()))
                .toList();
    }

    /**
     * Saldo de sessões de cada item por duração do contrato, somando toda a cadeia de
     * versões até a versão informada (o frontend usa a versão ativa).
     */
    @Transactional(readOnly = true)
    public List<ContractSessionBalanceResponseDTO> findSessionBalance(Long contractId) {
        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com id: " + contractId));
        return contract.getItems().stream()
                .filter(ContractItemEntity::isDurationBased)
                .map(item -> ContractSessionBalanceResponseDTO.fromItem(item,
                        contract.getPlan().getSessionCount(), usedSessionsOf(contract, item.getPricingGroup())))
                .toList();
    }

    // ---------------------------------------------------------------------------
    // Contrato do atendimento (ver domain-model.md, Consultation, "Contrato do atendimento")
    // ---------------------------------------------------------------------------

    // Sem contrato vinculado na data → Avulso (contrato nulo). Com algum vinculado, o
    // contrato é obrigatório e precisa ser um deles: com contrato, o atendimento nunca
    // vira Avulso. A escolha entre os dois (titular e beneficiário) é livre.
    private ContractEntity resolveContract(Long contractId, PatientEntity patient, LocalDate occurredAt) {
        List<ContractEntity> linked = contractRepository.findActiveCoveringDateByHolderOrBeneficiary(
                patient.getId(), occurredAt);
        if (contractId == null) {
            if (!linked.isEmpty()) {
                throw new BusinessRuleException(
                        "O paciente tem contrato vigente na data do atendimento: informe de qual contrato sai a sessão (contractId).");
            }
            return null;
        }
        return linked.stream()
                .filter(contract -> contract.getId().equals(contractId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "O contrato informado não está vinculado ao paciente na data do atendimento (precisa ser a versão ativa, cobrir a data e ter o paciente como titular ou beneficiário)."));
    }

    private EligibleContractResponseDTO toEligibleContract(ContractEntity contract, PatientEntity patient,
            PricingGroupEntity pricingGroup) {
        Integer remainingSessions = contract.findItemOf(pricingGroup)
                .filter(ContractItemEntity::isDurationBased)
                .map(item -> remainingSessionsOf(contract, item))
                .orElse(null);
        return new EligibleContractResponseDTO(
                contract.getId(),
                contract.getPatient().getId(),
                contract.getPatient().getName(),
                contract.getPatient().getId().equals(patient.getId()),
                contract.getPlan().getName(),
                contract.getEndDate(),
                ConsultationEntity.pricingSourceFor(contract, pricingGroup),
                remainingSessions);
    }

    // ---------------------------------------------------------------------------
    // Resolução de preço (ver domain-model.md, Consultation, "Resolução de preço"). Linha
    // "atual" = vigente hoje (validTo nulo), mesmo em lançamento retroativo.
    // ---------------------------------------------------------------------------

    private PriceLine resolvePrice(ContractEntity contract, TechniqueEntity technique, int durationMinutes) {
        if (contract == null) {
            return singleSessionPrice(technique, durationMinutes);
        }
        return contract.findItemOf(technique.getPricingGroup())
                .map(item -> contractItemPrice(item, contract.getPlan(), technique, durationMinutes))
                .orElseGet(() -> equivalentPlanPrice(contract.getPlan(), technique, durationMinutes));
    }

    // Caso 1: o contrato cobre o grupo. Pilates em Grupo → linha travada. Por duração:
    // mesma duração da linha travada → travada; outra → linha atual do plano do
    // contrato na duração do atendimento.
    private PriceLine contractItemPrice(ContractItemEntity item, PlanEntity contractPlan, TechniqueEntity technique,
            int durationMinutes) {
        if (!item.isDurationBased()) {
            return PriceLine.ofFrequency(item.getSessionFrequencyPrice());
        }
        SessionDurationPriceEntity locked = item.getSessionDurationPrice();
        if (locked.getDurationMinutes() == durationMinutes) {
            return PriceLine.ofDuration(locked);
        }
        return PriceLine.ofDuration(currentDurationPrice(technique, contractPlan, durationMinutes));
    }

    // Caso 2: o contrato não cobre o grupo → de-para a partir do plano do contrato
    // (planFor: o próprio plano nos grupos por duração; o equivalente na outra
    // família). No Pilates em Grupo, sempre o preço de 1x por semana (aula experimental).
    private PriceLine equivalentPlanPrice(PlanEntity contractPlan, TechniqueEntity technique, int durationMinutes) {
        PricingModel pricingModel = technique.getPricingGroup().getPricingModel();
        PlanEntity plan = contractPlan.planFor(pricingModel);
        if (pricingModel == PricingModel.FREQUENCY_BASED) {
            return PriceLine.ofFrequency(currentFrequencyPrice(technique, plan, TRIAL_CLASS_WEEKLY_FREQUENCY));
        }
        return PriceLine.ofDuration(currentDurationPrice(technique, plan, durationMinutes));
    }

    // Caso 3: sem contrato → linha atual do Avulso (único plano SINGLE_SESSION ativo).
    // O Pilates em Grupo não tem avulso: exige contrato.
    private PriceLine singleSessionPrice(TechniqueEntity technique, int durationMinutes) {
        PricingGroupEntity pricingGroup = technique.getPricingGroup();
        if (pricingGroup.getPricingModel() == PricingModel.FREQUENCY_BASED) {
            throw new BusinessRuleException(
                    technique.getName() + " exige contrato: não há preço avulso para o grupo " + pricingGroup.getName() + ".");
        }
        return PriceLine.ofDuration(sessionDurationPriceRepository
                .findByPricingGroupIdAndPlanTypeAndPlanActiveTrueAndDurationMinutesAndValidToIsNull(
                        pricingGroup.getId(), PlanType.SINGLE_SESSION, durationMinutes)
                .orElseThrow(() -> new BusinessRuleException(
                        "Não há preço avulso para " + technique.getName() + " " + durationMinutes + " min.")));
    }

    // Sem linha correspondente → 400: nunca se "chuta" um preço aproximado (o valor
    // vira obrigação de repasse na hora, sem aprovação)
    private SessionDurationPriceEntity currentDurationPrice(TechniqueEntity technique, PlanEntity plan,
            int durationMinutes) {
        return sessionDurationPriceRepository
                .findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull(
                        technique.getPricingGroup().getId(), plan.getId(), durationMinutes)
                .orElseThrow(() -> new BusinessRuleException("Não há preço do plano " + plan.getName()
                        + " para " + technique.getName() + " " + durationMinutes + " min."));
    }

    private SessionFrequencyPriceEntity currentFrequencyPrice(TechniqueEntity technique, PlanEntity plan,
            int weeklyFrequency) {
        return sessionFrequencyPriceRepository
                .findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                        technique.getPricingGroup().getId(), plan.getId(), weeklyFrequency)
                .orElseThrow(() -> new BusinessRuleException("Não há preço do plano " + plan.getName()
                        + " para " + technique.getName() + " " + weeklyFrequency + "x por semana."));
    }

    // A linha de preço resolvida: por duração OU por frequência (exclusive arc)
    private record PriceLine(SessionDurationPriceEntity durationPrice, SessionFrequencyPriceEntity frequencyPrice) {

        static PriceLine ofDuration(SessionDurationPriceEntity durationPrice) {
            return new PriceLine(durationPrice, null);
        }

        static PriceLine ofFrequency(SessionFrequencyPriceEntity frequencyPrice) {
            return new PriceLine(null, frequencyPrice);
        }
    }

    // ---------------------------------------------------------------------------
    // Saldo de sessões (ver domain-model.md, Consultation, "Saldo de sessões")
    // ---------------------------------------------------------------------------

    // Saldo do item depois do lançamento: só quando o atendimento debitou um item por
    // duração (o Grupo não tem saldo; de-para e Avulso não debitam)
    private Integer remainingSessionsAfter(ConsultationEntity consultation) {
        if (!consultation.getPricingSource().debitsContractItem() || consultation.getSessionDurationPrice() == null) {
            return null;
        }
        ContractEntity contract = consultation.getContract();
        return contract.findItemOf(consultation.getTechnique().getPricingGroup())
                .map(item -> remainingSessionsOf(contract, item))
                .orElse(null);
    }

    // Sessões do plano − sessões usadas na cadeia toda. Pode ficar negativo.
    private int remainingSessionsOf(ContractEntity contract, ContractItemEntity item) {
        return contract.getPlan().getSessionCount() - usedSessionsOf(contract, item.getPricingGroup());
    }

    // Atendimentos ativos que debitaram o item (contrato + grupo) em qualquer versão
    // da cadeia. long → int: é uma contagem de sessões de um contrato (dezenas).
    private int usedSessionsOf(ContractEntity contract, PricingGroupEntity pricingGroup) {
        return (int) consultationRepository.countByContractIdInAndPricingSourceAndActiveTrueAndSessionDurationPricePricingGroupId(
                contract.getVersionChainIds(), PricingSource.CONTRACT_ITEM, pricingGroup.getId());
    }

    // ---------------------------------------------------------------------------
    // Buscas (404 se o id não existir)
    // ---------------------------------------------------------------------------

    private ConsultationEntity getConsultationOrThrow(Long id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado com id: " + id));
    }

    private PatientEntity getPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com id: " + id));
    }

    private ProviderEntity getProviderOrThrow(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado com id: " + id));
    }

    private TechniqueEntity getTechniqueOrThrow(Long id) {
        return techniqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnica não encontrada com id: " + id));
    }
}
