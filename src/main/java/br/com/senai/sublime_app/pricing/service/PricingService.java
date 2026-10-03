package br.com.senai.sublime_app.pricing.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.dto.DurationPriceRequestDTO;
import br.com.senai.sublime_app.pricing.dto.FrequencyPriceRequestDTO;
import br.com.senai.sublime_app.pricing.dto.SessionPriceResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniquePriceTableResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueResponseDTO;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.pricing.repository.PricingGroupRepository;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

// Porta de entrada do módulo de preços: usado pelos endpoints de consulta e,
// futuramente, por contract e consultation para resolver preços vigentes.
@Service
public class PricingService {

    private final TechniqueRepository techniqueRepository;
    private final PricingGroupRepository pricingGroupRepository;
    private final PlanRepository planRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;

    public PricingService(TechniqueRepository techniqueRepository,
            PricingGroupRepository pricingGroupRepository,
            PlanRepository planRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository) {
        this.techniqueRepository = techniqueRepository;
        this.pricingGroupRepository = pricingGroupRepository;
        this.planRepository = planRepository;
        this.sessionDurationPriceRepository = sessionDurationPriceRepository;
        this.sessionFrequencyPriceRepository = sessionFrequencyPriceRepository;
    }

    @Transactional(readOnly = true)
    public List<TechniqueResponseDTO> findActiveTechniques() {
        return techniqueRepository.findByActiveTrue().stream()
                .map(TechniqueResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Retorna as linhas de preço vigentes do grupo da técnica, buscando só na
     * tabela correspondente ao pricingModel do grupo.
     *
     * Não bloqueia técnica inativa: a listagem de técnicas já as esconde da tela,
     * e o lançamento de atendimentos pode precisar do preço de uma técnica
     * desativada depois da assinatura do contrato.
     */
    @Transactional(readOnly = true)
    public TechniquePriceTableResponseDTO findCurrentPriceTable(Long techniqueId) {
        TechniqueEntity technique = techniqueRepository.findById(techniqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Technique not found with id: " + techniqueId));

        Long pricingGroupId = technique.getPricingGroup().getId();

        // switch com retorno obriga a tratar todos os valores do enum: um novo
        // PricingModel quebra a compilação aqui em vez de cair na tabela errada
        List<SessionPriceResponseDTO> prices = switch (technique.getPricingGroup().getPricingModel()) {
            case DURATION_BASED -> sessionDurationPriceRepository
                    .findByPricingGroupIdAndValidToIsNull(pricingGroupId).stream()
                    .map(SessionPriceResponseDTO::fromEntity)
                    .toList();
            case FREQUENCY_BASED -> sessionFrequencyPriceRepository
                    .findByPricingGroupIdAndValidToIsNull(pricingGroupId).stream()
                    .map(SessionPriceResponseDTO::fromEntity)
                    .toList();
        };

        return TechniquePriceTableResponseDTO.fromEntity(technique, prices);
    }

    /**
     * Cadastro de valor por duração — fluxo único para preço novo e reajuste:
     * se a combinação (grupo, plano, duração) tem linha vigente, ela é fechada
     * com validTo = hoje; a nova linha abre com validFrom = hoje (validTo é
     * exclusivo, então não há dia com dois preços). As regras de grupo, plano e
     * valor ficam no construtor da SessionDurationPriceEntity.
     */
    @Transactional
    public SessionPriceResponseDTO createDurationPrice(DurationPriceRequestDTO dto) {
        PricingGroupEntity pricingGroup = findPricingGroup(dto.pricingGroupId());
        PlanEntity plan = findPlan(dto.planId());
        LocalDate today = LocalDate.now();

        // saveAndFlush: grava o fechamento antes do INSERT da linha nova. O Hibernate
        // executa INSERTs antes de UPDATEs no flush; com o unique de current_flag
        // (migrations), a linha nova colidiria com a antiga ainda vigente.
        sessionDurationPriceRepository
                .findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull(
                        pricingGroup.getId(), plan.getId(), dto.durationMinutes())
                .ifPresent(current -> {
                    current.close(today);
                    sessionDurationPriceRepository.saveAndFlush(current);
                });

        SessionDurationPriceEntity price = new SessionDurationPriceEntity(
                pricingGroup, plan, dto.durationMinutes(), Money.of(dto.sessionValue()), today);
        sessionDurationPriceRepository.save(price);
        return SessionPriceResponseDTO.fromEntity(price);
    }

    /**
     * Cadastro de valor por frequência — mesmo fluxo único do preço por duração,
     * com a combinação (grupo, plano, frequência semanal).
     */
    @Transactional
    public SessionPriceResponseDTO createFrequencyPrice(FrequencyPriceRequestDTO dto) {
        PricingGroupEntity pricingGroup = findPricingGroup(dto.pricingGroupId());
        PlanEntity plan = findPlan(dto.planId());
        LocalDate today = LocalDate.now();

        // saveAndFlush: ver createDurationPrice
        sessionFrequencyPriceRepository
                .findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                        pricingGroup.getId(), plan.getId(), dto.weeklyFrequency())
                .ifPresent(current -> {
                    current.close(today);
                    sessionFrequencyPriceRepository.saveAndFlush(current);
                });

        SessionFrequencyPriceEntity price = new SessionFrequencyPriceEntity(
                pricingGroup, plan, dto.weeklyFrequency(), Money.of(dto.sessionValue()), today);
        sessionFrequencyPriceRepository.save(price);
        return SessionPriceResponseDTO.fromEntity(price);
    }

    private PricingGroupEntity findPricingGroup(Long id) {
        return pricingGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing group not found with id: " + id));
    }

    private PlanEntity findPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
    }
}
