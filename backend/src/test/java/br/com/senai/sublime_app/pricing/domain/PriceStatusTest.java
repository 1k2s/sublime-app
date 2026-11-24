package br.com.senai.sublime_app.pricing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PriceStatus;
import br.com.senai.sublime_app.pricing.enums.PricingModel;

// Regras de status da linha de preço (CURRENT, EXPIRED após 12 meses sem reajuste e CLOSED).
// Teste de unidade puro: sem Spring e sem banco.
class PriceStatusTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2025, 11, 18);

    @Test
    @DisplayName("Linha aberta dentro dos 12 meses (inclusive o aniversário de 12 meses) é CURRENT")
    void openPriceWithinTwelveMonthsIsCurrent() {
        SessionDurationPriceEntity durationPrice = durationPrice(VALID_FROM);
        SessionFrequencyPriceEntity frequencyPrice = frequencyPrice(VALID_FROM);

        assertThat(durationPrice.statusOn(VALID_FROM)).isEqualTo(PriceStatus.CURRENT);
        assertThat(durationPrice.statusOn(VALID_FROM.plusMonths(12))).isEqualTo(PriceStatus.CURRENT);
        assertThat(frequencyPrice.statusOn(VALID_FROM.plusMonths(12))).isEqualTo(PriceStatus.CURRENT);
    }

    @Test
    @DisplayName("Linha aberta com mais de 12 meses sem reajuste é EXPIRED")
    void openPriceOlderThanTwelveMonthsIsExpired() {
        SessionDurationPriceEntity durationPrice = durationPrice(VALID_FROM);
        SessionFrequencyPriceEntity frequencyPrice = frequencyPrice(VALID_FROM);
        LocalDate oneDayAfterYear = VALID_FROM.plusMonths(12).plusDays(1);

        assertThat(durationPrice.statusOn(oneDayAfterYear)).isEqualTo(PriceStatus.EXPIRED);
        assertThat(frequencyPrice.statusOn(oneDayAfterYear)).isEqualTo(PriceStatus.EXPIRED);
        // Continua aberta (isCurrent = true) até ser substituída por um reajuste
        assertThat(durationPrice.isCurrent()).isTrue();
        assertThat(frequencyPrice.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Linha encerrada por reajuste é sempre CLOSED, mesmo com mais de 12 meses")
    void closedPriceIsAlwaysClosed() {
        SessionDurationPriceEntity durationPrice = durationPrice(VALID_FROM);
        durationPrice.close(VALID_FROM.plusMonths(3));

        SessionFrequencyPriceEntity frequencyPrice = frequencyPrice(VALID_FROM);
        frequencyPrice.close(VALID_FROM.plusMonths(15));

        assertThat(durationPrice.statusOn(VALID_FROM.plusMonths(4))).isEqualTo(PriceStatus.CLOSED);
        assertThat(durationPrice.statusOn(VALID_FROM.plusYears(2))).isEqualTo(PriceStatus.CLOSED);
        assertThat(frequencyPrice.statusOn(VALID_FROM.plusYears(2))).isEqualTo(PriceStatus.CLOSED);
    }

    private static SessionDurationPriceEntity durationPrice(LocalDate validFrom) {
        PricingGroupEntity group = new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED);
        PlanEntity plan = new PlanEntity("Evolução", PlanType.PACKAGE, 12, null);
        return new SessionDurationPriceEntity(group, plan, 60, Money.of(new BigDecimal("180.00")), validFrom);
    }

    private static SessionFrequencyPriceEntity frequencyPrice(LocalDate validFrom) {
        PricingGroupEntity group = new PricingGroupEntity("Em Grupo", PricingModel.FREQUENCY_BASED);
        PlanEntity plan = new PlanEntity("Trimestral", PlanType.MONTHLY_FEE, null, 13);
        return new SessionFrequencyPriceEntity(group, plan, 2, Money.of(new BigDecimal("95.00")), validFrom);
    }
}
