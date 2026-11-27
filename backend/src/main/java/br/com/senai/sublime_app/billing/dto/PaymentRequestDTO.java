package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

// Registro de um pagamento, de valor livre. As regras (líquido até o bruto, parcelado só no
// crédito, contrato do paciente) ficam na entidade; aqui, só o formato.
public record PaymentRequestDTO(

        @NotNull(message = "O paciente é obrigatório.")
        Long patientId,

        // Vazio = avulso (pagamento sem contrato)
        Long contractId,

        // Bruto: o que o paciente pagou
        @NotNull(message = "O valor do pagamento é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do pagamento deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O valor do pagamento deve ter no máximo 2 casas decimais.")
        BigDecimal amount,

        // Líquido: o que a clínica recebe (a tela preenche com o bruto)
        @NotNull(message = "O valor líquido é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor líquido deve ser de R$ 0,00 até o valor pago.")
        @Digits(integer = 8, fraction = 2, message = "O valor líquido deve ter no máximo 2 casas decimais.")
        BigDecimal netAmount,

        // Parcelas no cartão de crédito; vazio = 1 (à vista)
        Integer cardInstallments,

        @NotNull(message = "A data do pagamento é obrigatória.")
        LocalDate paidAt,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        PaymentMethod paymentMethod

) {
    public int cardInstallmentsOrOne() {
        return cardInstallments != null ? cardInstallments : 1;
    }
}
