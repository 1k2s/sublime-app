package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.Installments;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Criação de contrato. Não tem endDate: o fim é calculado pelo sistema a partir do
// plano e da frequência dos itens (ver domain-model.md, "Vigência e frequência").
public record ContractRequestDTO(

        @NotNull(message = "O paciente titular é obrigatório.")
        Long patientId,

        // nullable: Pilates em Dupla / plano Familiar
        Long beneficiaryId,

        // Plano único do contrato (nunca o avulso). Os itens por duração usam linhas dele.
        @NotNull(message = "O plano do contrato é obrigatório.")
        Long planId,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate startDate,

        @NotNull(message = "O método de pagamento é obrigatório.")
        PaymentMethod paymentMethod,

        // De 1 até os meses do contrato (a entidade confere o máximo do contrato); 12 no máximo
        @NotNull(message = "O número de parcelas é obrigatório.")
        @Min(value = 1, message = "O número de parcelas deve ser no mínimo 1.")
        @Max(value = Installments.MAX_COUNT, message = "O número de parcelas deve ser no máximo 12.")
        Integer installments,

        // Um item por grupo de preço contratado
        @NotEmpty(message = "O contrato precisa ter pelo menos um item.")
        @Valid
        List<ContractItemRequestDTO> items

) {
}
