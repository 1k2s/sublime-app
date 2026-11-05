package br.com.senai.sublime_app.auth.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.senai.sublime_app.auth.security.ActiveUserTokenValidator;
import br.com.senai.sublime_app.auth.service.TokenService;

// Geração e leitura do token com uma única chave secreta (HMAC SHA-256): o próprio
// sistema emite e valida, então não precisa de par de chaves (RSA). Ver a spec de
// autenticação.
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    // A cada requisição o token é conferido: assinatura, expiração e emissor (os padrões)
    // e, por fim, se o usuário continua ativo e com o mesmo perfil (corte imediato do
    // acesso ao inativar ou mudar o perfil)
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, ActiveUserTokenValidator activeUserTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(TokenService.ISSUER),
                activeUserTokenValidator));
        return decoder;
    }
}
