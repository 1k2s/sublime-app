package br.com.senai.sublime_app.patient.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;

public record PatientResponseDTO(

        Long id,
        String name,
        String cpf,
        LocalDate birthDate,
        String phone,
        String email,
        AddressDTO address,
        // origem: prestador que trouxe o paciente; null = paciente da clínica
        Long referringProviderId,
        String referringProviderName,
        boolean active

) {
    public static PatientResponseDTO fromEntity(PatientEntity entity) {
        ProviderEntity referringProvider = entity.getReferringProvider();
        return new PatientResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getBirthDate(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress() != null ? AddressDTO.fromValueObject(entity.getAddress()) : null,
                referringProvider != null ? referringProvider.getId() : null,
                referringProvider != null ? referringProvider.getName() : null,
                entity.isActive());
    }
}
