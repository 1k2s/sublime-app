package br.com.senai.sublime_app.auth.security;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import br.com.senai.sublime_app.auth.service.TokenService;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.enums.Role;

// Preenche o parâmetro CurrentUser dos controllers com o usuário do token (claims userId e
// roles). Assim, só o auth conhece o JWT: os módulos de negócio recebem o CurrentUser.
// Registrado no Spring MVC pelo CurrentUserWebConfig.
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    // O token já passou pelo ActiveUserTokenValidator: userId existe e roles tem o perfil
    // atual do usuário
    @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Jwt token = requireJwtAuthentication().getToken();
        Number userId = token.getClaim(TokenService.USER_ID_CLAIM);
        List<String> roles = token.getClaimAsStringList(TokenService.ROLES_CLAIM);
        return new CurrentUser(userId.longValue(), Role.valueOf(roles.get(0)));
    }

    // Sem token só acontece se um endpoint público pedir o CurrentUser: erro de
    // programação (500), não do usuário
    private static JwtAuthenticationToken requireJwtAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException(
                    "CurrentUser pedido numa requisição sem token JWT: o endpoint precisa exigir autenticação.");
        }
        return jwtAuthentication;
    }
}
