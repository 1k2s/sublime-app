package br.com.senai.sublime_app.consultation.dto;

// O rendimento de hoje, da semana (segunda a domingo) e do mês do calendário, no fuso da
// clínica. Os limites de cada período vêm no from/to, para a tela não calcular
public record EarningsSummaryResponseDTO(

        EarningsResponseDTO today,
        EarningsResponseDTO week,
        EarningsResponseDTO month

) {
}
