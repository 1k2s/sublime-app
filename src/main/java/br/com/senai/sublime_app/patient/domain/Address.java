package br.com.senai.sublime_app.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "street")
    private String street;

    @Column(name="number_house")
    private String numberHouse;

    @Column(name = "city")
    private String city;

    @Column(name="complement")
    private String complement;

    
    @Column(name = "cep")
    private String cep;
}
