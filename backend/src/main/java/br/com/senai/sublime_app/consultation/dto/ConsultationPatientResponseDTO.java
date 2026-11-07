package br.com.senai.sublime_app.consultation.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;

// Paciente na tela de lançamento (GET /api/consultations/patients): só o necessário para
// escolher quem compareceu, sem CPF, contato nem endereço (o PROVIDER também usa). A data
// de nascimento ajuda a distinguir pacientes com o mesmo nome.
public record ConsultationPatientResponseDTO(

        Long id,
        String name,
        LocalDate birthDate

) {
    public static ConsultationPatientResponseDTO fromEntity(PatientEntity patient) {
        return new ConsultationPatientResponseDTO(patient.getId(), patient.getName(), patient.getBirthDate());
    }
}
