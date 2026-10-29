package br.com.senai.sublime_app.consultation.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.consultation.domain.PricingSource;

// Um contrato vinculado ao paciente na data do atendimento, para o modal do
// lançamento (no máximo dois: um como titular, um como beneficiário). Lista vazia →
// Avulso; um → selecionado automaticamente; dois → o usuário escolhe.
public record EligibleContractResponseDTO(

        Long contractId,
        // titular do contrato: a sessão sai sempre do contrato dele
        Long holderId,
        String holderName,
        // true = o paciente atendido é o titular; false = é o beneficiário
        boolean patientIsHolder,
        String planName,
        LocalDate endDate,
        // CONTRACT_ITEM = este contrato cobre o grupo da técnica e debita o item;
        // EQUIVALENT_PLAN = não cobre, o preço vem do de-para do plano dele
        PricingSource pricingSource,
        // saldo do item do grupo, só quando ele é por duração (o Grupo não tem saldo)
        Integer remainingSessions

) {
}
