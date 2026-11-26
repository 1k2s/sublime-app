package br.com.senai.sublime_app.contract.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// Valor do item do Pilates em Grupo: as aulas que cabem na vigência do contrato (semanas do
// contrato × vezes por semana), ao preço da aula do plano escolhido (ver o ADR 0041 e "Valor e
// parcelas" em Contract, no domain-model). Teste de unidade puro: sem Spring e sem banco. O
// ReflectionTestUtils só preenche os ids, que em produção vêm do banco.
class ContractGroupItemValueTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    private PlanEntity quarterly;
    private PlanEntity evolution;
    private TechniqueEntity groupPilates;
    private TechniqueEntity rpg;
    private SessionFrequencyPriceEntity quarterlyTwiceAWeek;
    private SessionDurationPriceEntity rpgEvolutionPrice;
    private PatientEntity holder;

    @BeforeEach
    void setUp() {
        quarterly = withId(new PlanEntity("Trimestral", PlanType.MONTHLY_FEE, null, 13), 1L);
        evolution = withId(new PlanEntity("Evolução", PlanType.PACKAGE, 12, null), 2L);
        quarterly.defineEquivalent(evolution);
        evolution.defineEquivalent(quarterly);

        PricingGroupEntity groupGroup = withId(new PricingGroupEntity("Em Grupo", PricingModel.FREQUENCY_BASED), 10L);
        PricingGroupEntity standardGroup = withId(
                new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED), 11L);
        groupPilates = withId(new TechniqueEntity("Pilates em Grupo", groupGroup), 20L);
        rpg = withId(new TechniqueEntity("RPG", standardGroup), 21L);
        quarterlyTwiceAWeek = withId(
                new SessionFrequencyPriceEntity(groupGroup, quarterly, 2, money("65.81"), START), 30L);
        rpgEvolutionPrice = withId(
                new SessionDurationPriceEntity(standardGroup, evolution, 60, money("167.00"), START), 31L);
        holder = withId(new PatientEntity("Ana", "11111111111", LocalDate.of(1990, 1, 1), null, null, null, null),
                100L);
    }

    @Test
    @DisplayName("A1. Contrato misto: o Grupo cobra só as aulas que cabem na vigência do contrato")
    void mixedContractChargesClassesWithinTheTerm() {
        // Evolução 2x por semana = 12 sessões em 6 semanas; o Grupo Trimestral 2x acompanha as 6
        ContractEntity contract = ContractEntity.create(holder, null, evolution, START, PaymentMethod.PIX, 1,
                List.of(rpgDraft(SessionFrequency.TWICE_A_WEEK), groupDraft()), START);

        ContractItemEntity groupItem = groupItemOf(contract);
        assertThat(contract.getDurationInWeeks()).isEqualTo(6);
        assertThat(groupItem.getValue().sessionCount()).isEqualTo(12);
        assertThat(groupItem.getValue().totalValue().amount()).isEqualByComparingTo("789.72");
    }

    @Test
    @DisplayName("A2. Contrato só de mensalidade: as aulas do prazo do plano, como antes")
    void monthlyFeeOnlyKeepsThePlanTerm() {
        ContractEntity contract = ContractEntity.create(holder, null, quarterly, START, PaymentMethod.PIX, 3,
                List.of(groupDraft()), START);

        assertThat(groupItemOf(contract).getValue().sessionCount()).isEqualTo(26);
        assertThat(contract.getTotalValue().amount()).isEqualByComparingTo("1711.06");
    }

    @Test
    @DisplayName("A3. Mensalidade com item por duração mais longo: o Grupo segue a vigência maior")
    void monthlyFeeWithLongerDurationItemFollowsTheTerm() {
        // Evolução quinzenal = 24 semanas, mais que as 13 do Trimestral
        ContractEntity contract = ContractEntity.create(holder, null, quarterly, START, PaymentMethod.PIX, 6,
                List.of(groupDraft(), rpgDraft(SessionFrequency.BIWEEKLY)), START);

        ContractItemEntity groupItem = groupItemOf(contract);
        assertThat(groupItem.getValue().sessionCount()).isEqualTo(48);
        assertThat(groupItem.getValue().totalValue().amount()).isEqualByComparingTo("3158.88");
    }

    @Test
    @DisplayName("A4. Aditivo que prorroga: o Grupo ganha as aulas da nova vigência, ao preço travado")
    void amendmentExtensionAddsGroupClasses() {
        // Evolução 1x por semana = 12 semanas; prorrogado em 4 semanas = 16
        ContractEntity contract = ContractEntity.create(holder, null, evolution, START, PaymentMethod.PIX, 3,
                List.of(rpgDraft(SessionFrequency.ONCE_A_WEEK), groupDraft()), START);
        LocalDate extendedEnd = contract.getEndDate().plusWeeks(4);

        ContractEntity amended = contract.amend(null, PaymentMethod.PIX, 3, extendedEnd,
                List.of(rpgDraft(SessionFrequency.ONCE_A_WEEK), groupDraft()), START.plusWeeks(2));

        assertThat(groupItemOf(contract).getValue().sessionCount()).isEqualTo(24);
        assertThat(amended.getDurationInWeeks()).isEqualTo(16);
        assertThat(groupItemOf(amended).getValue().sessionCount()).isEqualTo(32);
        assertThat(groupItemOf(amended).getValue().sessionValue().amount()).isEqualByComparingTo("65.81");
    }

    @Test
    @DisplayName("A5. Aditivo sem alteração num contrato misto continua recusado")
    void amendmentWithoutChangesIsStillRejected() {
        ContractEntity contract = ContractEntity.create(holder, null, evolution, START, PaymentMethod.PIX, 3,
                List.of(rpgDraft(SessionFrequency.ONCE_A_WEEK), groupDraft()), START);
        List<ContractItemDraft> sameItems = List.of(rpgDraft(SessionFrequency.ONCE_A_WEEK), groupDraft());

        assertThatThrownBy(() -> contract.amend(null, PaymentMethod.PIX, 3, null, sameItems, START.plusWeeks(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Nenhuma alteração no contrato.");
    }

    private ContractItemEntity groupItemOf(ContractEntity contract) {
        return contract.findItemOf(groupPilates.getPricingGroup()).orElseThrow();
    }

    private ContractItemDraft groupDraft() {
        return ContractItemDraft.withFrequencyPrice(groupPilates, quarterlyTwiceAWeek);
    }

    private ContractItemDraft rpgDraft(SessionFrequency frequency) {
        return ContractItemDraft.withDurationPrice(rpg, rpgEvolutionPrice, frequency);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
