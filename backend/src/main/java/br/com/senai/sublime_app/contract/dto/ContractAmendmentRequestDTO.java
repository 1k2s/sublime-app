package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import jakarta.validation.Valid;
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

        // Opcional: só para estender o fim (prorrogação). Vazio = mantém o fim atual
        // (ou o recalculado, se um item por duração for incluído).
        LocalDate endDate,

        @NotEmpty(message = "O contrato precisa ter pelo menos um item.")
        @Valid
        List<ContractItemRequestDTO> items

) {
}
