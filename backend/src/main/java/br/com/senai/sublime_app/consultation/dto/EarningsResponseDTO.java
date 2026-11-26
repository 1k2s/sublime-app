package br.com.senai.sublime_app.consultation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Rendimento de um período, pela data do lançamento: os atendimentos realizados (o
// rendimento) e as faltas cobradas à parte, já somados pelo backend (o front não soma).
// Ver "Rendimento" em Consultation, no domain-model.
public record EarningsResponseDTO(

        // período, inclusivo, pela data do lançamento
        LocalDate from,
        LocalDate to,
        // realizados (ATTENDED), ativos: o rendimento
        long consultationCount,
        BigDecimal payoutTotal,
        // faltas cobradas (NO_SHOW e CANCELED_LATE), ativas: repasse à parte
        long chargedAbsenceCount,
        BigDecimal chargedAbsencePayoutTotal,
        // o repasse inteiro do período: rendimento + faltas cobradas
        BigDecimal totalPayout,
        // só para o ADMIN sem prestador escolhido: um item por prestador (vazio nos demais casos)
        List<ProviderEarningsResponseDTO> byProvider

) {
}
