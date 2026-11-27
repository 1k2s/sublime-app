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

// Contrato de plano de mensalidade com item por duração (ver "Contract e ContractItem" no
// domain-model): a linha do item vem do pacote equivalente (de-para), a vigência é o maior prazo
// entre o do plano e o dos itens, e o item do Grupo continua obrigatório. Teste de unidade puro:
// sem Spring e sem banco. O ReflectionTestUtils só preenche os ids, que em produção vêm do banco.
class ContractEntityMonthlyFeeTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    private PlanEntity quarterly;
    private PlanEntity evolution;
    private PlanEntity essential;
    private TechniqueEntity groupPilates;
    private TechniqueEntity rpg;
    private SessionFrequencyPriceEntity groupPrice;
    private SessionDurationPriceEntity rpgEvolutionPrice;
    private SessionDurationPriceEntity rpgEssentialPrice;
    private PatientEntity holder;

    @BeforeEach
    void setUp() {
        quarterly = withId(new PlanEntity("Trimestral", PlanType.MONTHLY_FEE, null, 13), 1L);
        evolution = withId(new PlanEntity("Evolução", PlanType.PACKAGE, 12, null), 2L);
        essential = withId(new PlanEntity("Essencial", PlanType.PACKAGE, 4, null), 3L);
        quarterly.defineEquivalent(evolution);

        PricingGroupEntity groupGroup = withId(new PricingGroupEntity("Em Grupo", PricingModel.FREQUENCY_BASED), 10L);
        PricingGroupEntity standardGroup = withId(
                new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED), 11L);
        groupPilates = withId(new TechniqueEntity("Pilates em Grupo", groupGroup), 20L);
        rpg = withId(new TechniqueEntity("RPG", standardGroup), 21L);
        groupPrice = withId(new SessionFrequencyPriceEntity(groupGroup, quarterly, 2, money("65.81"), START), 30L);
        rpgEvolutionPrice = withId(
                new SessionDurationPriceEntity(standardGroup, evolution, 60, money("167.00"), START), 31L);
        rpgEssentialPrice = withId(
                new SessionDurationPriceEntity(standardGroup, essential, 60, money("180.00"), START), 32L);
        holder = withId(new PatientEntity("Carla", "33333333333", LocalDate.of(1990, 1, 1), null, null, null, null),
                100L);
    }

    @Test
    @DisplayName("Aceita item por duração com a linha do pacote equivalente; o prazo do plano prevalece se for maior")
    void acceptsDurationItemFromEquivalentPackage() {
        // Trimestral = 13 semanas; Evolução 1x por semana = 12
        ContractEntity contract = create(List.of(groupDraft(), rpgDraft(rpgEvolutionPrice, SessionFrequency.ONCE_A_WEEK)));

        assertThat(contract.getItems()).hasSize(2);
        assertThat(contract.getEndDate()).isEqualTo(ContractEntity.endDateFor(START, 13));
    }

    @Test
    @DisplayName("Item por duração mais longo que o plano estende a vigência")
    void durationItemExtendsTheTerm() {
        // Evolução quinzenal = 24 semanas, mais que as 13 do Trimestral
        ContractEntity contract = create(List.of(groupDraft(), rpgDraft(rpgEvolutionPrice, SessionFrequency.BIWEEKLY)));

        assertThat(contract.getEndDate()).isEqualTo(ContractEntity.endDateFor(START, 24));
        assertThat(contract.getMaxInstallments()).isEqualTo(6);
    }

    @Test
    @DisplayName("Recusa a linha de outro pacote que não o equivalente")
    void rejectsLineFromAnotherPackage() {
        List<ContractItemDraft> drafts = List.of(groupDraft(), rpgDraft(rpgEssentialPrice, SessionFrequency.ONCE_A_WEEK));

        assertThatThrownBy(() -> create(drafts))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O preço do item precisa ser do plano Evolução (equivalente do Trimestral).");
    }

    @Test
    @DisplayName("Recusa o contrato de mensalidade sem o item do Pilates em Grupo")
    void rejectsMonthlyFeeWithoutGroupItem() {
        List<ContractItemDraft> drafts = List.of(rpgDraft(rpgEvolutionPrice, SessionFrequency.ONCE_A_WEEK));

        assertThatThrownBy(() -> create(drafts))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Contrato com plano de mensalidade precisa do item do Pilates em Grupo. Para ficar só com itens por duração, faça um contrato novo com um plano de pacote.");
    }

    @Test
    @DisplayName("Recusa item por duração quando o plano de mensalidade não tem equivalente")
    void rejectsDurationItemWithoutEquivalent() {
        PlanEntity monthly = withId(new PlanEntity("Mensal", PlanType.MONTHLY_FEE, null, 4), 4L);
        PricingGroupEntity groupGroup = groupPilates.getPricingGroup();
        SessionFrequencyPriceEntity monthlyGroupPrice = withId(
                new SessionFrequencyPriceEntity(groupGroup, monthly, 2, money("73.63"), START), 33L);
        List<ContractItemDraft> drafts = List.of(
                ContractItemDraft.withFrequencyPrice(groupPilates, monthlyGroupPrice),
                rpgDraft(rpgEvolutionPrice, SessionFrequency.ONCE_A_WEEK));

        assertThatThrownBy(() -> ContractEntity.create(holder, null, monthly, START, PaymentMethod.PIX, 1, drafts, START))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O plano Mensal não tem plano equivalente definido (de-para).");
    }

    @Test
    @DisplayName("Aditivo: inclui item por duração num contrato só com o Grupo, contando da data do aditivo")
    void amendmentAddsDurationItem() {
        ContractEntity contract = create(List.of(groupDraft()));
        LocalDate amendmentDate = START.plusWeeks(8);

        ContractEntity amended = contract.amend(null, PaymentMethod.PIX, 3, null,
                List.of(groupDraft(), rpgDraft(rpgEvolutionPrice, SessionFrequency.ONCE_A_WEEK)), amendmentDate);

        // O fim atual (13 semanas) passa a ser o do item novo: 12 semanas a partir do aditivo
        assertThat(amended.getEndDate()).isEqualTo(ContractEntity.endDateFor(amendmentDate, 12));
        assertThat(amended.getItems()).hasSize(2);
    }

    private ContractEntity create(List<ContractItemDraft> drafts) {
        return ContractEntity.create(holder, null, quarterly, START, PaymentMethod.PIX, 3, drafts, START);
    }

    private ContractItemDraft groupDraft() {
        return ContractItemDraft.withFrequencyPrice(groupPilates, groupPrice);
    }

    private ContractItemDraft rpgDraft(SessionDurationPriceEntity price, SessionFrequency frequency) {
        return ContractItemDraft.withDurationPrice(rpg, price, frequency);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
