package br.com.senai.sublime_app.patient.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;

public record PatientResponseDTO(

        Long id,
        String name,
        String cpf,
        LocalDate birthDate,
        String phone,
        String email,
        AddressDTO address,
        boolean active

) {
    public static PatientResponseDTO fromEntity(PatientEntity entity) {
        return new PatientResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getBirthDate(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress() != null ? AddressDTO.fromValueObject(entity.getAddress()) : null,
                entity.isActive());
    }
}
