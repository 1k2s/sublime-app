package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Ponte entre o Percentage e a coluna DECIMAL: o Percentage fica livre de JPA e a
// coluna continua com o nome e a precisão definidos no @Column de cada campo.
// autoApply = true: o Hibernate usa este converter em todo campo do tipo Percentage,
// sem precisar de @Convert campo a campo.
@Converter(autoApply = true)
public class PercentageConverter implements AttributeConverter<Percentage, BigDecimal> {

    // Java → banco: chamado ao salvar (INSERT/UPDATE)
    @Override
    public BigDecimal convertToDatabaseColumn(Percentage percentage) {
        return percentage == null ? null : percentage.value();
    }

    // Banco → Java: chamado ao ler (SELECT). Passa pelo Percentage.of, então um valor
    // inválido no banco é barrado aqui em vez de circular pelo sistema.
    @Override
    public Percentage convertToEntityAttribute(BigDecimal value) {
        return value == null ? null : Percentage.of(value);
    }
}
