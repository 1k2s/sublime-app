package br.com.senai.sublime_app.patient.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientRequestDTO(

        @NotBlank(message = "O nome do paciente é obrigatório.")
        @Size(min = 3, max = 255, message = "O nome do paciente deve ter entre 3 e 255 caracteres.")
        String name,

        // Somente os 11 dígitos, sem pontuação
        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos, sem pontuação.")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate birthDate,

        String phone,

        @Email(message = "O e-mail informado é inválido.")
        String email,

        // Opcional no cadastro
        @Valid
        AddressDTO address

) {
}
