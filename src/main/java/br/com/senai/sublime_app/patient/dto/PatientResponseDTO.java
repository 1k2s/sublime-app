package br.com.senai.sublime_app.patient.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDTO {

    private Long id;
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private AddressDTO address;
    private boolean active;

    public PatientResponseDTO(PatientEntity patient) {
        this.id = patient.getId();
        this.name = patient.getName();
        this.cpf = patient.getCpf();
        this.birthDate = patient.getBirthDate();
        this.phone = patient.getPhone();
        this.email = patient.getEmail();
        this.address = patient.getAddress() != null ? new AddressDTO(patient.getAddress()) : null;
        this.active = patient.isActive();
    }
}
