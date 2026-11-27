package br.com.senai.sublime_app.consultation.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.consultation.dto.ConsultationPatientResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ContractSessionBalanceResponseDTO;
import br.com.senai.sublime_app.consultation.dto.DaySummaryResponseDTO;
import br.com.senai.sublime_app.consultation.dto.EarningsResponseDTO;
import br.com.senai.sublime_app.consultation.dto.EarningsSummaryResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ProviderEarningsResponseDTO;
import br.com.senai.sublime_app.consultation.dto.LaunchOptionResponseDTO;
import br.com.senai.sublime_app.consultation.dto.RecentPatientResponseDTO;
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
import br.com.senai.sublime_app.provider.service.RetroactiveLaunchGrantService;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ForbiddenException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;

@Service
public class ConsultationService {

    // Caso 2 no Pilates em Grupo: aula experimental, sempre pelo preço de 1x por semana
    private static final int TRIAL_CLASS_WEEKLY_FREQUENCY = 1;

    // Duração sugerida no Pilates em Grupo (só registra a aula; sem preço por duração)
    private static final int GROUP_CLASS_DEFAULT_DURATION_MINUTES = 60;

    // Quantos pacientes recentes a tela de lançamento mostra
    private static final int RECENT_PATIENTS_LIMIT = 8;

    private final ConsultationRepository consultationRepository;
    private final ContractRepository contractRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository sessionDurationPriceRepository;
    private final SessionFrequencyPriceRepository sessionFrequencyPriceRepository;
    private final UserRepository userRepository;
    private final RetroactiveLaunchGrantService retroactiveLaunchGrantService;

    public ConsultationService(ConsultationRepository consultationRepository,
            ContractRepository contractRepository,
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            TechniqueRepository techniqueRepository,
            SessionDurationPriceRepository sessionDurationPriceRepository,
            SessionFrequencyPriceRepository sessionFrequencyPriceRepository,
            UserRepository userRepository,
            RetroactiveLaunchGrantService retroactiveLaunchGrantService) {
        this.consultationRepository = consultationRepository;
        this.contractRepository = contractRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.techniqueRepository = techniqueRepository;
        this.sessionDurationPriceRepository = sessionDurationPriceRepository;
        this.sessionFrequencyPriceRepository = sessionFrequencyPriceRepository;
        this.userRepository = userRepository;
        this.retroactiveLaunchGrantService = retroactiveLaunchGrantService;
    }

    /**
     * Lança um atendimento. O service faz o que depende do banco: busca os registros,
     * confere o contrato escolhido entre os vinculados ao paciente na data e resolve
     * qual linha de preço usar (casos 1 a 3). A entidade valida a coerência e calcula
     * o caso do preço, o valor, a comissão e o repasse.
     */
    @Transactional
    public ConsultationResponseDTO create(ConsultationRequestDTO dto, CurrentUser currentUser) {
        PatientEntity patient = getPatientOrThrow(dto.patientId());
        ProviderEntity provider = resolveProvider(dto.providerId(), currentUser);
        requireLaunchDateAllowed(dto.occurredAt(), provider, currentUser);
        TechniqueEntity technique = getTechniqueOrThrow(dto.techniqueId());
        // Referência sem consulta: o token já garantiu que o usuário existe e está ativo
        UserEntity launchedBy = userRepository.getReferenceById(currentUser.userId());

        ContractEntity contract = resolveContract(dto.contractId(), patient, dto.occurredAt());
        PriceLine price = resolvePrice(contract, technique, dto.durationMinutes());

        // Data e hora do lançamento no fuso da clínica (linha "Hoje" em "Padrão de construção")
        ConsultationEntity consultation = new ConsultationEntity(patient, contract, provider, launchedBy,
                LocalDateTime.now(), technique, dto.durationMinutes(), dto.occurredAt(), dto.status(),
                price.durationPrice(), price.frequencyPrice());
        consultationRepository.save(consultation);
        return ConsultationResponseDTO.fromEntity(consultation, remainingSessionsAfter(consultation));
    }

    // PROVIDER: o filtro de prestador é sempre o dele, qualquer que seja o da URL. from/to
    // filtram pela data do atendimento; launchedFrom/launchedTo, pela do lançamento (o
    // dashboard, para a lista bater com o rendimento)
    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> search(Long providerId, Long patientId, LocalDate from, LocalDate to,
            LocalDate launchedFrom, LocalDate launchedTo, ConsultationStatus status, CurrentUser currentUser) {
        Long effectiveProviderId = resolveProviderIdForListing(providerId, currentUser);
        return consultationRepository.search(effectiveProviderId, patientId, from, to,
                        startOfDay(launchedFrom), startOfNextDay(launchedTo), status).stream()
                .map(ConsultationResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationResponseDTO findById(Long id, CurrentUser currentUser) {
        return ConsultationResponseDTO.fromEntity(getConsultationVisibleToOrThrow(id, currentUser));
    }

    // Soft delete: o lançamento nunca é editado; para corrigir, inativa e lança de
    // novo. Inativo não conta no saldo nem no repasse. Gravado pelo dirty checking.
    @Transactional
    public void deactivate(Long id, CurrentUser currentUser) {
        ConsultationEntity consultation = getConsultationVisibleToOrThrow(id, currentUser);
        requireDeactivationDateAllowed(consultation, currentUser);
        consultation.deactivate();
    }

    // Pacientes ativos para a tela de lançamento, sem dados sensíveis: os dois perfis usam
    // (o GET /api/patients, com os dados completos, é só do ADMIN)
    @Transactional(readOnly = true)
    public List<ConsultationPatientResponseDTO> findPatients() {
        return patientRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(ConsultationPatientResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Opções do lançamento para o paciente, a técnica e a data (nada é gravado): cada
     * contrato vinculado ou, sem nenhum, o Avulso, com o caso do preço, o saldo do item e as
     * durações que têm preço (ou o motivo de não dar para lançar).
     */
    @Transactional(readOnly = true)
    public List<LaunchOptionResponseDTO> findLaunchOptions(Long patientId, Long techniqueId, LocalDate occurredAt) {
        PatientEntity patient = getPatientOrThrow(patientId);
        TechniqueEntity technique = getTechniqueOrThrow(techniqueId);
        List<ContractEntity> linked = contractRepository.findActiveCoveringDateByHolderOrBeneficiary(
                patient.getId(), occurredAt);
        if (linked.isEmpty()) {
            return List.of(singleSessionOption(technique));
        }
        return linked.stream()
                .map(contract -> contractOption(contract, patient, technique))
                .toList();
    }

    // Os pacientes distintos mais recentes do prestador, com a técnica e a duração do último
    // atendimento ativo (sugestão da tela). Paciente ou técnica inativos ficam de fora: não
    // dariam um lançamento válido.
    @Transactional(readOnly = true)
    public List<RecentPatientResponseDTO> findRecentPatients(Long providerId, CurrentUser currentUser) {
        ProviderEntity provider = resolveProviderForReading(providerId, currentUser);
        Map<Long, RecentPatientResponseDTO> byPatient = new LinkedHashMap<>();
        for (ConsultationEntity consultation : consultationRepository
                .findTop100ByProviderIdAndActiveTrueOrderByOccurredAtDescIdDesc(provider.getId())) {
            if (byPatient.size() == RECENT_PATIENTS_LIMIT) {
                break;
            }
            if (consultation.getPatient().isActive() && consultation.getTechnique().isActive()) {
                byPatient.putIfAbsent(consultation.getPatient().getId(), RecentPatientResponseDTO.fromEntity(consultation));
            }
        }
        return List.copyOf(byPatient.values());
    }

    // O dia do prestador: o rendimento de hoje, pela mesma regra do dashboard (realizados,
    // pela data do lançamento), as faltas cobradas à parte e o que a liberação de hoje ainda
    // permite. Somado aqui: o front não soma
    @Transactional(readOnly = true)
    public DaySummaryResponseDTO findDaySummary(Long providerId, CurrentUser currentUser) {
        ProviderEntity provider = resolveProviderForReading(providerId, currentUser);
        LocalDate today = LocalDate.now();
        EarningsTally tally = EarningsTally.of(sumEarnings(provider.getId(), today, today));
        return new DaySummaryResponseDTO(today, tally.consultationCount, tally.payoutTotal,
                tally.chargedAbsenceCount, tally.chargedAbsencePayoutTotal,
                retroactiveLaunchGrantService.remainingFor(provider));
    }

    // ---------------------------------------------------------------------------
    // Rendimento (ver "Rendimento" em Consultation, no domain-model): soma do repasse dos
    // realizados ativos, pela data do lançamento; as faltas cobradas à parte
    // ---------------------------------------------------------------------------

    // Hoje, a semana (segunda a domingo) e o mês do calendário de hoje, no fuso da clínica
    @Transactional(readOnly = true)
    public EarningsSummaryResponseDTO findEarningsSummary(Long providerId, CurrentUser currentUser) {
        Long effectiveProviderId = resolveProviderIdForEarnings(providerId, currentUser);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new EarningsSummaryResponseDTO(
                earningsOf(effectiveProviderId, today, today, false),
                earningsOf(effectiveProviderId, weekStart, weekStart.plusDays(6), false),
                earningsOf(effectiveProviderId, today.withDayOfMonth(1),
                        today.with(TemporalAdjusters.lastDayOfMonth()), false));
    }

    // Um período qualquer. O ADMIN sem prestador recebe também o rendimento de cada um
    @Transactional(readOnly = true)
    public EarningsResponseDTO findEarnings(Long providerId, LocalDate from, LocalDate to, CurrentUser currentUser) {
        requireValidPeriod(from, to);
        Long effectiveProviderId = resolveProviderIdForEarnings(providerId, currentUser);
        return earningsOf(effectiveProviderId, from, to, effectiveProviderId == null);
    }

    private EarningsResponseDTO earningsOf(Long providerId, LocalDate from, LocalDate to, boolean withByProvider) {
        List<ConsultationRepository.EarningsRow> rows = sumEarnings(providerId, from, to);
        EarningsTally total = EarningsTally.of(rows);
        return new EarningsResponseDTO(from, to, total.consultationCount, total.payoutTotal,
                total.chargedAbsenceCount, total.chargedAbsencePayoutTotal, total.totalPayout(),
                withByProvider ? earningsByProvider(rows) : List.of());
    }

    // Os prestadores ativos aparecem mesmo sem lançamento no período (zerados, para o admin
    // ver quem não lançou); um inativo, só se tiver lançamento. Por nome
    private List<ProviderEarningsResponseDTO> earningsByProvider(List<ConsultationRepository.EarningsRow> rows) {
        Map<Long, List<ConsultationRepository.EarningsRow>> rowsByProvider = new HashMap<>();
        rows.forEach(row -> rowsByProvider.computeIfAbsent(row.getProviderId(), id -> new ArrayList<>()).add(row));
        return providerRepository.findAll().stream()
                .filter(provider -> provider.isActive() || rowsByProvider.containsKey(provider.getId()))
                .sorted(Comparator.comparing(ProviderEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(provider -> {
                    EarningsTally tally = EarningsTally.of(rowsByProvider.getOrDefault(provider.getId(), List.of()));
                    return new ProviderEarningsResponseDTO(provider.getId(), provider.getName(),
                            tally.consultationCount, tally.payoutTotal, tally.chargedAbsenceCount,
                            tally.chargedAbsencePayoutTotal, tally.totalPayout());
                })
                .toList();
    }

    private List<ConsultationRepository.EarningsRow> sumEarnings(Long providerId, LocalDate from, LocalDate to) {
        return consultationRepository.sumEarnings(providerId, startOfDay(from), startOfNextDay(to));
    }

    // Soma as linhas do banco (por prestador e status) separando o rendimento das faltas
    private static final class EarningsTally {
        private long consultationCount;
        private BigDecimal payoutTotal = BigDecimal.ZERO;
        private long chargedAbsenceCount;
        private BigDecimal chargedAbsencePayoutTotal = BigDecimal.ZERO;

        static EarningsTally of(List<ConsultationRepository.EarningsRow> rows) {
            EarningsTally tally = new EarningsTally();
            rows.forEach(tally::add);
            return tally;
        }

        private void add(ConsultationRepository.EarningsRow row) {
            if (ConsultationStatus.valueOf(row.getStatus()).isChargedAbsence()) {
                chargedAbsenceCount += row.getConsultationCount();
                chargedAbsencePayoutTotal = chargedAbsencePayoutTotal.add(row.getPayoutTotal());
            } else {
                consultationCount += row.getConsultationCount();
                payoutTotal = payoutTotal.add(row.getPayoutTotal());
            }
        }

        BigDecimal totalPayout() {
            return payoutTotal.add(chargedAbsencePayoutTotal);
        }
    }

    private static void requireValidPeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("A data inicial do período não pode ser depois da data final.");
        }
    }

    // O dia inteiro pela data e hora do lançamento: [início do dia, início do dia seguinte)
    private static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime startOfNextDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }

    /**
     * Saldo de cada item do contrato, somando toda a cadeia de versões até a versão informada
     * (o frontend usa a versão ativa): as sessões do item por duração e as aulas do Pilates em
     * Grupo (só controle de presença).
     */
    @Transactional(readOnly = true)
    public List<ContractSessionBalanceResponseDTO> findSessionBalance(Long contractId) {
        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com id: " + contractId));
        return contract.getItems().stream()
                .map(item -> ContractSessionBalanceResponseDTO.fromItem(item, usedOf(contract, item)))
                .toList();
    }

    // ---------------------------------------------------------------------------
    // Quem atendeu (ver "Quem lança" em Consultation, no domain-model)
    // ---------------------------------------------------------------------------

    // ADMIN lança para qualquer prestador (providerId obrigatório). PROVIDER só lança para
    // ele mesmo: o prestador vem do vínculo do usuário logado; providerId é opcional e,
    // informado, precisa ser o dele.
    private ProviderEntity resolveProvider(Long providerId, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return getProviderOrThrow(requireProviderId(providerId));
        }
        ProviderEntity own = getOwnProviderOrThrow(currentUser);
        requireOwnProvider(providerId, own);
        return own;
    }

    // Leituras de um prestador (recentes, resumo do dia): o ADMIN informa qual; o PROVIDER é
    // sempre o dele, qualquer que seja o da URL (como na listagem)
    private ProviderEntity resolveProviderForReading(Long providerId, CurrentUser currentUser) {
        return currentUser.isAdmin()
                ? getProviderOrThrow(requireProviderId(providerId))
                : getOwnProviderOrThrow(currentUser);
    }

    // Listagem: o ADMIN filtra (ou não) por prestador; o PROVIDER vê sempre só os dele
    private Long resolveProviderIdForListing(Long providerId, CurrentUser currentUser) {
        return currentUser.isAdmin() ? providerId : getOwnProviderOrThrow(currentUser).getId();
    }

    // Rendimento: como a listagem, mas o prestador informado pelo ADMIN precisa existir (404);
    // vazio = todos os prestadores
    private Long resolveProviderIdForEarnings(Long providerId, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return providerId != null ? getProviderOrThrow(providerId).getId() : null;
        }
        return getOwnProviderOrThrow(currentUser).getId();
    }

    // Sem o @NotNull no DTO (o PROVIDER não precisa enviar): a obrigatoriedade do ADMIN
    // é conferida aqui, com a mesma mensagem
    private static Long requireProviderId(Long providerId) {
        if (providerId == null) {
            throw new BusinessRuleException("O prestador é obrigatório.");
        }
        return providerId;
    }

    // Usuário PROVIDER criado antes do prestador ainda não tem vínculo
    private ProviderEntity getOwnProviderOrThrow(CurrentUser currentUser) {
        return providerRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> new ForbiddenException(
                        "Seu usuário não está vinculado a um prestador. Procure o administrador."));
    }

    private static void requireOwnProvider(Long providerId, ProviderEntity own) {
        if (providerId != null && !providerId.equals(own.getId())) {
            throw new ForbiddenException("Você só pode lançar atendimentos em seu próprio nome.");
        }
    }

    // ---------------------------------------------------------------------------
    // Data do lançamento (ver "Quem lança" em Consultation, no domain-model). "Hoje" é o
    // dia da clínica (linha "Hoje" em "Padrão de construção", no architecture.md).
    // ---------------------------------------------------------------------------

    // PROVIDER só lança com a data de hoje; o retroativo (data passada: o futuro já é recusado pelo
    // DTO e pela entidade) usa uma unidade da liberação do admin, que confere o saldo e a
    // trava até o fim do lançamento. ADMIN lança qualquer data, sem liberação.
    private void requireLaunchDateAllowed(LocalDate occurredAt, ProviderEntity provider, CurrentUser currentUser) {
        if (currentUser.isAdmin() || occurredAt.equals(LocalDate.now())) {
            return;
        }
        retroactiveLaunchGrantService.consume(provider);
    }

    // PROVIDER só desfaz o que é de hoje: o lançamento retroativo exige liberação, e o
    // que já passou fica com o admin
    private static void requireDeactivationDateAllowed(ConsultationEntity consultation, CurrentUser currentUser) {
        if (!currentUser.isAdmin() && !consultation.getOccurredAt().equals(LocalDate.now())) {
            throw new BusinessRuleException("Só é possível desfazer lançamentos de hoje. Procure o administrador.");
        }
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

    // ---------------------------------------------------------------------------
    // Opções do lançamento: as durações seguem a mesma resolução de preço do lançamento
    // (ver "Resolução de preço" em Consultation, no domain-model)
    // ---------------------------------------------------------------------------

    private LaunchOptionResponseDTO contractOption(ContractEntity contract, PatientEntity patient,
            TechniqueEntity technique) {
        PricingGroupEntity pricingGroup = technique.getPricingGroup();
        Optional<ContractItemEntity> item = contract.findItemOf(pricingGroup);
        Integer remainingSessions = item
                .filter(ContractItemEntity::isDurationBased)
                .map(found -> remainingSessionsOf(contract, found))
                .orElse(null);
        DurationChoice durations = item
                .map(found -> contractItemDurations(found, contract.getPlan(), technique))
                .orElseGet(() -> equivalentPlanDurations(contract.getPlan(), technique));
        return new LaunchOptionResponseDTO(
                contract.getId(),
                contract.getPatient().getId(),
                contract.getPatient().getName(),
                contract.getPatient().getId().equals(patient.getId()),
                contract.getPlan().getName(),
                ConsultationEntity.pricingSourceFor(contract, pricingGroup),
                remainingSessions,
                durations.options(),
                durations.anyDurationAllowed(),
                durations.suggested(),
                durations.unavailableReason());
    }

    // Sem contrato vinculado: o Avulso. O Pilates em Grupo não tem avulso (exige contrato)
    private LaunchOptionResponseDTO singleSessionOption(TechniqueEntity technique) {
        PricingGroupEntity pricingGroup = technique.getPricingGroup();
        DurationChoice durations = pricingGroup.getPricingModel() == PricingModel.FREQUENCY_BASED
                ? DurationChoice.unavailable(technique.getName() + " exige contrato: não há preço avulso para o grupo "
                        + pricingGroup.getName() + ".")
                : DurationChoice.of(durationsOf(sessionDurationPriceRepository
                        .findByPricingGroupIdAndPlanTypeAndPlanActiveTrueAndValidToIsNull(
                                pricingGroup.getId(), PlanType.SINGLE_SESSION)),
                        null, "Não há preço avulso para " + technique.getName() + ".");
        return new LaunchOptionResponseDTO(null, null, null, false, null, PricingSource.SINGLE_SESSION, null,
                durations.options(), durations.anyDurationAllowed(), durations.suggested(),
                durations.unavailableReason());
    }

    // Caso 1: o contrato cobre o grupo. Pilates em Grupo: qualquer duração (a linha é a
    // travada). Por duração: as do plano do contrato no grupo, mais a travada (sugerida)
    private DurationChoice contractItemDurations(ContractItemEntity item, PlanEntity contractPlan,
            TechniqueEntity technique) {
        if (!item.isDurationBased()) {
            return DurationChoice.anyDuration();
        }
        int locked = item.getSessionDurationPrice().getDurationMinutes();
        List<Integer> options = new ArrayList<>(currentDurationsOf(technique, contractPlan));
        if (!options.contains(locked)) {
            options.add(locked);
            options.sort(null);
        }
        return DurationChoice.of(options, locked, null);
    }

    // Caso 2: o contrato não cobre o grupo → de-para. Pilates em Grupo: o preço de 1x por
    // semana do plano equivalente precisa existir. Por duração: as do plano equivalente.
    // Sem equivalente cadastrado, o motivo é a mensagem da regra (planFor)
    private DurationChoice equivalentPlanDurations(PlanEntity contractPlan, TechniqueEntity technique) {
        PricingModel pricingModel = technique.getPricingGroup().getPricingModel();
        PlanEntity plan;
        try {
            plan = contractPlan.planFor(pricingModel);
        } catch (BusinessRuleException noEquivalent) {
            return DurationChoice.unavailable(noEquivalent.getMessage());
        }
        if (pricingModel == PricingModel.FREQUENCY_BASED) {
            boolean hasTrialPrice = sessionFrequencyPriceRepository
                    .findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                            technique.getPricingGroup().getId(), plan.getId(), TRIAL_CLASS_WEEKLY_FREQUENCY)
                    .isPresent();
            return hasTrialPrice
                    ? DurationChoice.anyDuration()
                    : DurationChoice.unavailable("Não há preço do plano " + plan.getName() + " para "
                            + technique.getName() + " " + TRIAL_CLASS_WEEKLY_FREQUENCY + "x por semana.");
        }
        return DurationChoice.of(currentDurationsOf(technique, plan), null,
                "Não há preço do plano " + plan.getName() + " para " + technique.getName() + ".");
    }

    private List<Integer> currentDurationsOf(TechniqueEntity technique, PlanEntity plan) {
        return durationsOf(sessionDurationPriceRepository.findByPricingGroupIdAndPlanIdAndValidToIsNull(
                technique.getPricingGroup().getId(), plan.getId()));
    }

    private static List<Integer> durationsOf(List<SessionDurationPriceEntity> lines) {
        return lines.stream()
                .map(SessionDurationPriceEntity::getDurationMinutes)
                .distinct()
                .sorted()
                .toList();
    }

    // As durações de uma opção: a lista com preço (vazia quando qualquer uma vale, no Pilates
    // em Grupo), a sugerida e o motivo de não dar para lançar, quando não há preço
    private record DurationChoice(List<Integer> options, boolean anyDurationAllowed, Integer suggested,
            String unavailableReason) {

        static DurationChoice anyDuration() {
            return new DurationChoice(List.of(), true, GROUP_CLASS_DEFAULT_DURATION_MINUTES, null);
        }

        static DurationChoice unavailable(String reason) {
            return new DurationChoice(List.of(), false, null, reason);
        }

        // Sem nenhuma duração com preço, a opção fica indisponível com o motivo. Sem sugerida,
        // a única opção (havendo só uma) é a sugerida
        static DurationChoice of(List<Integer> options, Integer suggested, String reasonWhenEmpty) {
            if (options.isEmpty()) {
                return unavailable(reasonWhenEmpty);
            }
            Integer effectiveSuggested = suggested != null ? suggested : (options.size() == 1 ? options.get(0) : null);
            return new DurationChoice(options, false, effectiveSuggested, null);
        }
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
    // duração (o saldo de aulas do Grupo fica sem aviso no lançamento; de-para e Avulso não
    // debitam)
    private Integer remainingSessionsAfter(ConsultationEntity consultation) {
        if (!consultation.getPricingSource().debitsContractItem() || consultation.getSessionDurationPrice() == null) {
            return null;
        }
        ContractEntity contract = consultation.getContract();
        return contract.findItemOf(consultation.getTechnique().getPricingGroup())
                .map(item -> remainingSessionsOf(contract, item))
                .orElse(null);
    }

    // Sessões do item − sessões usadas na cadeia toda. Pode ficar negativo. As sessões vêm do
    // item (a linha travada), não do plano do contrato: no contrato de mensalidade, a linha do
    // item por duração é do pacote equivalente, e o plano do contrato não tem sessões
    private int remainingSessionsOf(ContractEntity contract, ContractItemEntity item) {
        return item.getValue().sessionCount() - usedSessionsOf(contract, item.getPricingGroup());
    }

    // O usado do item: as sessões (item por duração) ou as aulas (Pilates em Grupo)
    private int usedOf(ContractEntity contract, ContractItemEntity item) {
        return item.isDurationBased()
                ? usedSessionsOf(contract, item.getPricingGroup())
                : usedClassesOf(contract, item.getPricingGroup());
    }

    // Atendimentos ativos que debitaram o item (contrato + grupo) em qualquer versão
    // da cadeia. long → int: é uma contagem de sessões de um contrato (dezenas).
    private int usedSessionsOf(ContractEntity contract, PricingGroupEntity pricingGroup) {
        return (int) consultationRepository.countByContractIdInAndPricingSourceAndActiveTrueAndSessionDurationPricePricingGroupId(
                contract.getVersionChainIds(), PricingSource.CONTRACT_ITEM, pricingGroup.getId());
    }

    // Aulas do Pilates em Grupo lançadas pelo item (caso 1, linha de frequência do grupo), na
    // cadeia toda: só controle de presença, não entram no consumido
    private int usedClassesOf(ContractEntity contract, PricingGroupEntity pricingGroup) {
        return (int) consultationRepository.countByContractIdInAndPricingSourceAndActiveTrueAndSessionFrequencyPricePricingGroupId(
                contract.getVersionChainIds(), PricingSource.CONTRACT_ITEM, pricingGroup.getId());
    }

    // ---------------------------------------------------------------------------
    // Buscas (404 se o id não existir)
    // ---------------------------------------------------------------------------

    // PROVIDER só enxerga os lançamentos dele: o de outro prestador dá o MESMO 404 de um
    // id inexistente, para não revelar que ele existe. O vínculo é conferido antes da
    // busca: sem prestador, 403 para qualquer id (existente ou não).
    private ConsultationEntity getConsultationVisibleToOrThrow(Long id, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return getConsultationOrThrow(id);
        }
        ProviderEntity own = getOwnProviderOrThrow(currentUser);
        ConsultationEntity consultation = getConsultationOrThrow(id);
        if (!isFromProvider(consultation, own)) {
            throw consultationNotFound(id);
        }
        return consultation;
    }

    // Compara ids: o prestador do lançamento é LAZY e pode vir como proxy (getId não o carrega)
    private static boolean isFromProvider(ConsultationEntity consultation, ProviderEntity provider) {
        return consultation.getProvider().getId().equals(provider.getId());
    }

    private ConsultationEntity getConsultationOrThrow(Long id) {
        return consultationRepository.findById(id).orElseThrow(() -> consultationNotFound(id));
    }

    private static ResourceNotFoundException consultationNotFound(Long id) {
        return new ResourceNotFoundException("Atendimento não encontrado com id: " + id);
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
