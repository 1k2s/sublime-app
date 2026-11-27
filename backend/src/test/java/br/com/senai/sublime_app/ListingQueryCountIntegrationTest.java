package br.com.senai.sublime_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.senai.sublime_app.billing.dto.AccountListItemResponseDTO;
import br.com.senai.sublime_app.billing.dto.PaymentRequestDTO;
import br.com.senai.sublime_app.billing.service.BillingAccountService;
import br.com.senai.sublime_app.billing.service.PaymentService;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.service.ConsultationService;
import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractItemRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.contract.service.ContractService;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.dto.PatientResponseDTO;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.patient.service.PatientService;
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
import jakarta.persistence.EntityManagerFactory;

// As listagens de pacientes, de contratos e do Financeiro sem N+1: o número de consultas ao banco é
// o mesmo com mais registros, de relações diferentes (outros prestadores de origem; outros planos,
// linhas de preço e técnicas; aditivos, pagamentos e avulsos). Ver "Consulta com relação" no
// architecture. Chama os services, não a API: a contagem é a das consultas da listagem, sem as do
// login. O banco é dividido com os outros testes de integração: a lista traz também os registros
// deles, que entram nas duas contagens
@IntegrationTest
class ListingQueryCountIntegrationTest {

    // CPFs únicos na execução (o banco não é zerado entre os testes)
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 10_000_000_000L + 13);

    @Autowired
    private PatientService patientService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private BillingAccountService billingAccountService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ConsultationService consultationService;
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
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // O dia da clínica: lido depois de o contexto subir, quando o fuso dela já vale (ADR 0034)
    private LocalDate today;
    // Quem lança e registra na lista do Financeiro (os lançamentos com data passada são do admin)
    private CurrentUser admin;
    private ProviderEntity provider;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        UserEntity adminUser = userRepository.save(new UserEntity(
                "admin-" + SEQUENCE.incrementAndGet() + "@listagem.test", "hash", Role.ADMIN));
        admin = new CurrentUser(adminUser.getId(), Role.ADMIN);
        provider = newProvider("Prestador de teste");
    }

    @Test
    @DisplayName("Pacientes: o número de consultas não cresce com mais pacientes de outros prestadores de origem")
    void patientListDoesNotGrow() {
        newPatient("Ana", newProvider("Bruna"));
        long before = QueryCounter.countQueries(entityManagerFactory, () -> patientService.findAll());

        newPatient("Carla", newProvider("Carla Prestadora"));
        newPatient("Davi", newProvider("Davi Prestador"));
        Long elisa = newPatient("Elisa", newProvider("Elisa Prestadora"));
        List<PatientResponseDTO> patients = patientService.findAll();
        long after = QueryCounter.countQueries(entityManagerFactory, () -> patientService.findAll());

        assertThat(after).isEqualTo(before).isEqualTo(1);
        assertThat(patients).filteredOn(patient -> patient.id().equals(elisa))
                .singleElement()
                .satisfies(patient -> assertThat(patient.referringProviderName()).isEqualTo("Elisa Prestadora"));
    }

    // A lista é 1 consulta; com contratos terminados (o status depende do saldo, do billing), mais
    // as do saldo deles, de uma vez: todas as versões com os itens, o consumo, os pagamentos e as
    // transferências. O banco é dividido com os outros testes, que deixam contratos terminados
    @Test
    @DisplayName("Contratos: o número de consultas não cresce com mais contratos, de outros planos e itens, terminados ou com aditivo")
    void contractListDoesNotGrow() {
        createContract(newPatient("Fábio", null), plan("Evolução"), List.of(rpgItem()), 10);
        createContract(newPatient("Júlia", null), plan("Evolução"), List.of(rpgItem()), 100);
        long before = QueryCounter.countQueries(entityManagerFactory, () -> contractService.findAll());

        createContract(newPatient("Gabi", null), plan("Evolução"), List.of(rpgItem(), chiropracticItem()), 10);
        createContract(newPatient("Hugo", null), plan("Trimestral"), List.of(groupItem()), 10);
        createContract(newPatient("Iara", null), plan("Trimestral"), List.of(groupItem(), rpgItem()), 10);
        // Terminados, um deles com aditivo (a cadeia de versões entra no saldo)
        createContract(newPatient("Kátia", null), plan("Evolução"), List.of(rpgItem()), 100);
        Long amended = createContract(newPatient("Lucas", null), plan("Evolução"), List.of(rpgItem()), 100);
        contractService.amend(amended, new ContractAmendmentRequestDTO(null, PaymentMethod.CASH, 1, null, null,
                List.of(rpgItem())));
        List<ContractResponseDTO> contracts = contractService.findAll();
        long after = QueryCounter.countQueries(entityManagerFactory, () -> contractService.findAll());

        assertThat(after).isEqualTo(before).isLessThanOrEqualTo(5);
        assertThat(contracts).filteredOn(contract -> contract.planName().equals("Trimestral"))
                .flatExtracting(ContractResponseDTO::items)
                .isNotEmpty()
                .allSatisfy(item -> assertThat(item.pricingGroupName()).isNotBlank());
    }

    // As contas de todos os pacientes de uma vez: as versões com os itens, o consumo, os
    // pagamentos, as transferências de crédito e os pacientes (ver "Lista do Financeiro" em
    // Payment, no domain-model)
    @Test
    @DisplayName("Financeiro: o número de consultas não cresce com mais pacientes, com aditivo, pagamento e avulso")
    void accountListDoesNotGrow() {
        createAccountWithAmendment("Carla");
        launch(newPatient("Diego", null), null, today.minusDays(3));
        long before = QueryCounter.countQueries(entityManagerFactory, () -> billingAccountService.findAccounts());

        for (String name : List.of("Elisa", "Fábio", "Gabi")) {
            createAccountWithAmendment(name);
            launch(newPatient(name + " Avulso", null), null, today.minusDays(3));
        }
        List<AccountListItemResponseDTO> accounts = billingAccountService.findAccounts();
        long after = QueryCounter.countQueries(entityManagerFactory, () -> billingAccountService.findAccounts());

        assertThat(after).isEqualTo(before).isLessThanOrEqualTo(5);
        assertThat(accounts).extracting(AccountListItemResponseDTO::patientName)
                .contains("Carla", "Diego", "Elisa", "Fábio", "Gabi", "Gabi Avulso");
    }

    // ---------------------------------------------------------------------------
    // Dados de teste
    // ---------------------------------------------------------------------------

    // Contrato com R$ 668,00 pagos, um atendimento e um aditivo, com um atendimento na versão nova
    private void createAccountWithAmendment(String name) {
        Long patient = newPatient(name, null);
        Long contractId = contractService.create(new ContractRequestDTO(patient, null, plan("Evolução").getId(),
                today.minusDays(30), PaymentMethod.PIX, 3, null, List.of(rpgItem()), null)).id();
        paymentService.register(new PaymentRequestDTO(patient, contractId, new BigDecimal("668.00"),
                new BigDecimal("668.00"), null, today, PaymentMethod.PIX), admin);
        launch(patient, contractId, today.minusDays(10));
        Long amended = contractService.amend(contractId, new ContractAmendmentRequestDTO(null, PaymentMethod.PIX, 1,
                null, null, List.of(rpgItem(), chiropracticItem()))).id();
        launch(patient, amended, today.minusDays(1));
    }

    // Um RPG 60 min realizado; contractId nulo = avulso
    private void launch(Long patientId, Long contractId, LocalDate occurredAt) {
        consultationService.create(new ConsultationRequestDTO(patientId, contractId, provider.getId(),
                technique("RPG").getId(), 60, occurredAt, ConsultationStatus.ATTENDED), admin);
    }

    private ProviderEntity newProvider(String name) {
        UserEntity user = userRepository.save(new UserEntity(
                "prestador-" + SEQUENCE.incrementAndGet() + "@listagem.test", "hash", Role.PROVIDER));
        return providerRepository.save(new ProviderEntity(user, name, null, null));
    }

    private Long newPatient(String name, ProviderEntity referringProvider) {
        String cpf = String.format("%011d", SEQUENCE.incrementAndGet());
        return patientRepository.save(new PatientEntity(name, cpf, LocalDate.of(1990, 1, 1), null, null, null,
                referringProvider)).getId();
    }

    // Com início há N dias (100: o Evolução semanal, de 12 semanas, já terminou)
    private Long createContract(Long patientId, PlanEntity plan, List<ContractItemRequestDTO> items, int daysAgo) {
        return contractService.create(new ContractRequestDTO(patientId, null, plan.getId(), today.minusDays(daysAgo),
                PaymentMethod.PIX, 1, null, items, null)).id();
    }

    private ContractItemRequestDTO rpgItem() {
        return durationItem("RPG", "Individual Padrão", 60);
    }

    private ContractItemRequestDTO chiropracticItem() {
        return durationItem("Quiropraxia", "Individual Especializado", 30);
    }

    private ContractItemRequestDTO durationItem(String techniqueName, String groupName, int minutes) {
        Long priceId = durationPriceRepository.findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull(
                group(groupName).getId(), plan("Evolução").getId(), minutes).orElseThrow().getId();
        return new ContractItemRequestDTO(technique(techniqueName).getId(), priceId, null, SessionFrequency.ONCE_A_WEEK);
    }

    private ContractItemRequestDTO groupItem() {
        Long priceId = frequencyPriceRepository.findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                group("Em Grupo").getId(), plan("Trimestral").getId(), 2).orElseThrow().getId();
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
}
