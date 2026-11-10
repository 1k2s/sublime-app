package br.com.senai.sublime_app.shared.exception;

// O usuário logado não pode fazer esta ação com estes dados (ex: PROVIDER lançando
// atendimento para outro prestador). Mapeado para HTTP 403. O acesso por perfil a cada
// endpoint é do SecurityConfig; esta exception é para as regras que dependem dos dados,
// conferidas no service.
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
