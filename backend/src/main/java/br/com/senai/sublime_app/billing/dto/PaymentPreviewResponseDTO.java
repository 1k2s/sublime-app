package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.billing.domain.Receivables;

// A taxa da maquininha (valor e % do bruto) e a previsão de recebimento: quando o líquido cai
// na conta da clínica
public record PaymentPreviewResponseDTO(

        BigDecimal fee,
        BigDecimal feePercentage,
        List<ReceivableDTO> receivables

) {
    public record ReceivableDTO(LocalDate date, BigDecimal amount) {
    }

    public static List<ReceivableDTO> fromReceivables(Receivables receivables) {
        return receivables.parts().stream()
                .map(part -> new ReceivableDTO(part.date(), part.amount().amount()))
                .toList();
    }
}
