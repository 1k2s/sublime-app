package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.billing.domain.InstallmentState;
import br.com.senai.sublime_app.billing.domain.PatientAccountSummary;

// Um vencimento: a data e o que falta da parcela
public record NextDueResponseDTO(LocalDate dueDate, BigDecimal amount) {

    public static NextDueResponseDTO fromInstallment(InstallmentState installment) {
        return new NextDueResponseDTO(installment.dueDate(), installment.remaining().amount());
    }

    public static NextDueResponseDTO fromNextDue(PatientAccountSummary.NextDue nextDue) {
        return new NextDueResponseDTO(nextDue.dueDate(), nextDue.amount().amount());
    }
}
