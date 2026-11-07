package br.com.senai.sublime_app.auth.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.com.senai.sublime_app.auth.security.CurrentUserArgumentResolver;

// Registra o CurrentUserArgumentResolver: com ele, um parâmetro CurrentUser no método do
// controller chega preenchido com o usuário logado.
@Configuration
public class CurrentUserWebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public CurrentUserWebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
