package br.com.senai.sublime_app.shared.exception;

// Recurso referenciado pelo cliente não existe (mapeado para HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
