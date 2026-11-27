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

// Estado completo da nova versão do contrato (aditivo). O frontend abre o formulário
// preenchido com a versão atual, o usuário altera o que quiser e envia tudo — assim
// a nova versão passa pelas mesmas regras da criação e não há ambiguidade entre
// "campo não enviado" e "campo removido" (item que não vier = item removido).
// Não tem titular, plano nem início: são copiados da versão atual e nunca mudam.
public record ContractAmendmentRequestDTO(

        // nullable: Pilates em Dupla / plano Familiar
        Long beneficiaryId,

        @NotNull(message = "O método de pagamento é obrigatório.")
        PaymentMethod paymentMethod,

        // Escolhidas em todo aditivo (dividem o que falta pagar: o total da nova versão − o pago):
        // de 1 até os meses do 1º vencimento ao novo fim (a entidade confere depois de calcular o
        // fim); 12 no máximo
        @NotNull(message = "O número de parcelas é obrigatório.")
        @Min(value = 1, message = "O número de parcelas deve ser no mínimo 1.")
        @Max(value = Installments.MAX_COUNT, message = "O número de parcelas deve ser no máximo 12.")
        Integer installments,

        // Opcional: só para estender o fim (prorrogação). Vazio = mantém o fim atual
        // (ou o recalculado, se um item por duração for incluído).
        LocalDate endDate,

        // Opcional: o 1º vencimento da nova versão (a partir da data do aditivo); vazio = o
        // sugerido (o dia de vencimento atual, neste mês ou no próximo)
        LocalDate firstDueDate,

        @NotEmpty(message = "O contrato precisa ter pelo menos um item.")
        @Valid
        List<ContractItemRequestDTO> items

) {
}
