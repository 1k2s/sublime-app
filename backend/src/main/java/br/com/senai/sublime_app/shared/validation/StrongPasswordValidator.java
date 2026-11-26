package br.com.senai.sublime_app.shared.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Regra do @StrongPassword. As classes de caractere são Unicode: "Ç" conta como maiúscula e
// "ã" como minúscula. Especial é o que não é letra, número nem espaço (ex: ! @ # $ -).
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    public static final int MIN_LENGTH = 8;

    private static final Pattern UPPERCASE = Pattern.compile("\\p{Lu}");
    private static final Pattern LOWERCASE = Pattern.compile("\\p{Ll}");
    private static final Pattern NUMBER = Pattern.compile("\\p{N}");
    private static final Pattern SPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\s]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (isNotInformed(password)) {
            return true;
        }
        return hasMinLength(password)
                && contains(UPPERCASE, password)
                && contains(LOWERCASE, password)
                && contains(NUMBER, password)
                && contains(SPECIAL, password);
    }

    private boolean isNotInformed(String password) {
        return password == null || password.isBlank();
    }

    private boolean hasMinLength(String password) {
        return password.codePointCount(0, password.length()) >= MIN_LENGTH;
    }

    private boolean contains(Pattern pattern, String password) {
        return pattern.matcher(password).find();
    }
}
