package br.com.senai.sublime_app.pricing.domain;

import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Fixo após a criação (sem método que o altere): o tipo define em quais tabelas
    // de preço o plano tem linhas e se ele pode entrar em contrato.
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType type;

    // Obrigatório em SINGLE_SESSION/PACKAGE, nulo em MONTHLY_FEE (o Grupo é
    // mensalidade, não pacote de sessões). Ver PlanType.hasSessionCount.
    @Column(name = "session_count")
    private Integer sessionCount;

    // Prazo em semanas, só em MONTHLY_FEE (os pacotes calculam o prazo pela
    // frequência, no contrato). Ver PlanType.hasTermWeeks.
    @Column(name = "term_weeks")
    private Integer termWeeks;

    // De-para: o plano equivalente na outra família (pacote ⇄ mensalidade).
    // Preenchido nos dois lados por causa da assimetria do Evolução Familiar
    // (ver domain-model.md, "Equivalência de planos").
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equivalent_plan_id")
    private PlanEntity equivalentPlan;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um plano com os dados obrigatórios
    public PlanEntity(String name, PlanType type, Integer sessionCount, Integer termWeeks) {
        validateName(name);
        validateAmountsForType(type, sessionCount, termWeeks);
        this.name = name;
        this.type = type;
        this.sessionCount = sessionCount;
        this.termWeeks = termWeeks;
        this.active = true;
    }

    // Define (ou troca) o equivalente do de-para. Só este lado é alterado: o outro
    // plano define o seu próprio equivalente (os dois lados podem diferir — Evolução
    // Familiar → Trimestral, mas Trimestral → Evolução).
    public void defineEquivalent(PlanEntity equivalent) {
        validateEquivalent(equivalent);
        this.equivalentPlan = equivalent;
    }

    // De-para: o plano a usar num grupo com este modelo de preço. O próprio, se o tipo
    // dele é precificado assim (os grupos por duração compartilham os planos:
    // Evolução → Evolução); senão, o equivalente na outra família (Evolução no Grupo →
    // Trimestral; Trimestral num grupo por duração → Evolução).
    public PlanEntity planFor(PricingModel pricingModel) {
        if (type.isPricedBy(pricingModel)) {
            return this;
        }
        requireEquivalent();
        return equivalentPlan;
    }

    // Sem equivalente cadastrado não há como resolver o de-para (nunca se "chuta" um plano)
    private void requireEquivalent() {
        if (equivalentPlan == null) {
            throw new BusinessRuleException("O plano " + name + " não tem plano equivalente definido (de-para).");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do plano é obrigatório.");
        }
    }

    // Coerência entre o tipo e os campos de quantidade: cada tipo tem exatamente o
    // campo dele e nenhum do outro. O Avulso é uma sessão só.
    private static void validateAmountsForType(PlanType type, Integer sessionCount, Integer termWeeks) {
        if (type == null) {
            throw new BusinessRuleException("O tipo do plano é obrigatório.");
        }
        if (type.hasSessionCount() && (sessionCount == null || sessionCount < 1)) {
            throw new BusinessRuleException("A quantidade de sessões é obrigatória para este tipo de plano e deve ser de no mínimo 1.");
        }
        if (!type.hasSessionCount() && sessionCount != null) {
            throw new BusinessRuleException("Plano de mensalidade não tem quantidade de sessões.");
        }
        if (type == PlanType.SINGLE_SESSION && sessionCount != 1) {
            throw new BusinessRuleException("O plano avulso tem exatamente 1 sessão.");
        }
        if (type.hasTermWeeks() && (termWeeks == null || termWeeks < 1)) {
            throw new BusinessRuleException("O prazo em semanas é obrigatório para plano de mensalidade e deve ser de no mínimo 1.");
        }
        if (!type.hasTermWeeks() && termWeeks != null) {
            throw new BusinessRuleException("Só plano de mensalidade tem prazo em semanas.");
        }
    }

    // O equivalente liga as duas famílias (pacote ⇄ mensalidade); o Avulso não tem.
    // Equivalente novo precisa estar ativo (o que é novo precisa ser válido — mesma
    // ideia do contrato). Compara ids: relação LAZY pode vir como proxy do Hibernate.
    private void validateEquivalent(PlanEntity equivalent) {
        if (equivalent == null) {
            throw new BusinessRuleException("O plano equivalente é obrigatório.");
        }
        if (equivalent.getId().equals(this.id)) {
            throw new BusinessRuleException("Um plano não pode ser equivalente a si mesmo.");
        }
        if (!equivalent.isActive()) {
            throw new BusinessRuleException("O plano equivalente está inativo.");
        }
        if (!this.type.isEquivalentFamilyOf(equivalent.getType())) {
            throw new BusinessRuleException("O equivalente precisa ser da outra família de planos (pacote ⇄ mensalidade). O avulso não tem equivalente.");
        }
    }

}
