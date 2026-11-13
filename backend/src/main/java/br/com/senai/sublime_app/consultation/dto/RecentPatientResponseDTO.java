package br.com.senai.sublime_app.consultation.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;

// Paciente recente do prestador, com a técnica e a duração do último atendimento ativo
// dele: a tela sugere as duas ao escolher o paciente (o status não é sugerido)
public record RecentPatientResponseDTO(

        Long patientId,
        String patientName,
        Long techniqueId,
        String techniqueName,
        int durationMinutes,
        LocalDate lastOccurredAt

) {
    public static RecentPatientResponseDTO fromEntity(ConsultationEntity consultation) {
        return new RecentPatientResponseDTO(
                consultation.getPatient().getId(),
                consultation.getPatient().getName(),
                consultation.getTechnique().getId(),
                consultation.getTechnique().getName(),
                consultation.getDurationMinutes(),
                consultation.getOccurredAt());
    }
}
