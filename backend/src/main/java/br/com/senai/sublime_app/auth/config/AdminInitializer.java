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

// Primeiro admin: com todas as rotas protegidas, ninguém conseguiria criar o primeiro
// usuário pela API. Ao subir, se não houver nenhum ADMIN ativo, cria um com ADMIN_EMAIL e
// ADMIN_PASSWORD, pelo UserService (já com senha definitiva, sem exigir primeiro acesso).
// Ver o "Primeiro admin" no domain-model (seção User) e os ADRs 0023 e 0025.
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
        if (userService.existsActiveUserWithRole(Role.ADMIN)) {
            return;
        }
        if (!properties.hasCredentials()) {
            log.warn("Nenhum usuário ADMIN ativo e ADMIN_EMAIL/ADMIN_PASSWORD não definidos: nenhum admin "
                    + "foi criado, e ninguém conseguirá fazer login. Defina as variáveis e reinicie a aplicação.");
            return;
        }
        createAdmin(new UserRequestDTO(properties.email(), properties.password(), Role.ADMIN));
    }

    private void createAdmin(UserRequestDTO admin) {
        validateCredentials(admin);
        try {
            userService.createDefinitiveAdmin(admin);
            log.info("Primeiro admin criado: {}", admin.email());
        } catch (ConflictException e) {
            log.warn("Nenhum ADMIN ativo, mas o e-mail {} já pertence a outro usuário (inativo ou com outro "
                    + "perfil): nenhum admin foi criado. Use outro ADMIN_EMAIL.", admin.email());
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
