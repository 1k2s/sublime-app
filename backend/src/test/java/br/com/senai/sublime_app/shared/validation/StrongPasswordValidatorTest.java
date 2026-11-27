package br.com.senai.sublime_app.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

// Regra do @StrongPassword: 8+ caracteres, maiúscula, minúscula, número e especial.
// Teste de unidade puro: sem Spring e sem banco.
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = { "Abcdefg1!", "Senha-Forte-2026", "Ação2026#", "1234567aA@" })
    @DisplayName("Senha com os cinco critérios é aceita (acentuadas contam como letras)")
    void acceptsStrongPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Abcde1!", // 7 caracteres
            "abcdefg1!", // sem maiúscula
            "ABCDEFG1!", // sem minúscula
            "Abcdefgh!", // sem número
            "Abcdefgh1", // sem especial
            "Abcdefg 1" // espaço não conta como especial
    })
    @DisplayName("Senha sem algum dos critérios é recusada")
    void rejectsWeakPasswords(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    @DisplayName("Senha não informada é aceita: a obrigatoriedade é do @NotBlank, e a vazia mantém a atual")
    void acceptsMissingPassword(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }
}
