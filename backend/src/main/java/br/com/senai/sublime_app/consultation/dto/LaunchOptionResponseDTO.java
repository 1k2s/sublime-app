package br.com.senai.sublime_app.consultation.dto;

import java.util.List;

import br.com.senai.sublime_app.consultation.domain.PricingSource;

// Uma opção do lançamento para o paciente, a técnica e a data (nada é gravado): um contrato
// vinculado ou, sem contrato, o Avulso. Uma opção → usada direto; duas (titular e
// beneficiário) → o usuário escolhe. Traz as durações com preço, para a tela não oferecer
// uma que daria "Não há preço...". Ver "Contrato do atendimento" em Consultation, no
// domain-model.
public record LaunchOptionResponseDTO(

        // nulo = Avulso (paciente sem contrato vinculado na data)
        Long contractId,
        // titular do contrato (a sessão sai sempre do contrato dele); nulo no Avulso
        Long holderId,
        String holderName,
        // true = o paciente atendido é o titular; false = é o beneficiário (ou Avulso)
        boolean patientIsHolder,
        String planName,
        // o caso do preço que esta opção daria
        PricingSource pricingSource,
        // saldo do item por duração do contrato (aviso de saldo acabando); nulo sem saldo
        Integer remainingSessions,
        // durações com preço nesta opção, em ordem; vazia quando qualquer duração vale
        List<Integer> durationOptions,
        // true no Pilates em Grupo: a duração só registra a aula, sem preço por duração
        boolean anyDurationAllowed,
        // a duração travada do item (caso 1), a única opção, ou 60 no Pilates em Grupo
        Integer suggestedDurationMinutes,
        // preenchido quando não dá para lançar por esta opção (sem preço): a mensagem da regra
        String unavailableReason

) {
}
