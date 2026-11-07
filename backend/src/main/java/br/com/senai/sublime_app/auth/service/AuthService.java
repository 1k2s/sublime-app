package br.com.senai.sublime_app.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.auth.dto.FirstAccessRequestDTO;
import br.com.senai.sublime_app.auth.dto.LoginRequestDTO;
import br.com.senai.sublime_app.auth.dto.LoginResponseDTO;
import br.com.senai.sublime_app.auth.security.AuthenticatedUser;
import br.com.senai.sublime_app.shared.exception.FirstAccessRequiredException;
import br.com.senai.sublime_app.user.service.UserService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;

    public AuthService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    /**
     * Login por e-mail e senha. O AuthenticationManager confere o usuário (existe e está
     * ativo) e a senha contra o hash; qualquer falha lança AuthenticationException, que
     * vira o 401 "E-mail ou senha inválidos." (mensagem única, ver a spec). Se a senha
     * estiver correta mas for provisória (primeiro acesso pendente), lança FirstAccessRequiredException
     * (403 com firstAccessRequired = true) sem emitir token (ADR 0025).
     */
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        if (user.firstAccessPending()) {
            throw new FirstAccessRequiredException(
                    "Primeiro acesso pendente. Defina sua nova senha para continuar.");
        }

        return LoginResponseDTO.from(tokenService.generate(user), user);
    }

    /**
     * Conclui o primeiro acesso: confere o e-mail e a senha provisória, garante que o
     * usuário está com primeiro acesso pendente, grava a nova senha definitiva (desde que
     * diferente da provisória) e já devolve o token JWT para iniciar a sessão (ADR 0025).
     */
    @Transactional
    public LoginResponseDTO completeFirstAccess(FirstAccessRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.provisionalPassword()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        if (!user.firstAccessPending()) {
            throw new BadCredentialsException("Este usuário não está com primeiro acesso pendente.");
        }

        userService.completeFirstAccess(user.id(), dto.newPassword());
        return LoginResponseDTO.from(tokenService.generate(user), user);
    }
}
