package br.com.senai.sublime_app;

import static br.com.senai.sublime_app.ApiTestClient.body;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.senai.sublime_app.ApiTestClient.ApiResponse;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.repository.PlanRepository;
import br.com.senai.sublime_app.pricing.repository.PricingGroupRepository;
import br.com.senai.sublime_app.pricing.repository.SessionDurationPriceRepository;
import br.com.senai.sublime_app.pricing.repository.SessionFrequencyPriceRepository;
import br.com.senai.sublime_app.pricing.repository.TechniqueRepository;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

// O cenário dos testes pela API: um admin logado (o login real), um prestador e os dados criados
// pela própria API, como o front faria (pacientes, contratos, lançamentos e pagamentos). O catálogo
// é o da V4 (os preços do seed: RPG 60 min no Evolução R$ 167,00; Quiropraxia 30 min R$ 132,00;
// Avulso 60 min R$ 220,00; aula do Grupo Trimestral 2x R$ 65,81), lido pelos repositórios: é o
// preparo, não o que o teste confere. Uma instância por teste, no @BeforeEach (o "hoje" é o dia da
// clínica, lido depois de o contexto subir). O banco é dividido com os outros testes: tudo criado
// aqui é único
public final class ApiScenario {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L * 10);
    private static final String ADMIN_PASSWORD = "senha-de-teste";

    public final ApiTestClient api;
    public final LocalDate today = LocalDate.now();

    private final PlanRepository planRepository;
    private final PricingGroupRepository pricingGroupRepository;
    private final TechniqueRepository techniqueRepository;
    private final SessionDurationPriceRepository durationPriceRepository;
    private final SessionFrequencyPriceRepository frequencyPriceRepository;
    private final Long providerId;

    public ApiScenario(ApplicationContext context) {
        this.api = new ApiTestClient(context.getBean(MockMvc.class), context.getBean(ObjectMapper.class));
        this.planRepository = context.getBean(PlanRepository.class);
        this.pricingGroupRepository = context.getBean(PricingGroupRepository.class);
        this.techniqueRepository = context.getBean(TechniqueRepository.class);
        this.durationPriceRepository = context.getBean(SessionDurationPriceRepository.class);
        this.frequencyPriceRepository = context.getBean(SessionFrequencyPriceRepository.class);

        // O admin direto no repositório (o cadastro de usuário pede um admin logado), com a senha
        // pelo PasswordEncoder da aplicação; o resto, pela API
        UserEntity admin = context.getBean(UserRepository.class).save(new UserEntity(uniqueEmail("admin"),
                context.getBean(PasswordEncoder.class).encode(ADMIN_PASSWORD), Role.ADMIN));
        api.login(admin.getEmail(), ADMIN_PASSWORD);
        Long providerUserId = api.post("/api/users", body("email", uniqueEmail("prestador"), "password", "Prestador@2026",
                "role", "PROVIDER")).expect(201).id();
        this.providerId = api.post("/api/providers", body("userId", providerUserId, "name", "Prestador de teste"))
                .expect(201).id();
    }

    // ---------------------------------------------------------------------------
    // Cadastros pela API
    // ---------------------------------------------------------------------------

    public Long patient(String name) {
        String cpf = String.format("%011d", SEQUENCE.incrementAndGet() % 100_000_000_000L);
        return api.post("/api/patients", body("name", name, "cpf", cpf, "birthDate", "1990-01-01")).expect(201).id();
    }

    // O pedido do cadastro do contrato (Evolução, PIX, 3 parcelas, RPG semanal), para o teste mudar
    // o que quiser antes de enviar
    public Map<String, Object> contractBody(Long patientId, LocalDate startDate) {
        return body("patientId", patientId, "beneficiaryId", null, "planId", planId("Evolução"),
                "startDate", startDate.toString(), "paymentMethod", "PIX", "installments", 3, "firstDueDate", null,
                "items", List.of(rpgItem()), "applyCredit", null);
    }

    public ApiResponse contract(Map<String, Object> body) {
        return api.post("/api/contracts", body).expect(201);
    }

    // O Evolução RPG semanal padrão, com início há N dias (100: os 12 semanas já terminaram)
    public Long contract(Long patientId, int startDaysAgo) {
        return contract(contractBody(patientId, today.minusDays(startDaysAgo))).id();
    }

    // O aditivo: o estado completo da nova versão
    public ApiResponse amend(Long contractId, String paymentMethod, int installments, String firstDueDate,
            List<Map<String, Object>> items) {
        return api.post("/api/contracts/" + contractId + "/amendments", body("beneficiaryId", null,
                "paymentMethod", paymentMethod, "installments", installments, "endDate", null,
                "firstDueDate", firstDueDate, "items", items));
    }

    // Um atendimento realizado (ou o status dado), lançado pelo admin; contractId nulo = avulso
    public Long launch(Long patientId, Long contractId, String technique, LocalDate occurredAt, String status) {
        int minutes = technique.equals("Quiropraxia") ? 30 : 60;
        return api.post("/api/consultations", body("patientId", patientId, "contractId", contractId,
                "providerId", providerId, "techniqueId", techniqueId(technique), "durationMinutes", minutes,
                "occurredAt", occurredAt.toString(), "status", status)).expect(201).id();
    }

    public Long launch(Long patientId, Long contractId, String technique, LocalDate occurredAt) {
        return launch(patientId, contractId, technique, occurredAt, "ATTENDED");
    }

    // N atendimentos em dias seguidos, a partir da data
    public List<Long> launchMany(Long patientId, Long contractId, String technique, LocalDate firstDate, int count) {
        List<Long> ids = new ArrayList<>();
        for (int day = 0; day < count; day++) {
            ids.add(launch(patientId, contractId, technique, firstDate.plusDays(day)));
        }
        return ids;
    }

    // O pedido do pagamento (Pix, líquido = bruto, hoje), para o teste mudar o que quiser
    public Map<String, Object> paymentBody(Long patientId, Long contractId, String amount) {
        return body("patientId", patientId, "contractId", contractId, "amount", amount, "netAmount", amount,
                "cardInstallments", null, "paidAt", today.toString(), "paymentMethod", "PIX");
    }

    public Long pay(Long patientId, Long contractId, String amount) {
        return api.post("/api/payments", paymentBody(patientId, contractId, amount)).expect(201).id();
    }

    public ApiResponse account(Long patientId) {
        return api.get("/api/billing/patients/" + patientId + "/account").expect(200);
    }

    // ---------------------------------------------------------------------------
    // Catálogo (V4)
    // ---------------------------------------------------------------------------

    public Map<String, Object> rpgItem() {
        return durationItem("RPG", "Individual Padrão", 60, "ONCE_A_WEEK");
    }

    public Map<String, Object> chiropracticItem(String frequency) {
        return durationItem("Quiropraxia", "Individual Especializado", 30, frequency);
    }

    public Map<String, Object> groupItem() {
        Long priceId = frequencyPriceRepository.findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
                group("Em Grupo").getId(), planId("Trimestral"), 2).orElseThrow().getId();
        return body("techniqueId", techniqueId("Pilates em Grupo"), "sessionDurationPriceId", null,
                "sessionFrequencyPriceId", priceId, "frequency", null);
    }

    private Map<String, Object> durationItem(String technique, String groupName, int minutes, String frequency) {
        Long priceId = durationPriceRepository.findByPricingGroupIdAndPlanIdAndDurationMinutesAndValidToIsNull(
                group(groupName).getId(), planId("Evolução"), minutes).orElseThrow().getId();
        return body("techniqueId", techniqueId(technique), "sessionDurationPriceId", priceId,
                "sessionFrequencyPriceId", null, "frequency", frequency);
    }

    public Long planId(String name) {
        return planRepository.findAll().stream().filter(plan -> plan.getName().equals(name))
                .map(PlanEntity::getId).findFirst().orElseThrow();
    }

    private PricingGroupEntity group(String name) {
        return pricingGroupRepository.findAll().stream().filter(group -> group.getName().equals(name))
                .findFirst().orElseThrow();
    }

    private Long techniqueId(String name) {
        return techniqueRepository.findByActiveTrue().stream().filter(technique -> technique.getName().equals(name))
                .findFirst().orElseThrow().getId();
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@api.test";
    }
}
