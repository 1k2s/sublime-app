package br.com.senai.sublime_app.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.Installments;

// Uma parcela do cronograma do contrato: o número (1 = a primeira), o vencimento e o valor. Sem a
// situação (paga, em aberto): o contract não lê os pagamentos; ela fica no Financeiro (billing)
public record InstallmentResponseDTO(int number, LocalDate dueDate, BigDecimal value) {

    public static List<InstallmentResponseDTO> fromSchedule(List<Installments.Scheduled> schedule) {
        return schedule.stream()
                .map(installment -> new InstallmentResponseDTO(installment.number(), installment.dueDate(),
                        installment.value().amount()))
                .toList();
    }
}
