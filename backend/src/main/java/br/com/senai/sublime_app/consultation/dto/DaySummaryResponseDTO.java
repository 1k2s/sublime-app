package br.com.senai.sublime_app.consultation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resumo do dia de um prestador, para a tela de lançamento: os atendimentos ativos com a
// data de hoje (nos três status, todos cobrados) e o repasse somado pelo backend
public record DaySummaryResponseDTO(

        // hoje, no fuso da clínica
        LocalDate date,
        int consultationCount,
        BigDecimal payoutTotal,
        // lançamentos retroativos que a liberação de hoje ainda permite (0 sem liberação)
        int retroactiveLaunchesRemaining

) {
}
