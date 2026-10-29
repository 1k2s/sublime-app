package br.com.senai.sublime_app.contract.domain;

import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

// Entidade interna do agregado Contract: um item por grupo de preço contratado.
// Não tem repositório nem service próprios — só o ContractEntity cria itens (o
// construtor de negócio não é público). Ver domain-model.md, "ContractItem".
@Entity
@Table(name = "contract_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContractItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Dono do item (raiz do agregado)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    // Exclusive arc: linha de preço travada (por duração OU por frequência)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_duration_price_id")
    private SessionDurationPriceEntity sessionDurationPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_frequency_price_id")
    private SessionFrequencyPriceEntity sessionFrequencyPrice;

    // Só informativa (aparece no contrato impresso): não restringe o atendimento
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technique_id", nullable = false)
    private TechniqueEntity technique;

    // Ritmo negociado; só em item de grupo por duração (nulo no item do Grupo)
    // varchar, não o enum nativo do MySQL: valor novo no enum não exige migration (ADR 0021)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "frequency", length = 30)
    private SessionFrequency frequency;

    // Sem "public": só o ContractEntity (mesmo pacote) cria itens. "previous" é o
    // item do MESMO grupo na versão anterior — null na criação ou se o grupo é novo
    // no aditivo. O que for igual a ele é aceito como está; o que é novo precisa ser válido.
    ContractItemEntity(ContractEntity contract, ContractItemDraft draft, ContractItemEntity previous) {
        validatePrice(draft, previous);
        validateTechnique(draft, previous);
        validateFrequency(draft);
        this.contract = contract;
        this.sessionDurationPrice = draft.durationPrice();
        this.sessionFrequencyPrice = draft.frequencyPrice();
        this.technique = draft.technique();
        this.frequency = draft.frequency();
    }

    // O grupo vem da linha de preço travada (derivável — não é coluna)
    public PricingGroupEntity getPricingGroup() {
        return sessionDurationPrice != null ? sessionDurationPrice.getPricingGroup() : sessionFrequencyPrice.getPricingGroup();
    }

    public boolean isDurationBased() {
        return sessionDurationPrice != null;
    }

    // Semanas para completar as sessões do plano no ritmo do item. Só faz sentido
    // em item por duração — usado no cálculo do fim do contrato.
    public int weeksToComplete() {
        return frequency.weeksToComplete(sessionDurationPrice.getPlan().getSessionCount());
    }

    // Linha vigente, ou a mesma já travada no item anterior (o contrato mantém o
    // valor combinado mesmo após um reajuste; trocar de linha exige linha vigente).
    // Compara ids: relação LAZY pode vir como proxy do Hibernate.
    private static void validatePrice(ContractItemDraft draft, ContractItemEntity previous) {
        boolean current = draft.isDurationBased()
                ? draft.durationPrice().isCurrent()
                : draft.frequencyPrice().isCurrent();
        if (!current && !isSamePriceAsPrevious(draft, previous)) {
            throw new BusinessRuleException("O preço selecionado não está mais vigente. Use o preço atual.");
        }
    }

    private static boolean isSamePriceAsPrevious(ContractItemDraft draft, ContractItemEntity previous) {
        if (previous == null) {
            return false;
        }
        if (draft.isDurationBased()) {
            return previous.sessionDurationPrice != null
                    && previous.sessionDurationPrice.getId().equals(draft.durationPrice().getId());
        }
        return previous.sessionFrequencyPrice != null
                && previous.sessionFrequencyPrice.getId().equals(draft.frequencyPrice().getId());
    }

    // A técnica precisa ser do grupo da linha (o contrato impresso não pode mostrar
    // "RPG" com o preço da Quiropraxia). Técnica nova precisa estar ativa; a mesma do
    // item anterior é aceita mesmo inativa.
    private static void validateTechnique(ContractItemDraft draft, ContractItemEntity previous) {
        if (draft.technique() == null) {
            throw new BusinessRuleException("A técnica do item é obrigatória.");
        }
        if (!draft.technique().getPricingGroup().getId().equals(draft.pricingGroup().getId())) {
            throw new BusinessRuleException("A técnica não pertence ao grupo de preço do preço selecionado.");
        }
        boolean sameAsPrevious = previous != null && previous.technique.getId().equals(draft.technique().getId());
        if (!draft.technique().isActive() && !sameAsPrevious) {
            throw new BusinessRuleException("A técnica está inativa.");
        }
    }

    // Item por duração: frequência obrigatória e dentro do teto de 52 semanas (pacote
    // longo em ritmo lento não pode ser esticado). Item do Grupo: frequência vazia —
    // ela é o eixo do preço e já está na linha travada.
    private static void validateFrequency(ContractItemDraft draft) {
        if (!draft.isDurationBased()) {
            if (draft.frequency() != null) {
                throw new BusinessRuleException("A frequência não deve ser informada no item do Pilates em Grupo: ela vem da linha de preço.");
            }
            return;
        }
        if (draft.frequency() == null) {
            throw new BusinessRuleException("A frequência é obrigatória em item de grupo por duração.");
        }
        validateFitsMaxTerm(draft.frequency(), draft.durationPrice().getPlan());
    }

    // Pacote longo em ritmo lento não pode ser esticado além do teto. Estático e do
    // pacote: o ContractEntity reaproveita no cálculo do prazo (inclusive na prévia).
    static void validateFitsMaxTerm(SessionFrequency frequency, PlanEntity plan) {
        if (!frequency.fitsMaxTerm(plan.getSessionCount())) {
            throw new BusinessRuleException("O plano " + plan.getName()
                    + " nesta frequência passa do prazo máximo de " + SessionFrequency.MAX_TERM_WEEKS + " semanas.");
        }
    }
}
