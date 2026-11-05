package br.com.senai.sublime_app.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI sublimeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sublime Fisioterapia — API de Controle de Atendimento")
                        .description("""
                                API REST do sistema de controle de atendimento e repasse de honorários
                                da clínica **Sublime Fisioterapia**.
                                
                                ## Autenticação
                                Todas as rotas, exceto o login, exigem o token JWT. Faça o login em
                                **Autenticação → POST /api/auth/login**, copie o `token` da resposta, clique em
                                **Authorize** (cadeado, no topo) e cole o token. Ele vale 8 horas.

                                ## Módulos disponíveis
                                - **Autenticação** — login (e-mail e senha) e token JWT
                                - **Pacientes** — cadastro e gestão de pacientes
                                - **Grupos de preço** — agrupamento das técnicas que compartilham a mesma tabela de preço
                                - **Técnicas e preços** — cadastro de técnicas e consulta da tabela de preços vigente
                                - **Planos** — planos usados nas tabelas de preço
                                - **Cadastro de preços** — preço novo e reajuste (historizados, nunca alterados)
                                - **Contratos** — contratos entre paciente e clínica, com snapshot de preço e regra de exclusive arc
                                - **Prestadores** — cadastro de prestadores de serviço e comissões
                                - **Atendimentos** — lançamento dos atendimentos, repasse e saldo de sessões
                                - **Usuários** — usuários de acesso ao sistema

                                ## Regras gerais
                                - Datas no formato `YYYY-MM-DD`
                                - Valores em reais e percentuais com no máximo 2 casas decimais (mais casas são rejeitadas, nunca arredondadas)
                                - Soft delete: registros inativados são preservados no histórico
                                - Preços travados no momento da assinatura do contrato (não retroativos)
                                - Erros no formato ProblemDetail (RFC 7807), com mensagens em português
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
