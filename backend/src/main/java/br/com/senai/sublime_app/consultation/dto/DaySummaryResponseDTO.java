package br.com.senai.sublime_app.consultation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resumo do dia de um prestador, para a tela de lançamento: o rendimento de hoje, pela mesma
// regra do dashboard (realizados, pela data do lançamento), com as faltas cobradas à parte
public record DaySummaryResponseDTO(

        // hoje, no fuso da clínica
        LocalDate date,
        // realizados (ATTENDED), ativos, lançados hoje
        long consultationCount,
        BigDecimal payoutTotal,
        // faltas cobradas (NO_SHOW e CANCELED_LATE), ativas, lançadas hoje
        long chargedAbsenceCount,
        BigDecimal chargedAbsencePayoutTotal,
        // lançamentos retroativos que a liberação de hoje ainda permite (0 sem liberação)
        int retroactiveLaunchesRemaining

) {
}
