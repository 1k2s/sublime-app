package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "provider")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderEntity {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

   
    @OneToOne(fetch = FetchType.LAZY, optional = false) // cardinalidade 1:1 (lazy para pegar somente o usuario do bando) optional false para não permitir nulo
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um provider com os dados obrigatórios
    public ProviderEntity(UserEntity user, String name, BigDecimal commissionPercentage) {
        // Todo prestador se autentica por um User; o vínculo é fixo (update não o recebe)
        if (user == null) {
            throw new BusinessRuleException("Provider must be linked to a user.");
        }
        validateName(name);
        validateCommission(commissionPercentage);
        this.user = user;
        this.name = name;
        this.commissionPercentage = commissionPercentage;
        this.active = true;
    }

    //update publico para atualizar os dados do provider
    public void update(String name, BigDecimal commissionPercentage) {
        validateName(name);
        validateCommission(commissionPercentage);
        this.name = name;
        this.commissionPercentage = commissionPercentage;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Provider name is required.");
        }
    }

    // Invariante do prestador: a comissão entra no cálculo do repasse de cada
    // atendimento, então nunca pode ficar fora de 0–100%, venha de onde vier.
    // O DTO valida o mesmo intervalo para devolver o erro junto com os demais campos.
    private static void validateCommission(BigDecimal commissionPercentage) {
        if (commissionPercentage == null
                || commissionPercentage.compareTo(BigDecimal.ZERO) < 0
                || commissionPercentage.compareTo(ONE_HUNDRED) > 0) {
            throw new BusinessRuleException("commissionPercentage must be between 0 and 100.");
        }
    }
}
