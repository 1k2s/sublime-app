package br.com.senai.sublime_app.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sublimeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sublime Fisioterapia — API de Controle de Atendimento")
                        .description("""
                                API REST do sistema de controle de atendimento e repasse de honorários
                                da clínica **Sublime Fisioterapia**.
                                
                                ## Módulos disponíveis
                                - **Pacientes** — cadastro e gestão de pacientes
                                - **Contratos** — contratos entre paciente e clínica, com snapshot de preço e regra de exclusive arc
                                - **Prestadores** — cadastro de prestadores de serviço e comissões
                                - **Usuários** — autenticação e controle de acesso
                                
                                ## Regras gerais
                                - Datas no formato `YYYY-MM-DD`
                                - Soft delete: registros inativados são preservados no histórico
                                - Preços travados no momento da assinatura do contrato (não retroativos)
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
                ));
    }
}
