package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.billing.domain.AccountWarning;

// Um aviso da conta: o tipo e os dados para a tela montar o texto. Os campos que não se aplicam ao
// tipo vêm nulos
public record AccountWarningResponseDTO(

        AccountWarning.Type type,
        Integer count,
        BigDecimal amount,
        LocalDate date,
        String techniqueName,
        Integer contractedCount

) {
    public static AccountWarningResponseDTO fromWarning(AccountWarning warning) {
        return new AccountWarningResponseDTO(warning.type(), warning.count(),
                warning.amount() != null ? warning.amount().amount() : null, warning.date(), warning.techniqueName(),
                warning.contractedCount());
    }
}
