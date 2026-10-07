package br.com.senai.sublime_app.pricing.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "technique")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TechniqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Fixo após a criação (sem método que o altere): trocar o grupo mudaria o preço
    // dos atendimentos futuros. Reclassificar = nova técnica + inativar a antiga.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pricing_group_id", nullable = false)
    private PricingGroupEntity pricingGroup;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar uma técnica com os dados obrigatórios
    public TechniqueEntity(String name, PricingGroupEntity pricingGroup) {
        validateNameAndPricingGroup(name, pricingGroup);
        this.name = name;
        this.pricingGroup = pricingGroup;
        this.active = true;
    }

    // Soft delete: contratos e atendimentos antigos continuam referenciando a técnica
    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // Inativar duas vezes não é uma operação válida: 409 avisa que o estado já era esse
    private void requireActive() {
        if (!active) {
            throw new ConflictException("A técnica já está inativa.");
        }
    }

    private static void validateNameAndPricingGroup(String name, PricingGroupEntity pricingGroup) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome da técnica é obrigatório.");
        }
        if (pricingGroup == null) {
            throw new BusinessRuleException("A técnica deve pertencer a um grupo de preço.");
        }
    }

}
