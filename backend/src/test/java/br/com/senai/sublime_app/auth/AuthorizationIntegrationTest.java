package br.com.senai.sublime_app.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.jayway.jsonpath.JsonPath;

import br.com.senai.sublime_app.IntegrationTest;
import br.com.senai.sublime_app.auth.service.TokenService;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.repository.UserRepository;

// Autorização por perfil e corte do acesso, pela API real (SecurityConfig e ADRs 0023, 0025
// e 0026): os tokens saem do login de verdade, não são montados à mão.
@IntegrationTest
class AuthorizationIntegrationTest {

    private static final String PASSWORD = "senha-de-teste";

    // Hash BCrypt da PASSWORD, calculado uma vez para todos os testes (o BCrypt é lento de
    // propósito), com o PasswordEncoder da própria aplicação
    private static String encodedPassword;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeAll
    static void encodePassword(@Autowired PasswordEncoder passwordEncoder) {
        encodedPassword = passwordEncoder.encode(PASSWORD);
    }

    @Test
    @DisplayName("Sem token, rota protegida responde 401 com ProblemDetail")
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Token malformado (nem é um JWT) responde 401 com ProblemDetail")
    void rejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer("abc")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // O prestador troca, no próprio token, o userId e o perfil pelos de um admin real e
    // mantém a assinatura original. O payload passaria pelo ActiveUserTokenValidator (o
    // admin existe, está ativo e é ADMIN): só a assinatura barra o ataque. O caso de
    // controle prova isso, assinando o mesmo payload com a chave certa.
    @Nested
    @DisplayName("Token com o payload alterado")
    class TamperedPayload {

        @Test
        @DisplayName("Trocar o usuário e o perfil mantendo a assinatura responde 401")
        void rejectsPayloadChangedWithOriginalSignature() throws Exception {
            String[] parts = loginAs(Role.PROVIDER).split("\\.");
            String forgedPayload = impersonateAdmin(parts[1], saveUser(Role.ADMIN));

            String forgedToken = parts[0] + "." + forgedPayload + "." + parts[2];

            mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(forgedToken)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Controle: o mesmo payload, assinado com a chave certa, é aceito")
        void acceptsSamePayloadWhenSignedWithTheRealKey() throws Exception {
            String[] parts = loginAs(Role.PROVIDER).split("\\.");
            String forgedPayload = impersonateAdmin(parts[1], saveUser(Role.ADMIN));

            String reSignedToken = parts[0] + "." + forgedPayload + "." + sign(parts[0] + "." + forgedPayload);

            mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(reSignedToken)))
                    .andExpect(status().isOk());
        }
    }

    @ParameterizedTest(name = "PROVIDER acessa GET {0}")
    @ValueSource(strings = { "/api/users/me", "/api/techniques" })
    @DisplayName("PROVIDER acessa as rotas das telas dele")
    void providerAccessesOwnRoutes(String path) throws Exception {
        String token = loginAs(Role.PROVIDER);

        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    // Inclui vizinhas das rotas liberadas: a liberação é rota a rota, sem curinga, e o {id}
    // do lançamento só casa com número. Cada rota daqui tem o caso positivo do ADMIN em
    // adminAccessesAdminRoutes (um 403 sozinho não distingue "só do admin" de "quebrada
    // para todos"), exceto /api/consultations/resumo, que não existe: só confere que o
    // PROVIDER não passa pelo {id}
    @ParameterizedTest(name = "PROVIDER recebe 403 em GET {0}")
    @ValueSource(strings = { "/api/users", "/api/contracts", "/api/providers", "/api/patients",
            "/api/techniques/all", "/api/consultations/resumo" })
    @DisplayName("PROVIDER recebe 403 nas rotas só do ADMIN")
    void providerIsForbiddenOnAdminRoutes(String path) throws Exception {
        String token = loginAs(Role.PROVIDER);

        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @ParameterizedTest(name = "ADMIN acessa GET {0}")
    @ValueSource(strings = { "/api/users", "/api/contracts", "/api/providers", "/api/patients",
            "/api/techniques/all" })
    @DisplayName("ADMIN acessa as rotas de administração")
    void adminAccessesAdminRoutes(String path) throws Exception {
        String token = loginAs(Role.ADMIN);

        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login com senha errada responde 401 sem dizer qual dado errou")
    void rejectsWrongPassword() throws Exception {
        UserEntity user = saveUser(Role.PROVIDER);

        mockMvc.perform(login(user.getEmail(), "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("Login com primeiro acesso pendente responde 403 com firstAccessRequired")
    void requiresFirstAccessForProvisionalPassword() throws Exception {
        UserEntity user = userRepository.save(UserEntity.withProvisionalPassword(
                uniqueEmail(), encodedPassword, Role.PROVIDER));

        mockMvc.perform(login(user.getEmail(), PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.firstAccessRequired").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // Tudo pela API, cada requisição na própria transação: confere que o validador do token
    // lê o estado gravado no banco, não um objeto já carregado
    @Test
    @DisplayName("Usuário inativado pelo admin perde o acesso na hora: o token emitido antes vira 401")
    void cutsAccessWhenUserIsDeactivated() throws Exception {
        UserEntity provider = saveUser(Role.PROVIDER);
        String providerToken = tokenFor(provider);
        String adminToken = loginAs(Role.ADMIN);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(providerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}", provider.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(providerToken)))
                .andExpect(status().isUnauthorized());
    }

    // Grava um usuário com senha definitiva e faz o login pela API
    private String loginAs(Role role) throws Exception {
        return tokenFor(saveUser(role));
    }

    private String tokenFor(UserEntity user) throws Exception {
        String body = mockMvc.perform(login(user.getEmail(), PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private UserEntity saveUser(Role role) {
        return userRepository.save(new UserEntity(uniqueEmail(), encodedPassword, role));
    }

    // Payload do token (Base64URL) com o userId e o perfil do admin no lugar dos do prestador
    private static String impersonateAdmin(String encodedPayload, UserEntity admin) {
        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String forged = JsonPath.parse(payload)
                .set("$." + TokenService.USER_ID_CLAIM, admin.getId())
                .set("$." + TokenService.ROLES_CLAIM, List.of(Role.ADMIN.name()))
                .jsonString();
        return base64Url(forged.getBytes(StandardCharsets.UTF_8));
    }

    // Assinatura HMAC SHA-256 com a chave do perfil de teste, como o JwtConfig faz
    private String sign(String headerAndPayload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(headerAndPayload.getBytes(StandardCharsets.US_ASCII)));
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static RequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // E-mail único: sem rollback, os usuários de um teste continuam no banco até o fim da
    // execução (ver a @IntegrationTest). UUID, e não um relógio, que pode repetir.
    private static String uniqueEmail() {
        return "usuario-" + UUID.randomUUID() + "@sublime.test";
    }
}
