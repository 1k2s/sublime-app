package br.com.senai.sublime_app.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// Senha forte: no mínimo 8 caracteres, com letra maiúscula, letra minúscula, número e
// caractere especial. A mesma regra em todas as senhas digitadas (cadastro, senha provisória,
// primeiro acesso, Configurações e o ADMIN_PASSWORD). Nula ou em branco é aceita: a
// obrigatoriedade fica com o @NotBlank, e nas alterações a senha vazia significa "manter".
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "A senha deve ter no mínimo 8 caracteres, com letra maiúscula, letra minúscula, "
            + "número e caractere especial.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
