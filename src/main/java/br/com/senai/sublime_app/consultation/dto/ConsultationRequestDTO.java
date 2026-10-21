package br.com.senai.sublime_app.consultation.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

// Lançamento de atendimento. Não tem preço nem valores: a linha de preço é resolvida
// pelo sistema e os valores são calculados (ver domain-model.md, seção Consultation).
public record ConsultationRequestDTO(

        // Quem compareceu (titular ou beneficiário de um contrato, ou paciente sem contrato)
        @NotNull(message = "O paciente é obrigatório.")
        Long patientId,

        // Contrato escolhido entre os vinculados ao paciente na data (GET
        // /api/consultations/eligible-contracts). Obrigatório quando houver algum;
        // vazio só quando o paciente não tem contrato vinculado (Avulso).
        Long contractId,

        @NotNull(message = "O prestador é obrigatório.")
        Long providerId,

        @NotNull(message = "A técnica é obrigatória.")
        Long techniqueId,

        @NotNull(message = "A duração do atendimento é obrigatória.")
        @Min(value = 1, message = "A duração do atendimento deve ser de no mínimo 1 minuto.")
        Integer durationMinutes,

        @NotNull(message = "A data do atendimento é obrigatória.")
        @PastOrPresent(message = "A data do atendimento não pode ser futura.")
        LocalDate occurredAt,

        @NotNull(message = "O status do atendimento é obrigatório.")
        ConsultationStatus status

) {
}
