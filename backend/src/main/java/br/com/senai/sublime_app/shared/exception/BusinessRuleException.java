package br.com.senai.sublime_app.shared.exception;

// Requisição viola uma regra de negócio ou invariante de domínio (mapeado para HTTP 400)
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
