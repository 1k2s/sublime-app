package br.com.senai.sublime_app.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
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
}
