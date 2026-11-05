package br.com.senai.sublime_app.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// E-mail e senha do primeiro ADMIN (variáveis de ambiente ADMIN_EMAIL e ADMIN_PASSWORD).
// Só são usados quando não existe nenhum ADMIN ativo; nunca ficam no git.
@ConfigurationProperties("sublime.admin")
public record AdminProperties(String email, String password) {

    // As duas variáveis precisam estar definidas para criar o admin
    public boolean hasCredentials() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
