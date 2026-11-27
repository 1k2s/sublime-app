package br.com.senai.sublime_app.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import br.com.senai.sublime_app.IntegrationTest;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.repository.UserRepository;

// Rendimento do prestador pela API real (ver "Rendimento" em Consultation, no domain-model):
// só os atendidos ativos, pela data do lançamento, com as faltas cobradas à parte. Confere
// também a consulta nativa de soma (ConsultationRepository.sumEarnings) no MySQL.
// Cada teste cria os próprios prestadores: o banco não é limpo entre os testes (ver a
// @IntegrationTest), então os totais de um prestador novo só têm o que o teste lançou. O total
// do ADMIN sem prestador mistura os de outros testes e não é conferido em valor absoluto.
@IntegrationTest
class EarningsIntegrationTest {

    private static final String PASSWORD = "senha-de-teste";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

    // RPG (Individual Padrão), Avulso de 60 min no seed V4: R$ 220,00; com a comissão padrão
    // de 40%, o repasse de cada lançamento é R$ 88,00
    private static final String TECHNIQUE_NAME = "RPG";
    private static final int DURATION_MINUTES = 60;
    private static final double PAYOUT = 88.0;

    private static String encodedPassword;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private long techniqueId;
    private LocalDate today;

    @BeforeAll
    static void encodePassword(@Autowired PasswordEncoder passwordEncoder) {
        encodedPassword = passwordEncoder.encode(PASSWORD);
    }

    @BeforeEach
    void setUp() throws Exception {
        today = LocalDate.now(CLINIC_ZONE);
        adminToken = tokenFor(saveUser(Role.ADMIN));
        String techniques = read(get("/api/techniques"), adminToken);
        List<Number> ids = JsonPath.read(techniques, "$[?(@.name == '" + TECHNIQUE_NAME + "')].id");
        techniqueId = ids.get(0).longValue();
    }

    @Nested
    @DisplayName("Rendimento do período")
    class Earnings {

        @Test
        @DisplayName("Atendido entra no rendimento; faltou e desmarcou < 6h vão para as faltas cobradas")
        void separatesAttendedFromChargedAbsences() throws Exception {
            Provider provider = newProvider();
            launchAsProvider(provider, "ATTENDED");
            launchAsProvider(provider, "NO_SHOW");
            launchAsProvider(provider, "CANCELED_LATE");

            mockMvc.perform(withToken(earnings(today, today), provider.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultationCount").value(1))
                    .andExpect(jsonPath("$.payoutTotal").value(PAYOUT))
                    .andExpect(jsonPath("$.chargedAbsenceCount").value(2))
                    .andExpect(jsonPath("$.chargedAbsencePayoutTotal").value(2 * PAYOUT))
                    .andExpect(jsonPath("$.totalPayout").value(3 * PAYOUT))
                    .andExpect(jsonPath("$.byProvider").isEmpty());
        }

        // O admin lança com a data do atendimento de 5 dias atrás: o lançamento é de hoje
        @Test
        @DisplayName("Retroativo conta no rendimento do dia do lançamento, não no do atendimento")
        void countsRetroactiveOnLaunchDay() throws Exception {
            Provider provider = newProvider();
            launchAsAdmin(provider, today.minusDays(5), "ATTENDED");

            mockMvc.perform(withToken(earnings(today.minusDays(5), today.minusDays(1)), provider.token()))
                    .andExpect(jsonPath("$.consultationCount").value(0));
            mockMvc.perform(withToken(earnings(today, today), provider.token()))
                    .andExpect(jsonPath("$.consultationCount").value(1))
                    .andExpect(jsonPath("$.payoutTotal").value(PAYOUT));
        }

        @Test
        @DisplayName("Atendimento desfeito sai do rendimento")
        void excludesUndoneConsultation() throws Exception {
            Provider provider = newProvider();
            long id = launchAsProvider(provider, "ATTENDED");

            mockMvc.perform(withToken(delete("/api/consultations/{id}", id), provider.token()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(withToken(earnings(today, today), provider.token()))
                    .andExpect(jsonPath("$.consultationCount").value(0))
                    .andExpect(jsonPath("$.payoutTotal").value(0.0))
                    .andExpect(jsonPath("$.totalPayout").value(0.0));
        }

        @Test
        @DisplayName("Período sem lançamentos dá zero, com o período na resposta")
        void returnsZeroForEmptyPeriod() throws Exception {
            Provider provider = newProvider();
            LocalDate from = today.minusMonths(2).withDayOfMonth(1);
            LocalDate to = from.plusDays(9);

            mockMvc.perform(withToken(earnings(from, to), provider.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value(from.toString()))
                    .andExpect(jsonPath("$.to").value(to.toString()))
                    .andExpect(jsonPath("$.consultationCount").value(0))
                    .andExpect(jsonPath("$.chargedAbsenceCount").value(0))
                    .andExpect(jsonPath("$.totalPayout").value(0.0));
        }

        @Test
        @DisplayName("Data inicial depois da final responde 400")
        void rejectsInvertedPeriod() throws Exception {
            Provider provider = newProvider();

            mockMvc.perform(withToken(earnings(today, today.minusDays(1)), provider.token()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("A data inicial do período não pode ser depois da data final."));
        }

        @Test
        @DisplayName("Resumo: hoje, a semana de segunda a domingo e o mês do calendário")
        void summaryUsesTodayWeekAndMonth() throws Exception {
            Provider provider = newProvider();
            launchAsProvider(provider, "ATTENDED");
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            mockMvc.perform(withToken(get("/api/consultations/earnings-summary"), provider.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.today.from").value(today.toString()))
                    .andExpect(jsonPath("$.today.to").value(today.toString()))
                    .andExpect(jsonPath("$.week.from").value(monday.toString()))
                    .andExpect(jsonPath("$.week.to").value(monday.plusDays(6).toString()))
                    .andExpect(jsonPath("$.month.from").value(today.withDayOfMonth(1).toString()))
                    .andExpect(jsonPath("$.month.to").value(today.with(TemporalAdjusters.lastDayOfMonth()).toString()))
                    .andExpect(jsonPath("$.today.consultationCount").value(1))
                    .andExpect(jsonPath("$.week.consultationCount").value(1))
                    .andExpect(jsonPath("$.month.consultationCount").value(1))
                    .andExpect(jsonPath("$.month.byProvider").isEmpty());
        }

        @Test
        @DisplayName("ADMIN sem prestador: o byProvider traz cada prestador, o ativo sem lançamento zerado")
        void adminWithoutProviderGetsEachProvider() throws Exception {
            Provider withLaunch = newProvider();
            Provider withoutLaunch = newProvider();
            launchAsProvider(withLaunch, "ATTENDED");
            launchAsProvider(withLaunch, "NO_SHOW");

            String body = read(earnings(today, today), adminToken);

            assertThat(numbersOf(body, withLaunch, "consultationCount")).containsExactly(1L);
            assertThat(numbersOf(body, withLaunch, "chargedAbsenceCount")).containsExactly(1L);
            assertThat(numbersOf(body, withoutLaunch, "consultationCount")).containsExactly(0L);
            assertThat(numbersOf(body, withoutLaunch, "totalPayout")).containsExactly(0L);
            // Soma do byProvider = total (os dois vêm das mesmas linhas da consulta)
            List<Number> payouts = JsonPath.read(body, "$.byProvider[*].payoutTotal");
            double sum = payouts.stream().mapToDouble(Number::doubleValue).sum();
            assertThat(sum).isCloseTo(((Number) JsonPath.read(body, "$.payoutTotal")).doubleValue(), within(0.001));
        }

        @Test
        @DisplayName("Resumo do dia da tela de Lançamento: só o atendido, com as faltas à parte")
        void daySummaryFollowsEarningsRule() throws Exception {
            Provider provider = newProvider();
            launchAsProvider(provider, "ATTENDED");
            launchAsProvider(provider, "NO_SHOW");

            mockMvc.perform(withToken(get("/api/consultations/day-summary"), provider.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value(today.toString()))
                    .andExpect(jsonPath("$.consultationCount").value(1))
                    .andExpect(jsonPath("$.payoutTotal").value(PAYOUT))
                    .andExpect(jsonPath("$.chargedAbsenceCount").value(1))
                    .andExpect(jsonPath("$.chargedAbsencePayoutTotal").value(PAYOUT));
        }
    }

    @Nested
    @DisplayName("Acesso por perfil")
    class Access {

        @Test
        @DisplayName("PROVIDER vê sempre o próprio rendimento, qualquer que seja o providerId da URL")
        void providerAlwaysSeesOwnEarnings() throws Exception {
            Provider other = newProvider();
            Provider own = newProvider();
            launchAsProvider(other, "ATTENDED");

            mockMvc.perform(withToken(earnings(today, today).param("providerId", String.valueOf(other.id())), own.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultationCount").value(0));
        }

        @Test
        @DisplayName("PROVIDER sem prestador vinculado recebe 403")
        void rejectsProviderUserWithoutProvider() throws Exception {
            String token = tokenFor(saveUser(Role.PROVIDER));

            mockMvc.perform(withToken(get("/api/consultations/earnings-summary"), token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Seu usuário não está vinculado a um prestador. Procure o administrador."));
        }

        @Test
        @DisplayName("ADMIN com o providerId vê só aquele prestador, sem o byProvider")
        void adminWithProviderSeesOnlyThatProvider() throws Exception {
            Provider provider = newProvider();
            launchAsProvider(provider, "ATTENDED");

            mockMvc.perform(withToken(earnings(today, today).param("providerId", String.valueOf(provider.id())), adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultationCount").value(1))
                    .andExpect(jsonPath("$.payoutTotal").value(PAYOUT))
                    .andExpect(jsonPath("$.byProvider").isEmpty());
        }

        @Test
        @DisplayName("ADMIN com um providerId que não existe recebe 404")
        void adminWithUnknownProviderGetsNotFound() throws Exception {
            mockMvc.perform(withToken(earnings(today, today).param("providerId", "999999999"), adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sem token, o resumo e o rendimento respondem 401")
        void rejectsRequestWithoutToken() throws Exception {
            mockMvc.perform(get("/api/consultations/earnings-summary"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(earnings(today, today))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Listagem pela data do lançamento")
    class Listing {

        @Test
        @DisplayName("launchedFrom/launchedTo trazem o retroativo lançado hoje; from/to seguem pela data do atendimento")
        void filtersByLaunchDateOrByOccurredDate() throws Exception {
            Provider provider = newProvider();
            long id = launchAsAdmin(provider, today.minusDays(5), "ATTENDED");

            assertThat(idsOf(listing().param("launchedFrom", today.toString()).param("launchedTo", today.toString()), provider))
                    .containsExactly(id);
            assertThat(idsOf(listing().param("from", today.toString()).param("to", today.toString()), provider))
                    .isEmpty();
            assertThat(idsOf(listing().param("from", today.minusDays(5).toString()).param("to", today.minusDays(5).toString()), provider))
                    .containsExactly(id);
        }

        @Test
        @DisplayName("Filtro por status traz só o status escolhido, com a data do lançamento na resposta")
        void filtersByStatus() throws Exception {
            Provider provider = newProvider();
            launchAsProvider(provider, "ATTENDED");
            long noShow = launchAsProvider(provider, "NO_SHOW");

            MockHttpServletRequestBuilder request = listing()
                    .param("launchedFrom", today.toString()).param("launchedTo", today.toString())
                    .param("status", "NO_SHOW");

            assertThat(idsOf(request, provider)).containsExactly(noShow);
            mockMvc.perform(withToken(request, provider.token()))
                    .andExpect(jsonPath("$[0].status").value("NO_SHOW"))
                    .andExpect(jsonPath("$[0].launchedAt").isNotEmpty());
        }
    }

    // ---------------------------------------------------------------------------
    // Dados de cada teste: prestador com usuário próprio e um paciente sem contrato (Avulso)
    // ---------------------------------------------------------------------------

    private record Provider(long id, String token, long patientId) {
    }

    private Provider newProvider() throws Exception {
        UserEntity user = saveUser(Role.PROVIDER);
        String provider = read(post("/api/providers").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId": %d, "name": "Prestador %s"}
                """.formatted(user.getId(), shortId())), adminToken);
        String patient = read(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Paciente %s", "cpf": "%s", "birthDate": "1990-05-10"}
                """.formatted(shortId(), uniqueCpf())), adminToken);
        return new Provider(idOf(provider), tokenFor(user), idOf(patient));
    }

    // O PROVIDER lança com a data de hoje, em nome próprio
    private long launchAsProvider(Provider provider, String consultationStatus) throws Exception {
        return launch(provider.token(), null, provider.patientId(), today, consultationStatus);
    }

    // O ADMIN lança por um prestador, com qualquer data não futura (sem liberação)
    private long launchAsAdmin(Provider provider, LocalDate occurredAt, String consultationStatus) throws Exception {
        return launch(adminToken, provider.id(), provider.patientId(), occurredAt, consultationStatus);
    }

    private long launch(String token, Long providerId, long patientId, LocalDate occurredAt, String consultationStatus)
            throws Exception {
        String body = mockMvc.perform(withToken(post("/api/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"patientId": %d, "providerId": %s, "techniqueId": %d, "durationMinutes": %d,
                         "occurredAt": "%s", "status": "%s"}
                        """.formatted(patientId, providerId, techniqueId, DURATION_MINUTES, occurredAt, consultationStatus)), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private static MockHttpServletRequestBuilder earnings(LocalDate from, LocalDate to) {
        return get("/api/consultations/earnings").param("from", from.toString()).param("to", to.toString());
    }

    private static MockHttpServletRequestBuilder listing() {
        return get("/api/consultations");
    }

    // Ids da listagem vista pelo próprio prestador
    private List<Long> idsOf(MockHttpServletRequestBuilder request, Provider provider) throws Exception {
        List<Number> ids = JsonPath.read(read(request, provider.token()), "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    // Um campo do item do byProvider de um prestador (lista com um elemento, se ele estiver lá)
    private static List<Long> numbersOf(String body, Provider provider, String field) {
        List<Number> values = JsonPath.read(body, "$.byProvider[?(@.providerId == " + provider.id() + ")]." + field);
        return values.stream().map(Number::longValue).toList();
    }

    private String read(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(withToken(request, token))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String tokenFor(UserEntity user) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private UserEntity saveUser(Role role) {
        return userRepository.save(new UserEntity("usuario-" + UUID.randomUUID() + "@sublime.test", encodedPassword, role));
    }

    private static long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // CPF único (só o formato: o backend não confere os dígitos verificadores)
    private static String uniqueCpf() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 100_000_000_000L));
    }
}
