package br.com.senai.sublime_app.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 403: logado, mas sem permissão para o recurso. Só passa a acontecer com a autorização
// por perfil (feature seguinte); o tratamento já fica pronto.
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private static final String DETAIL = "Você não tem permissão para acessar este recurso.";

    private final ProblemDetailResponseWriter writer;

    public ProblemDetailAccessDeniedHandler(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        writer.write(request, response, HttpStatus.FORBIDDEN, DETAIL);
    }
}
