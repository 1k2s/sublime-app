package br.com.senai.sublime_app.consultation.dto;

import java.math.BigDecimal;

// Rendimento de um prestador no período (a visão do ADMIN com todos os prestadores). Os
// prestadores ativos aparecem mesmo sem lançamento, zerados; um inativo só com lançamento
public record ProviderEarningsResponseDTO(

        Long providerId,
        String providerName,
        long consultationCount,
        BigDecimal payoutTotal,
        long chargedAbsenceCount,
        BigDecimal chargedAbsencePayoutTotal,
        BigDecimal totalPayout

) {
}
