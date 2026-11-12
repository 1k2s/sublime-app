package br.com.senai.sublime_app.auth.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.enums.Role;
import br.com.senai.sublime_app.user.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

// Admin padrão: com todas as rotas protegidas, ninguém conseguiria criar o primeiro
// usuário pela API. Na primeira subida (ainda sem admin padrão no banco), cria um com
// ADMIN_EMAIL e ADMIN_PASSWORD, pelo UserService (mesmas regras do cadastro: hash, e-mail
// único), marcado como admin padrão (não pode ser inativado nem mudar de perfil) e já com
// senha definitiva (sem exigir primeiro acesso). Depois disso as variáveis são ignoradas
// (o banco é a fonte da verdade). Ver o "Admin padrão" no domain-model (seção User) e os
// ADRs 0023, 0025 e 0027.
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;
    private final AdminProperties properties;
    private final Validator validator;

    public AdminInitializer(UserService userService, AdminProperties properties, Validator validator) {
        this.userService = userService;
        this.properties = properties;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.existsDefaultAdmin()) {
            return;
        }
        if (!properties.hasCredentials()) {
            log.warn("Nenhum admin padrão e ADMIN_EMAIL/ADMIN_PASSWORD não definidos: nenhum admin "
                    + "foi criado, e ninguém conseguirá fazer login. Defina as variáveis e reinicie a aplicação.");
            return;
        }
        createAdmin(new UserRequestDTO(properties.email(), properties.password(), Role.ADMIN));
    }

    private void createAdmin(UserRequestDTO admin) {
        validateCredentials(admin);
        try {
            userService.createDefaultAdmin(admin.email(), admin.password());
            log.info("Admin padrão criado: {}", admin.email());
        } catch (ConflictException e) {
            log.warn("Nenhum admin padrão, mas o e-mail {} já pertence a outro usuário: nenhum admin foi "
                    + "criado. Use outro ADMIN_EMAIL.", admin.email());
        }
    }

    // Chamar o UserService direto pula o @Valid do controller: as mesmas regras do cadastro
    // (e-mail válido, senha com 8+ caracteres) são aplicadas aqui. Variáveis inválidas
    // impedem a aplicação de subir, como o JWT_SECRET.
    private void validateCredentials(UserRequestDTO admin) {
        Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(admin);
        if (!violations.isEmpty()) {
            String messages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(" "));
            throw new IllegalStateException("ADMIN_EMAIL/ADMIN_PASSWORD inválidos: " + messages);
        }
    }
}
