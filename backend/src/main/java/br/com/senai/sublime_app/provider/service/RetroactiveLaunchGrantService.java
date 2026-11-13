package br.com.senai.sublime_app.provider.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.provider.domain.RetroactiveLaunchGrantEntity;
import br.com.senai.sublime_app.provider.dto.RetroactiveGrantResponseDTO;
import br.com.senai.sublime_app.provider.repository.ProviderRepository;
import br.com.senai.sublime_app.provider.repository.RetroactiveLaunchGrantRepository;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;

// Liberação de lançamentos retroativos (o "hoje" é o dia da clínica: ver a linha "Hoje"
// em "Padrão de construção", no architecture.md). O consultation usa o consume e o
// remaining ao lançar; o admin libera e consulta pelo controller.
@Service
public class RetroactiveLaunchGrantService {

    private final RetroactiveLaunchGrantRepository grantRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;

    public RetroactiveLaunchGrantService(RetroactiveLaunchGrantRepository grantRepository,
            ProviderRepository providerRepository, UserRepository userRepository) {
        this.grantRepository = grantRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    // Libera para hoje. Liberar de novo no mesmo dia cria outra, que passa a valer (a
    // anterior fica no histórico)
    @Transactional
    public RetroactiveGrantResponseDTO grant(Long providerId, int allowedCount, CurrentUser currentUser) {
        ProviderEntity provider = getProviderOrThrow(providerId);
        // Referência sem consulta: o token já garantiu que o usuário existe e está ativo
        UserEntity admin = userRepository.getReferenceById(currentUser.userId());
        RetroactiveLaunchGrantEntity grant = new RetroactiveLaunchGrantEntity(provider, admin, LocalDate.now(),
                allowedCount);
        grantRepository.save(grant);
        return RetroactiveGrantResponseDTO.fromEntity(grant);
    }

    // A liberação de hoje, se ainda tem unidade
    @Transactional(readOnly = true)
    public Optional<RetroactiveGrantResponseDTO> findActive(Long providerId) {
        getProviderOrThrow(providerId);
        return findUsableToday(providerId).map(RetroactiveGrantResponseDTO::fromEntity);
    }

    // Quantos lançamentos retroativos o prestador ainda pode fazer hoje (0 sem liberação)
    @Transactional(readOnly = true)
    public int remainingFor(ProviderEntity provider) {
        return findUsableToday(provider.getId()).map(RetroactiveLaunchGrantEntity::getRemaining).orElse(0);
    }

    // Um lançamento retroativo usa uma unidade. Tranca a liberação até o fim da transação
    // do lançamento (que chama este método): dois ao mesmo tempo não usam a mesma unidade
    @Transactional
    public void consume(ProviderEntity provider) {
        LocalDate today = LocalDate.now();
        RetroactiveLaunchGrantEntity grant = grantRepository
                .findFirstWithLockByProviderIdAndGrantedOnOrderByIdDesc(provider.getId(), today)
                .filter(candidate -> candidate.isUsableOn(today))
                .orElseThrow(() -> new BusinessRuleException(
                        "Lançamento retroativo precisa de liberação do administrador."));
        grant.consume();
    }

    private Optional<RetroactiveLaunchGrantEntity> findUsableToday(Long providerId) {
        LocalDate today = LocalDate.now();
        return grantRepository.findFirstByProviderIdAndGrantedOnOrderByIdDesc(providerId, today)
                .filter(grant -> grant.isUsableOn(today));
    }

    private ProviderEntity getProviderOrThrow(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado com id: " + id));
    }
}
