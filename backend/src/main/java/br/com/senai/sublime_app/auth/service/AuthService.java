package br.com.senai.sublime_app.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import br.com.senai.sublime_app.auth.dto.LoginRequestDTO;
import br.com.senai.sublime_app.auth.dto.LoginResponseDTO;
import br.com.senai.sublime_app.auth.security.AuthenticatedUser;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    /**
     * Login por e-mail e senha. O AuthenticationManager confere o usuário (existe e está
     * ativo) e a senha contra o hash; qualquer falha lança AuthenticationException, que
     * vira o 401 "E-mail ou senha inválidos." (mensagem única, ver a spec). Com sucesso,
     * gera o token.
     */
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return LoginResponseDTO.from(tokenService.generate(user), user);
    }
}
