package br.com.senai.sublime_app.pricing.domain;

import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pricing_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PricingGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Fixo após a criação (sem método que o altere): as linhas de preço do grupo
    // já estão na tabela correspondente a ele.
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false)
    private PricingModel pricingModel;

    // Construtor publico para criar um grupo de preço com os dados obrigatórios
    public PricingGroupEntity(String name, PricingModel pricingModel) {
        validateNameAndPricingModel(name, pricingModel);
        this.name = name;
        this.pricingModel = pricingModel;
    }

    private static void validateNameAndPricingModel(String name, PricingModel pricingModel) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Pricing group name is required.");
        }
        if (pricingModel == null) {
            throw new BusinessRuleException("Pricing group pricingModel is required.");
        }
    }

}
