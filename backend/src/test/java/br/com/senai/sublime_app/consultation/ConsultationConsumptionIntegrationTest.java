package br.com.senai.sublime_app.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.senai.sublime_app.IntegrationTest;
import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ContractSessionBalanceResponseDTO;
import br.com.senai.sublime_app.consultation.repository.ConsultationRepository;
import br.com.senai.sublime_app.consultation.service.ConsultationService;
import br.com.senai.sublime_app.contract.dto.ContractItemRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.contract.service.ContractService;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.pricing.repository.PricingGroupRepository;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.provider.repository.ProviderRepository;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.repository.UserRepository;

// O consumo que o billing lê (ConsultationConsumption, ver "Saldo de sessões" em Consultation,
// no domain-model) e o saldo de cada item, no MySQL de verdade: as projeções leem o baseValue
// (Money, com AttributeConverter) e os joins opcionais (contrato e linha por duração). Os dados
// usam o catálogo da V4 e são criados pelos services, como a API faria. O banco é dividido com os
// outros testes de integração: cada teste cria pacientes novos e confere só os próprios
// atendimentos.
@IntegrationTest
class ConsultationConsumptionIntegrationTest {

    // CPFs únicos na execução (o banco não é zerado entre os testes)
    private static final AtomicLong CPF_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 10_000_000_000L);

    @Autowired
    private ContractService contractService;
    @Autowired
    private ConsultationService consultationService;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private PricingGroupRepository pricingGroupRepository;
    @Autowired
    private TechniqueRepository techniqueRepository;
    @Autowired
    private SessionDurationPriceRepository durationPriceRepository;
    @Autowired
    private SessionFrequencyPriceRepository frequencyPriceRepository;

    private CurrentUser admin;
    private Long providerId;
    // O dia da clínica: lido depois de o contexto subir, quando o fuso dela já vale (ADR 0034).
    // Um campo estático seria lido antes, no fuso da máquina
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        UserEntity adminUser = userRepository.save(new UserEntity(uniqueEmail("admin"), "hash", Role.ADMIN));
        admin = new CurrentUser(adminUser.getId(), Role.ADMIN);
        UserEntity providerUser = userRepository.save(new UserEntity(uniqueEmail("prestador"), "hash", Role.PROVIDER));
        providerId = providerRepository.save(new ProviderEntity(providerUser, "Prestador de teste", null, null)).getId();
    }

    @Test
    @DisplayName("Consumo de um contrato: soma o item por duração e o de-para; o inativado fica de fora")
    void contractConsumptionSumsDurationItemAndEquivalentPlan() {
        Long ana = newPatient("Ana");
        // Evolução, RPG 60 min semanal (linha de R$ 167,00 no Individual Padrão)
        Long contractId = createContract(ana, plan("Evolução"),
                List.of(durationItem("RPG", "Individual Padrão", "Evolução", 60, SessionFrequency.ONCE_A_WEEK)));

        launch(ana, contractId, "RPG", 60, today.minusDays(20));
        launch(ana, contractId, "RPG", 60, today.minusDays(13));
        Long inactivated = launch(ana, contractId, "RPG", 60, today.minusDays(6)).id();
        consultationService.deactivate(inactivated, admin);
        // Quiropraxia fora do contrato: de-para, R$ 132,00 (Evolução 30 min no Individual Especializado)
        launch(ana, contractId, "Quiropraxia", 30, today.minusDays(5));

        List<ConsultationConsumption> consumption = consultationRepository.findConsumptionByContractIdIn(
                List.of(contractId));

        assertThat(consumption).hasSize(3);
        assertThat(consumption).extracting(ConsultationConsumption::pricingSource)
                .containsExactlyInAnyOrder(PricingSource.CONTRACT_ITEM, PricingSource.CONTRACT_ITEM,
                        PricingSource.EQUIVALENT_PLAN);
        assertThat(consumption).allMatch(ConsultationConsumption::durationBased);
        // O grupo da linha usada: o do RPG (Individual Padrão) e o da Quiropraxia (Especializado)
        assertThat(consumption).extracting(ConsultationConsumption::pricingGroupId)
                .containsOnly(group("Individual Padrão").getId(), group("Individual Especializado").getId());
        assertThat(ConsultationConsumption.sumConsumed(consumption).amount()).isEqualByComparingTo("466.00");
    }

    @Test
    @DisplayName("Contrato de mensalidade: as aulas do Grupo não somam; o saldo de aulas e o do item por duração saem do item")
    void monthlyFeeContractBalancesAndConsumption() {
        Long bia = newPatient("Bia");
        // Trimestral 2x (13 semanas = 26 aulas) + RPG semanal pela linha do Evolução (de-para)
        Long contractId = createContract(bia, plan("Trimestral"), List.of(
                groupItem("Trimestral", 2),
                durationItem("RPG", "Individual Padrão", "Evolução", 60, SessionFrequency.ONCE_A_WEEK)));

        launch(bia, contractId, "Pilates em Grupo", 60, today.minusDays(20));
        launch(bia, contractId, "Pilates em Grupo", 60, today.minusDays(18));
        launch(bia, contractId, "Pilates em Grupo", 60, today.minusDays(13));
        // Antes da correção, o saldo do item vinha do plano do contrato (a mensalidade, sem sessões): 500
        ConsultationResponseDTO rpg = launch(bia, contractId, "RPG", 60, today.minusDays(12));

        assertThat(rpg.remainingSessions()).isEqualTo(11);
        // A aula do Grupo traz o grupo da linha por frequência (Em Grupo)
        assertThat(consultationRepository.findConsumptionByContractIdIn(List.of(contractId)))
                .filteredOn(row -> !row.durationBased())
                .extracting(ConsultationConsumption::pricingGroupId)
                .containsOnly(group("Em Grupo").getId());
        assertThat(ConsultationConsumption.sumConsumed(
                consultationRepository.findConsumptionByContractIdIn(List.of(contractId))).amount())
                .isEqualByComparingTo("167.00");

        List<ContractSessionBalanceResponseDTO> balance = consultationService.findSessionBalance(contractId);
        ContractSessionBalanceResponseDTO group = balance.stream().filter(ContractSessionBalanceResponseDTO::groupItem)
                .findFirst().orElseThrow();
        ContractSessionBalanceResponseDTO duration = balance.stream().filter(item -> !item.groupItem())
                .findFirst().orElseThrow();
        assertThat(group.sessionCount()).isEqualTo(26);
        assertThat(group.usedSessions()).isEqualTo(3);
        assertThat(group.remainingSessions()).isEqualTo(23);
        assertThat(duration.sessionCount()).isEqualTo(12);
        assertThat(duration.remainingSessions()).isEqualTo(11);
    }

    @Test
    @DisplayName("Avulso: soma o atendimento sem contrato do paciente")
    void singleSessionConsumptionWithoutContract() {
        Long diego = newPatient("Diego");

        // Sem contrato: o Avulso 60 min no Individual Padrão, R$ 220,00
        launch(diego, null, "RPG", 60, today.minusDays(2));

        List<ConsultationConsumption> consumption = consultationRepository
                .findConsumptionWithoutContractByPatientId(diego);
        assertThat(consumption).singleElement().satisfies(row -> {
            assertThat(row.contractId()).isNull();
            assertThat(row.pricingSource()).isEqualTo(PricingSource.SINGLE_SESSION);
        });
        assertThat(ConsultationConsumption.sumConsumed(consumption).amount()).isEqualByComparingTo("220.00");
    }

    @Test
    @DisplayName("Por período e na lista de todos: os mesmos atendimentos ativos; as sessões do contrato na ordem da data")
    void periodAllActiveAndOrderedSessions() {
        Long carla = newPatient("Carla");
        Long contractId = createContract(carla, plan("Evolução"),
                List.of(durationItem("RPG", "Individual Padrão", "Evolução", 60, SessionFrequency.ONCE_A_WEEK)));
        Long later = launch(carla, contractId, "RPG", 60, today.minusDays(3)).id();
        Long earlier = launch(carla, contractId, "RPG", 60, today.minusDays(10)).id();
        Long inactivated = launch(carla, contractId, "RPG", 60, today.minusDays(8)).id();
        consultationService.deactivate(inactivated, admin);

        List<Long> inPeriod = consultationRepository.findConsumptionBetween(today.minusDays(10), today.minusDays(4))
                .stream().map(ConsultationConsumption::consultationId).toList();
        List<Long> allActive = consultationRepository.findAllActiveConsumption().stream()
                .map(ConsultationConsumption::consultationId).toList();
        List<Long> ordered = consultationRepository.findByContractIdInAndActiveTrueOrderByOccurredAtAscIdAsc(
                List.of(contractId)).stream().map(ConsultationEntity::getId).toList();

        assertThat(inPeriod).contains(earlier).doesNotContain(later, inactivated);
        assertThat(allActive).contains(earlier, later).doesNotContain(inactivated);
        assertThat(ordered).containsExactly(earlier, later);
    }

    // ---------------------------------------------------------------------------
    // Dados de teste
    // ---------------------------------------------------------------------------

    private Long newPatient(String name) {
        String cpf = String.format("%011d", CPF_SEQUENCE.incrementAndGet());
        return patientRepository.save(new PatientEntity(name, cpf, LocalDate.of(1990, 1, 1), null, null, null, null))
                .getId();
    }

    // Contrato com início há 30 dias (os lançamentos com data passada são do admin)
    private Long createContract(Long patientId, PlanEntity plan, List<ContractItemRequestDTO> items) {
        return contractService.create(new ContractRequestDTO(patientId, null, plan.getId(), today.minusDays(30),
                PaymentMethod.PIX, 1, null, items, null)).id();
    }

    private ConsultationResponseDTO launch(Long patientId, Long contractId, String techniqueName, int minutes,
            LocalDate occurredAt) {
        return consultationService.create(new ConsultationRequestDTO(patientId, contractId, providerId,
                technique(techniqueName).getId(), minutes, occurredAt, ConsultationStatus.ATTENDED), admin);
    }

    private ContractItemRequestDTO durationItem(String techniqueName, String groupName, String planName, int minutes,
            SessionFrequency frequency) {
        Long priceId = durationPriceRepository.findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull(
                group(groupName).getId(), plan(planName).getId(), minutes).orElseThrow().getId();
        return new ContractItemRequestDTO(technique(techniqueName).getId(), priceId, null, frequency);
    }

    private ContractItemRequestDTO groupItem(String planName, int weeklyFrequency) {
        Long priceId = frequencyPriceRepository.findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                group("Em Grupo").getId(), plan(planName).getId(), weeklyFrequency).orElseThrow().getId();
        return new ContractItemRequestDTO(technique("Pilates em Grupo").getId(), null, priceId, null);
    }

    private PlanEntity plan(String name) {
        return planRepository.findAll().stream().filter(plan -> plan.getName().equals(name)).findFirst().orElseThrow();
    }

    private PricingGroupEntity group(String name) {
        return pricingGroupRepository.findAll().stream().filter(group -> group.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private TechniqueEntity technique(String name) {
        return techniqueRepository.findByActiveTrue().stream().filter(technique -> technique.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + CPF_SEQUENCE.incrementAndGet() + "@consumo.test";
    }
}
