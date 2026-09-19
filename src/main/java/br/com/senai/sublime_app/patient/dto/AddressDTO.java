package br.com.senai.sublime_app.patient.dto;

import br.com.senai.sublime_app.patient.domain.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Usado tanto na entrada quanto na saída: o endereço não tem identidade própria,
// então não há motivo para separar em request/response.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private String street;
    private String numberHouse;
    private String city;
    private String complement;
    private String cep;

    public AddressDTO(Address address) {
        this.street = address.getStreet();
        this.numberHouse = address.getNumberHouse();
        this.city = address.getCity();
        this.complement = address.getComplement();
        this.cep = address.getCep();
    }

    public Address toValueObject() {
        return new Address(street, numberHouse, city, complement, cep);
    }
}
