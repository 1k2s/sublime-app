package br.com.senai.sublime_app.patient.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Value Object imutável: nenhum setter. Trocar de endereço é substituir a
// instância inteira (ver PatientEntity.updateAddress), nunca editar um campo
// isolado. equals/hashCode consideram todos os campos (padrão correto para VO,
// diferente de entidade, que se identifica só pelo id).
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Address {

    @Column(name = "street")
    private String street;

    @Column(name = "number_house")
    private String numberHouse;

    @Column(name = "city")
    private String city;

    @Column(name = "complement")
    private String complement;

    @Column(name = "cep")
    private String cep;

    // Construtor de negócio do VO: um endereço só existe completo (o complemento é o
    // único campo opcional)
    public Address(String street, String numberHouse, String city, String complement, String cep) {
        validateRequiredFieldsAndCep(street, numberHouse, city, cep);
        this.street = street;
        this.numberHouse = numberHouse;
        this.city = city;
        this.complement = complement;
        this.cep = cep;
    }

    // numberHouse é texto livre para aceitar "S/N". CEP só com os 8 dígitos, sem
    // hífen (mesmo formato do CPF: só dígitos). O DTO valida o mesmo para devolver
    // os erros junto com os demais campos.
    private static void validateRequiredFieldsAndCep(String street, String numberHouse, String city, String cep) {
        if (street == null || street.isBlank()) {
            throw new BusinessRuleException("A rua é obrigatória.");
        }
        if (numberHouse == null || numberHouse.isBlank()) {
            throw new BusinessRuleException("O número é obrigatório.");
        }
        if (city == null || city.isBlank()) {
            throw new BusinessRuleException("A cidade é obrigatória.");
        }
        if (cep == null || !cep.matches("\\d{8}")) {
            throw new BusinessRuleException("O CEP deve conter exatamente 8 dígitos, sem hífen.");
        }
    }
}
