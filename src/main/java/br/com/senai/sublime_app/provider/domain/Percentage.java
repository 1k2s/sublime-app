package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Value Object de percentual, no formato 0–100 (35.00 = 35%). Mesmo desenho do Money:
// record persistido por AttributeConverter (PercentageConverter), sem construtor vazio
// nem campos mutáveis. Fica no provider porque a comissão é conceito do prestador; o
// consultation (snapshot da comissão aplicada) já depende do provider.
// Aplicar o percentual a um valor (repasse) entra com o Consultation, junto com a
// decisão do modo de arredondamento.
public record Percentage(BigDecimal value) {

    private static final int SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    // Construtor compacto: roda antes de o record atribuir o campo. Reatribuir
    // "value" aqui muda o valor que será guardado (a versão normalizada).
    public Percentage {
        validateValue(value);
        value = value.setScale(SCALE);
    }

    // Fabrica que chama o constructor. Facilita a leitura comparado com "new Percentage(...)"
    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    // Mais de 2 casas é rejeitado em vez de arredondado: arredondar em silêncio
    // esconderia um valor digitado errado (a coluna é DECIMAL(5,2)).
    private static void validateValue(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("O percentual é obrigatório.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new BusinessRuleException("O percentual deve estar entre 0 e 100.");
        }
        if (value.stripTrailingZeros().scale() > SCALE) {
            throw new BusinessRuleException("O percentual deve ter no máximo " + SCALE + " casas decimais.");
        }
    }
}
