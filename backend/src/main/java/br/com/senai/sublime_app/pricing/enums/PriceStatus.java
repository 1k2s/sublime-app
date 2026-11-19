package br.com.senai.sublime_app.pricing.enums;

import java.time.LocalDate;

// Status de uma linha de preço numa data (regra centralizada, sem repetir comparação de data no front):
// - CLOSED: encerrada por reajuste (validTo preenchido);
// - EXPIRED: ainda aberta (validTo nulo), mas sem reajuste há mais de 12 meses desde validFrom
//   (sinaliza revisão na tela de preços; continua utilizável em contratos e lançamentos até ser reajustada);
// - CURRENT: aberta e dentro da janela de 12 meses.
public enum PriceStatus {

    CURRENT,
    EXPIRED,
    CLOSED;

    public static final int VALIDITY_MONTHS = 12;

    public static PriceStatus resolve(LocalDate validFrom, LocalDate validTo, LocalDate date) {
        if (validTo != null) {
            return CLOSED;
        }
        return validFrom.plusMonths(VALIDITY_MONTHS).isBefore(date) ? EXPIRED : CURRENT;
    }
}
