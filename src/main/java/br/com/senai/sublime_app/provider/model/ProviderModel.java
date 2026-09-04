package br.com.senai.sublime_app.provider.model;

import java.math.BigInteger;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// bigint id PK
//     bigint userId FK
//     string name
//     decimal commissionPercentage
//     boolean active

@Entity
@NoArgsConstructor 
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public class ProviderModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    //PlaceHolder for the name of the provider
    private String userId;

    
    private String name;

}
