package br.com.senai.sublime_app.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.dto.SessionPriceResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniquePriceTableResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueResponseDTO;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

// Porta de entrada do módulo de preços: usado pelos endpoints de consulta e,
// futuramente, por contract e consultation para resolver preços vigentes.
@Service
public class PricingService {

    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;

    public PricingService(TechniqueRepository techniqueRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository) {
        this.techniqueRepository = techniqueRepository;
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
}
