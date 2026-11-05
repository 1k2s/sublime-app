package br.com.senai.sublime_app.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuração do token, lida do application.properties (prefixo "jwt"). O segredo vem
// da variável de ambiente JWT_SECRET, nunca do git. Sem segredo válido, a aplicação não
// sobe: é melhor falhar ao iniciar do que emitir tokens fáceis de forjar.
@ConfigurationProperties("jwt")
public record JwtProperties(String secret, Duration expiration) {

    // HS256 exige uma chave de pelo menos 256 bits
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        validateSecret(secret);
        validateExpiration(expiration);
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret precisa ter no mínimo " + MIN_SECRET_BYTES
                    + " bytes (256 bits). Defina a variável de ambiente JWT_SECRET (ver docs/development.md).");
        }
    }

    private static void validateExpiration(Duration expiration) {
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("jwt.expiration precisa ser uma duração positiva (ex: 8h).");
        }
    }
}
