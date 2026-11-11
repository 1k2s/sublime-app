package br.com.senai.sublime_app.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

class PercentageTest {

    @Test
    @DisplayName("Aplica o percentual arredondando para 2 casas com HALF_UP")
    void appliesPercentageWithHalfUpRounding() {
        // Prepara: o cenário
        Percentage commission = Percentage.of(new BigDecimal("35"));

        // Executa: a ação testada
        BigDecimal payout = commission.applyTo(new BigDecimal("58.91"));

        // Confere: o resultado esperado (58.91 × 35% = 20.6185 → 20.62)
        assertThat(payout).isEqualByComparingTo("20.62");
    }

    @Test
    @DisplayName("Recusa percentual com mais de 2 casas decimais, sem arredondar")
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Percentage.of(new BigDecimal("35.555")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O percentual deve ter no máximo 2 casas decimais.");
    }
}
