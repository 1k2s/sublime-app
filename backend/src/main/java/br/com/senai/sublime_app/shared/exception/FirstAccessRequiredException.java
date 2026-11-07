package br.com.senai.sublime_app.shared.exception;

// Lançada no login quando a senha provisória está correta, mas o usuário ainda precisa
// definir sua própria senha pessoal antes de receber o token JWT (ADR 0025). Mapeado para HTTP 403.
public class FirstAccessRequiredException extends RuntimeException {

    public FirstAccessRequiredException(String message) {
        super(message);
    }
}
