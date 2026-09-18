package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;

import br.com.senai.sublime_app.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "providers")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

   
    @OneToOne(fetch = FetchType.LAZY, optional = false) // cardinalidade 1:1 (lazy para pegar somente o usuario do bando) optional false para não permitir nulo
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um provider com os dados obrigatórios
    public ProviderEntity(UserEntity user, String name, BigDecimal commissionPercentage) {
        this.user = user;
        this.name = name;
        this.commissionPercentage = commissionPercentage;
        this.active = true;
    }

    //update publico para atualizar os dados do provider
    public void update(String name, BigDecimal commissionPercentage) {
        this.name = name;
        this.commissionPercentage = commissionPercentage;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
