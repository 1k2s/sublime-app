package br.com.senai.sublime_app.auth.security;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import br.com.senai.sublime_app.auth.service.TokenService;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;

// Corte imediato do acesso: além da assinatura e da validade, cada requisição confere se
// o usuário do token continua ativo, sem pendência de primeiro acesso e com o mesmo perfil.
// Inativado, com senha redefinida para provisória ou com o perfil alterado, o token é
// recusado (401) e o usuário faz login de novo. Custo: uma consulta por chave primária.
@Component
public class ActiveUserTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_USER = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "O usuário do token não existe, está inativo ou teve o perfil alterado.", null);

    private final UserRepository userRepository;

    public ActiveUserTokenValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object userId = token.getClaim(TokenService.USER_ID_CLAIM);
        if (!(userId instanceof Number id)) {
            return OAuth2TokenValidatorResult.failure(INVALID_USER);
        }
        boolean valid = userRepository.findById(id.longValue())
                .map(user -> isActiveWithSameRole(user, token.getClaimAsStringList(TokenService.ROLES_CLAIM)))
                .orElse(false);
        return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INVALID_USER);
    }

    // O token carrega o perfil da hora do login; se o perfil mudou ou a senha voltou a ser provisória, ele não vale mais
    private static boolean isActiveWithSameRole(UserEntity user, List<String> tokenRoles) {
        return user.isActive()
                && !user.isFirstAccessPending()
                && tokenRoles != null
                && tokenRoles.equals(List.of(user.getRole().name()));
    }
}
