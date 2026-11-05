package br.com.senai.sublime_app.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.senai.sublime_app.auth.config.JwtProperties;
import br.com.senai.sublime_app.auth.security.AuthenticatedUser;

// Só GERA o token. Quem valida a cada requisição é o resource server do Spring
// (JwtDecoder, em JwtConfig).
@Service
public class TokenService {

    public static final String ISSUER = "sublime-app";
    public static final String USER_ID_CLAIM = "userId";
    // Lista, não texto: é o formato que o conversor de perfis do resource server lê
    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public GeneratedToken generate(AuthenticatedUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.email())
                .claim(USER_ID_CLAIM, user.id())
                .claim(ROLES_CLAIM, List.of(user.role().name()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        // HS256 explícito no cabeçalho: sem ele, o encoder tentaria uma chave RSA
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new GeneratedToken(value, expiresAt);
    }

    // O token e quando ele vence (o front guarda a expiração para encerrar a sessão)
    public record GeneratedToken(String value, Instant expiresAt) {
    }
}
