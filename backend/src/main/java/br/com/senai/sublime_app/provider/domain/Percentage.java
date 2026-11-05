package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Value Object de percentual, no formato 0–100 (35.00 = 35%). Mesmo desenho do Money:
// record persistido por AttributeConverter (PercentageConverter), sem construtor vazio
// nem campos mutáveis. Fica no provider porque a comissão é conceito do prestador; o
// consultation (snapshot da comissão aplicada) já depende do provider.
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

    // Comparação entre percentuais (ex: o de quem trouxe o paciente nunca é menor que o padrão)
    public boolean isLessThan(Percentage other) {
        return value.compareTo(other.value) < 0;
    }

    // Aplica o percentual a um valor (ex: repasse = valor da sessão × comissão),
    // arredondado para 2 casas com HALF_UP (arredondamento comercial: o prestador
    // confere na calculadora). Ex: 35% de 58.91 = 20.6185 → 20.62. Aqui arredondar é
    // certo: é uma conta, não um valor digitado. Recebe BigDecimal, e não Money, para
    // o provider não depender do pricing; quem chama embrulha o resultado no Money.
    public BigDecimal applyTo(BigDecimal amount) {
        return amount.multiply(value).divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
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
