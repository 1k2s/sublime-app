package br.com.senai.sublime_app.contract.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// O vencimento e o valor parcelado do contrato (ver "Valor e parcelas" e "Versionamento
// (aditivos)" em Contract, no domain-model): o 1º vencimento, o cronograma, o valor parcelado
// (o total − o pago no aditivo), a distribuição do pago, a inativação com data e o que conta
// como alteração nas parcelas. Teste de unidade puro: sem Spring e sem banco.
//
// O contrato base: Evolução, RPG 60 min semanal = 12 × R$ 167,00 = R$ 2.004,00, 12 semanas, de
// 02/11/2026 a 24/01/2027, em 3 parcelas de R$ 668,00 (02/11, 02/12, 02/01).
class ContractEntityDueDateTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);

    private PlanEntity evolution;
    private TechniqueEntity rpg;
    private TechniqueEntity chiropractic;
    private SessionDurationPriceEntity rpgPrice;
    private SessionDurationPriceEntity chiropracticPrice;
    private PatientEntity holder;

    @BeforeEach
    void setUp() {
        evolution = withId(new PlanEntity("Evolução", PlanType.PACKAGE, 12, null), 1L);
        PricingGroupEntity standardGroup = withId(
                new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED), 10L);
        PricingGroupEntity specializedGroup = withId(
                new PricingGroupEntity("Individual Especializado", PricingModel.DURATION_BASED), 11L);
        rpg = withId(new TechniqueEntity("RPG", standardGroup), 20L);
        chiropractic = withId(new TechniqueEntity("Quiropraxia", specializedGroup), 21L);
        rpgPrice = withId(new SessionDurationPriceEntity(standardGroup, evolution, 60, money("167.00"), START), 30L);
        chiropracticPrice = withId(
                new SessionDurationPriceEntity(specializedGroup, evolution, 30, money("132.00"), START), 31L);
        holder = withId(new PatientEntity("Ana", "11111111111", LocalDate.of(1990, 1, 1), null, null, null, null),
                100L);
    }

    @Nested
    @DisplayName("1º vencimento e cronograma no cadastro")
    class Creation {

        @Test
        @DisplayName("Sem o 1º vencimento, é o início do contrato")
        void firstDueDateDefaultsToStart() {
            assertThat(create(null).getFirstDueDate()).isEqualTo(START);
        }

        @Test
        @DisplayName("Recusa o 1º vencimento antes do início")
        void rejectsFirstDueDateBeforeStart() {
            assertThatThrownBy(() -> create(START.minusDays(1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O 1º vencimento deve ser entre o início do contrato e um mês depois.");
        }

        @Test
        @DisplayName("Recusa o 1º vencimento mais de um mês depois do início; aceita até um mês")
        void rejectsFirstDueDateAfterOneMonth() {
            assertThatThrownBy(() -> create(START.plusMonths(1).plusDays(1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O 1º vencimento deve ser entre o início do contrato e um mês depois.");
            assertThat(create(START.plusMonths(1)).getFirstDueDate()).isEqualTo(START.plusMonths(1));
        }

        @Test
        @DisplayName("O valor parcelado é o total, em 3 parcelas mensais no dia do 1º vencimento")
        void scheduleFromTheFirstDueDate() {
            ContractEntity contract = create(null);

            assertThat(contract.getInstallmentBase().amount()).isEqualByComparingTo("2004.00");
            assertThat(contract.getInstallmentSchedule())
                    .extracting(Installments.Scheduled::dueDate)
                    .containsExactly(START, LocalDate.of(2026, 12, 2), LocalDate.of(2027, 1, 2));
            assertThat(contract.getInstallmentSchedule())
                    .extracting(installment -> installment.value().amount())
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(new BigDecimal("668.00"), new BigDecimal("668.00"), new BigDecimal("668.00"));
            assertThat(contract.getDeactivatedOn()).isNull();
        }

        @Test
        @DisplayName("Vencimento no dia 31: o mês curto vence no último dia, e o dia volta no seguinte")
        void dayThirtyOneKeepsTheDay() {
            LocalDate start = LocalDate.of(2027, 1, 31);
            ContractEntity contract = ContractEntity.create(holder, null, evolution, start, PaymentMethod.PIX, 3,
                    List.of(rpgDraft()), null, start);

            assertThat(contract.getInstallmentSchedule())
                    .extracting(Installments.Scheduled::dueDate)
                    .containsExactly(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31));
        }

        @Test
        @DisplayName("Inativar grava a data da inativação")
        void deactivationHasADate() {
            ContractEntity contract = create(null);
            LocalDate today = LocalDate.of(2026, 12, 15);

            contract.deactivate(today);

            assertThat(contract.isActive()).isFalse();
            assertThat(contract.getDeactivatedOn()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("Valor parcelado e vencimento no aditivo")
    class Amendment {

        private final LocalDate amendmentDate = LocalDate.of(2026, 12, 1);

        @Test
        @DisplayName("Com R$ 668,00 pagos, inclui Quiropraxia: as parcelas dividem o que falta")
        void installmentsSplitWhatIsLeft() {
            ContractEntity contract = create(null);

            // Quiropraxia 30 min semanal = 12 × R$ 132,00 = R$ 1.584,00; total R$ 3.588,00
            ContractEntity amended = contract.amend(null, PaymentMethod.PIX, 3, null,
                    List.of(rpgDraft(), chiropracticDraft()), null, money("668.00"), amendmentDate);

            assertThat(amended.getTotalValue().amount()).isEqualByComparingTo("3588.00");
            assertThat(amended.getInstallmentBase().amount()).isEqualByComparingTo("2920.00");
            // 2.920,00 em 3: 1ª de R$ 973,34 + 2 × R$ 973,33; a 1ª vence no sugerido, 02/12
            assertThat(amended.getInstallmentValues().firstValue().amount()).isEqualByComparingTo("973.34");
            assertThat(amended.getFirstDueDate()).isEqualTo(LocalDate.of(2026, 12, 2));
        }

        @Test
        @DisplayName("Recusa o 1º vencimento antes da data do aditivo")
        void rejectsFirstDueDateBeforeAmendment() {
            ContractEntity contract = create(null);

            assertThatThrownBy(() -> contract.amend(null, PaymentMethod.CASH, 2, null, List.of(rpgDraft()),
                    amendmentDate.minusDays(1), nothingPaid(), amendmentDate))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("No aditivo, o 1º vencimento deve ser a partir da data do aditivo.");
            assertThat(contract.isActive()).isTrue();
        }

        @Test
        @DisplayName("Contrato de 12 meses com aditivo no 10º mês: no máximo 2 parcelas")
        void maxInstallmentsAreTheRemainingMonths() {
            PlanEntity vitality = withId(new PlanEntity("Vitalidade", PlanType.PACKAGE, 52, null), 2L);
            SessionDurationPriceEntity vitalityPrice = withId(new SessionDurationPriceEntity(
                    rpgPrice.getPricingGroup(), vitality, 60, money("134.00"), START), 32L);
            ContractEntity contract = ContractEntity.create(holder, null, vitality, START, PaymentMethod.PIX, 12,
                    List.of(ContractItemDraft.withDurationPrice(rpg, vitalityPrice, SessionFrequency.ONCE_A_WEEK)),
                    null, START);

            assertThatThrownBy(() -> contract.amend(null, PaymentMethod.CASH, 3, null,
                    List.of(ContractItemDraft.withDurationPrice(rpg, vitalityPrice, SessionFrequency.ONCE_A_WEEK)),
                    null, nothingPaid(), LocalDate.of(2027, 9, 1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O número de parcelas deve ser de 1 a 2 (meses do contrato).");
        }

        @Test
        @DisplayName("O pago cobre o total: valor parcelado zero, em 1 parcela de R$ 0,00")
        void paidCoversTheTotal() {
            ContractEntity contract = ContractEntity.create(holder, null, evolution, START, PaymentMethod.PIX, 3,
                    List.of(rpgDraft(), chiropracticDraft()), null, START);

            // Pagou R$ 3.000,00 e o aditivo remove a Quiropraxia: o total cai para R$ 2.004,00
            ContractEntity amended = contract.amend(null, PaymentMethod.PIX, 2, null, List.of(rpgDraft()), null,
                    money("3000.00"), amendmentDate);

            assertThat(amended.getInstallmentBase().amount()).isEqualByComparingTo("0.00");
            assertThat(amended.getInstallments()).isEqualTo(1);
            assertThat(amended.getInstallmentValues().firstValue().amount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("A versão substituída fica inativa com a data do aditivo")
        void supersededVersionGetsTheAmendmentDate() {
            ContractEntity contract = create(null);

            ContractEntity amended = contract.amend(null, PaymentMethod.CASH, 2, null, List.of(rpgDraft()), null,
                    nothingPaid(), amendmentDate);

            assertThat(contract.getDeactivatedOn()).isEqualTo(amended.getCreatedAt());
        }

        @Test
        @DisplayName("Sugestão com a parcela deste mês paga: o mesmo dia no mês seguinte")
        void suggestionWhenThisMonthIsPaid() {
            assertThat(create(null).suggestedFirstDueDate(money("1336.00"), amendmentDate))
                    .isEqualTo(LocalDate.of(2027, 1, 2));
        }

        @Test
        @DisplayName("Sugestão com a parcela deste mês em aberto e o dia por vir: o dia deste mês")
        void suggestionWhenThisMonthIsOpen() {
            assertThat(create(null).suggestedFirstDueDate(money("668.00"), amendmentDate))
                    .isEqualTo(LocalDate.of(2026, 12, 2));
        }

        @Test
        @DisplayName("Sugestão com a parcela em aberto e o dia já passado: o dia do mês seguinte")
        void suggestionWhenTheDayHasPassed() {
            assertThat(create(null).suggestedFirstDueDate(money("668.00"), LocalDate.of(2026, 12, 10)))
                    .isEqualTo(LocalDate.of(2027, 1, 2));
        }

        @Test
        @DisplayName("Manter o dia e as 2 parcelas que faltam não é alteração")
        void keepingDayAndRemainingInstallmentsIsNoChange() {
            ContractEntity contract = create(null);

            assertThatThrownBy(() -> contract.amend(null, PaymentMethod.PIX, 2, null, List.of(rpgDraft()),
                    LocalDate.of(2026, 12, 2), money("668.00"), amendmentDate))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nenhuma alteração no contrato.");
        }

        @Test
        @DisplayName("Mudar o dia do vencimento, ou para 1 parcela, é alteração")
        void changingDayOrCountIsAChange() {
            ContractEntity otherDay = create(null).amend(null, PaymentMethod.PIX, 2, null, List.of(rpgDraft()),
                    LocalDate.of(2026, 12, 10), money("668.00"), amendmentDate);
            ContractEntity oneInstallment = create(null).amend(null, PaymentMethod.PIX, 1, null, List.of(rpgDraft()),
                    null, money("668.00"), amendmentDate);

            assertThat(otherDay.getFirstDueDate()).isEqualTo(LocalDate.of(2026, 12, 10));
            assertThat(oneInstallment.getInstallments()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Distribuição do pago")
    class PaidDistribution {

        @Test
        @DisplayName("O pago vai pela ordem de vencimento: R$ 1.000,00 pagam a 1ª e R$ 332,00 da 2ª")
        void distributesByDueOrder() {
            List<Money> paid = Installments.split(money("2004.00"), 3).distribute(money("1000.00"));

            assertThat(paid).extracting(Money::amount)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(new BigDecimal("668.00"), new BigDecimal("332.00"), new BigDecimal("0.00"));
        }

        @Test
        @DisplayName("O pago das parcelas fica entre 0 e o valor parcelado")
        void paidTowardInstallmentsIsClamped() {
            ContractEntity amended = create(null).amend(null, PaymentMethod.PIX, 3, null,
                    List.of(rpgDraft(), chiropracticDraft()), null, money("668.00"), LocalDate.of(2026, 12, 1));

            // Pago antes do aditivo: R$ 668,00 (o total − o valor parcelado)
            assertThat(amended.paidTowardInstallments(money("500.00")).amount()).isEqualByComparingTo("0.00");
            assertThat(amended.paidTowardInstallments(money("1668.00")).amount()).isEqualByComparingTo("1000.00");
            assertThat(amended.paidTowardInstallments(money("9000.00")).amount()).isEqualByComparingTo("2920.00");
        }

        @Test
        @DisplayName("O máximo num período que termina antes do início é 1")
        void maxForPeriodIsAtLeastOne() {
            assertThat(Installments.maxForPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 1, 1))).isEqualTo(1);
        }
    }

    private ContractEntity create(LocalDate firstDueDate) {
        return ContractEntity.create(holder, null, evolution, START, PaymentMethod.PIX, 3, List.of(rpgDraft()),
                firstDueDate, START);
    }

    private ContractItemDraft rpgDraft() {
        return ContractItemDraft.withDurationPrice(rpg, rpgPrice, SessionFrequency.ONCE_A_WEEK);
    }

    private ContractItemDraft chiropracticDraft() {
        return ContractItemDraft.withDurationPrice(chiropractic, chiropracticPrice, SessionFrequency.ONCE_A_WEEK);
    }

    private static Money nothingPaid() {
        return Money.of(BigDecimal.ZERO);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
