package br.com.senai.sublime_app.shared.exception;

// Requisição válida, mas conflita com o estado atual do recurso
// (ex: CPF já cadastrado, paciente com contrato ativo). Mapeado para HTTP 409.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
