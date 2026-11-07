package br.com.senai.sublime_app.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.senai.sublime_app.auth.security.ProblemDetailAccessDeniedHandler;
import br.com.senai.sublime_app.auth.security.ProblemDetailAuthenticationEntryPoint;
import br.com.senai.sublime_app.auth.service.TokenService;
import br.com.senai.sublime_app.user.enums.Role;

// Regras de acesso da API: login e Swagger públicos; todo o resto exige o token JWT
// (validado pelo resource server do Spring a cada requisição) e o perfil certo. Sem sessão
// no servidor: cada requisição se identifica pelo token. Ver a seção "Segurança" do
// docs/architecture.md e o ADR 0023.
@Configuration
public class SecurityConfig {

    // Rotas que não exigem token
    private static final String[] PUBLIC_DOCS = { "/swagger-ui.html", "/swagger-ui/**", "/api-docs/**" };

    private static final String ADMIN = Role.ADMIN.name();
    private static final String PROVIDER = Role.PROVIDER.name();

    // Id só numérico: um {id} simples casaria com qualquer rota nova de um segmento
    // (ex: /api/consultations/resumo), que nasceria aberta ao PROVIDER
    private static final String CONSULTATION_BY_ID = "/api/consultations/{id:\\d+}";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // Token no header, não em cookie: sem CSRF
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(PUBLIC_DOCS).permitAll()
                // Página de erro interna do Spring: sem isto, um erro numa rota pública
                // viraria 401
                .requestMatchers("/error").permitAll()
                // ADMIN e PROVIDER: só o que as telas do prestador usam (Lançamentos e
                // Configurações). Rota a rota, sem curinga: endpoint novo não abre sozinho.
                // A restrição aos dados do próprio prestador fica nos services.
                .requestMatchers(HttpMethod.POST, "/api/consultations").hasAnyRole(ADMIN, PROVIDER)
                .requestMatchers(HttpMethod.GET, "/api/consultations", CONSULTATION_BY_ID,
                        "/api/consultations/eligible-contracts", "/api/consultations/session-balance",
                        "/api/consultations/patients").hasAnyRole(ADMIN, PROVIDER)
                .requestMatchers(HttpMethod.DELETE, CONSULTATION_BY_ID).hasAnyRole(ADMIN, PROVIDER)
                .requestMatchers(HttpMethod.GET, "/api/techniques").hasAnyRole(ADMIN, PROVIDER)
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole(ADMIN, PROVIDER)
                .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole(ADMIN, PROVIDER)
                // Todo o resto: só ADMIN. Esquecer de configurar um endpoint nega o acesso,
                // nunca o abre
                .anyRequest().hasRole(ADMIN))
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            // Também para quando a requisição nem traz o token
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    // Lê os perfis da claim "roles" (["ADMIN"]) como ROLE_ADMIN, para o hasRole("ADMIN")
    // da autorização por perfil funcionar
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Libera o front em desenvolvimento (Vite, porta 5173) para chamar a API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
