package br.com.senai.sublime_app.patient.dto;

import br.com.senai.sublime_app.patient.domain.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Usado tanto na entrada quanto na saída: o endereço não tem identidade própria,
// então não há motivo para separar em request/response. A conversão para o Value
// Object (entrada) fica no PatientService.
public record AddressDTO(

        @NotBlank(message = "A rua é obrigatória.")
        String street,

        // Texto livre para aceitar "S/N"
        @NotBlank(message = "O número é obrigatório.")
        String numberHouse,

        @NotBlank(message = "A cidade é obrigatória.")
        String city,

        // Único campo opcional do endereço
        String complement,

        // Somente os 8 dígitos, sem hífen
        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 dígitos, sem hífen.")
        String cep

) {
    public static AddressDTO fromValueObject(Address address) {
        return new AddressDTO(
                address.getStreet(),
                address.getNumberHouse(),
                address.getCity(),
                address.getComplement(),
                address.getCep());
    }
}
