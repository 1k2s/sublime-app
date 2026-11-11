package br.com.senai.sublime_app.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.senai.sublime_app.user.domain.CurrentUser;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    // O CurrentUser vem do token (CurrentUserArgumentResolver), não do request: sem isto, o
    // Swagger o mostraria como parâmetros de query (userId, role)
    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI sublimeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sublime Fisioterapia — API de Controle de Atendimento")
                        .description("""
                                API REST do controle de atendimento e repasse de honorários da clínica
                                **Sublime Fisioterapia**. As regras de negócio estão no `docs/domain-model.md`
                                do repositório; aqui, só como chamar cada endpoint.

                                ## Autenticação
                                Todas as rotas, exceto o login e o primeiro acesso, exigem o token. Faça o
                                login em **POST /api/auth/login**, clique em **Authorize** (cadeado, no topo) e
                                cole o `token`. Ele vale 8 horas.

                                ## Perfis
                                - **ADMIN:** todos os endpoints.
                                - **PROVIDER:** só os marcados com **"Perfis: ADMIN e PROVIDER"**, e só com os
                                  próprios dados. Nos demais, recebe **403**.

                                ## Formatos
                                - Datas `AAAA-MM-DD`; valores e percentuais com até 2 casas decimais (mais casas
                                  são rejeitadas, nunca arredondadas).
                                - Erros em ProblemDetail (RFC 7807): `detail` com a mensagem em português e,
                                  na validação, `errors` por campo.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Sublime Fisioterapia")
                                .email("contato@sublimefisioterapia.com.br"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://sublimefisioterapia.com.br")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Ambiente de desenvolvimento local")
                ))
                // Token JWT em todas as rotas (botão "Authorize"); o login tira a exigência
                // com @SecurityRequirements() vazio
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido pelo POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
