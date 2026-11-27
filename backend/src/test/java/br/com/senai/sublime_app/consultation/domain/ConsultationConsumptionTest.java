package br.com.senai.sublime_app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.pricing.domain.Money;

// O que conta como consumido na conta do paciente (ver "Saldo de sessões" em Consultation, no
// domain-model). Teste de unidade puro.
class ConsultationConsumptionTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 26);

    @Test
    @DisplayName("Conta o item por duração, o de-para e o avulso; não conta a aula do Grupo")
    void countsDurationItemEquivalentPlanAndSingleSession() {
        assertThat(row(PricingSource.CONTRACT_ITEM, true).countsAsConsumed()).isTrue();
        assertThat(row(PricingSource.EQUIVALENT_PLAN, true).countsAsConsumed()).isTrue();
        assertThat(row(PricingSource.EQUIVALENT_PLAN, false).countsAsConsumed()).isTrue();
        assertThat(row(PricingSource.SINGLE_SESSION, true).countsAsConsumed()).isTrue();
        assertThat(row(PricingSource.CONTRACT_ITEM, false).countsAsConsumed()).isFalse();
    }

    @Test
    @DisplayName("A soma deixa de fora a aula do Grupo; zero na lista vazia")
    void sumLeavesGroupClassesOut() {
        List<ConsultationConsumption> rows = List.of(
                row(PricingSource.CONTRACT_ITEM, true),
                row(PricingSource.CONTRACT_ITEM, false),
                row(PricingSource.EQUIVALENT_PLAN, true));

        assertThat(ConsultationConsumption.sumConsumed(rows).amount()).isEqualByComparingTo("200.00");
        assertThat(ConsultationConsumption.sumConsumed(List.of()).amount()).isEqualByComparingTo("0.00");
    }

    private static ConsultationConsumption row(PricingSource source, boolean durationBased) {
        return new ConsultationConsumption(1L, 10L, 100L, source, Money.of(new BigDecimal("100.00")), durationBased,
                20L, DAY);
    }
}
