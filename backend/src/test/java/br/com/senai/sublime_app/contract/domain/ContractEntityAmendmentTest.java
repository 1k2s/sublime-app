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
import br.com.senai.sublime_app.shared.exception.ConflictException;

// Regras do aditivo no ContractEntity (ver "Versionamento (aditivos)" e "Valor e parcelas" em
// Contract, no domain-model). Teste de unidade puro: sem Spring e sem banco. O
// ReflectionTestUtils só preenche os ids, que em produção vêm do banco (as regras comparam ids).
//
// O contrato base: 1º vencimento = o início (02/11), 3 parcelas (02/11, 02/12, 02/01), fim em
// 24/01. No aditivo de 01/12, sem nada pago: o 1º vencimento sugerido é 02/12; faltam 2 parcelas
// (02/12 e 02/01); o máximo são os meses de 02/12 a 24/01 = 8 semanas = 2.
class ContractEntityAmendmentTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    // 24 sessões a 2x por semana = 12 semanas: fim em 24/01/2027
    private static final LocalDate CURRENT_END = LocalDate.of(2027, 1, 24);
    private static final LocalDate AMENDMENT_DATE = LocalDate.of(2026, 12, 1);
    private static final Money NOTHING_PAID = Money.of(BigDecimal.ZERO);

    private PlanEntity plan;
    private TechniqueEntity rpg;
    private TechniqueEntity otherRpgTechnique;
    private TechniqueEntity chiropractic;
    private SessionDurationPriceEntity rpgPrice;
    private SessionDurationPriceEntity rpgShorterPrice;
    private SessionDurationPriceEntity chiropracticPrice;
    private PatientEntity holder;
    private PatientEntity beneficiary;

    @BeforeEach
    void setUp() {
        plan = withId(new PlanEntity("Trimestral", PlanType.PACKAGE, 24, null), 1L);
        PricingGroupEntity rpgGroup = withId(new PricingGroupEntity("RPG", PricingModel.DURATION_BASED), 10L);
        PricingGroupEntity chiropracticGroup = withId(
                new PricingGroupEntity("Quiropraxia", PricingModel.DURATION_BASED), 11L);
        rpg = withId(new TechniqueEntity("RPG", rpgGroup), 20L);
        otherRpgTechnique = withId(new TechniqueEntity("Reeducação postural", rpgGroup), 21L);
        chiropractic = withId(new TechniqueEntity("Quiropraxia", chiropracticGroup), 22L);
        rpgPrice = withId(new SessionDurationPriceEntity(rpgGroup, plan, 50, money("100.00"), START), 30L);
        rpgShorterPrice = withId(new SessionDurationPriceEntity(rpgGroup, plan, 30, money("80.00"), START), 31L);
        chiropracticPrice = withId(
                new SessionDurationPriceEntity(chiropracticGroup, plan, 50, money("120.00"), START), 32L);
        holder = withId(patient("Ana", "11111111111"), 100L);
        beneficiary = withId(patient("Bia", "22222222222"), 101L);
    }

    @Nested
    @DisplayName("Parcelas no aditivo")
    class InstallmentsInAmendment {

        @Test
        @DisplayName("Recusa parcelas acima dos meses que faltam até o fim (aqui, 2)")
        void rejectsInstallmentsAboveRemainingMonths() {
            ContractEntity contract = newContract();

            assertThatThrownBy(() -> amend(contract, null, PaymentMethod.CASH, 3, null, rpgItem(), AMENDMENT_DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O número de parcelas deve ser de 1 a 2 (meses do contrato).");
            assertThat(contract.isActive()).isTrue();
        }

        @Test
        @DisplayName("Prorrogação aumenta o máximo: de 02/12 ao novo fim (30/04), 5 parcelas")
        void extensionRaisesMaxInstallments() {
            ContractEntity contract = newContract();
            LocalDate end26Weeks = ContractEntity.endDateFor(START, 26);

            ContractEntity amended = amend(contract, null, PaymentMethod.PIX, 5, end26Weeks, rpgItem(), AMENDMENT_DATE);

            assertThat(amended.getInstallments()).isEqualTo(5);
            assertThatThrownBy(() -> amend(newContract(), null, PaymentMethod.PIX, 6, end26Weeks, rpgItem(),
                    AMENDMENT_DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O número de parcelas deve ser de 1 a 5 (meses do contrato).");
        }

        @Test
        @DisplayName("Item incluído, sem nada pago: as parcelas dividem o novo total")
        void installmentsSplitTheNewTotal() {
            ContractEntity contract = newContract();
            List<ContractItemDraft> items = List.of(rpgDraft(), chiropracticDraft(SessionFrequency.TWICE_A_WEEK));

            // O item incluído conta de 01/12: fim em 22/02, de 02/12 ao fim cabem 3 parcelas
            ContractEntity amended = amend(contract, null, PaymentMethod.PIX, 3, null, items, AMENDMENT_DATE);

            // 24 × 100,00 + 24 × 120,00 = 5.280,00, em 3 parcelas de 1.760,00
            assertThat(amended.getTotalValue().amount()).isEqualByComparingTo("5280.00");
            assertThat(amended.getInstallmentBase().amount()).isEqualByComparingTo("5280.00");
            assertThat(amended.getInstallmentValues().count()).isEqualTo(3);
            assertThat(amended.getInstallmentValues().value().amount()).isEqualByComparingTo("1760.00");
        }
    }

    @Nested
    @DisplayName("Aditivo sem alteração")
    class NoChanges {

        @Test
        @DisplayName("Recusa o aditivo igual à versão atual (as 2 parcelas que faltam, no mesmo dia) e a mantém ativa")
        void rejectsIdenticalAmendment() {
            ContractEntity contract = newContract();

            assertThatThrownBy(() -> amend(contract, null, PaymentMethod.PIX, 2, null, rpgItem(), AMENDMENT_DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nenhuma alteração no contrato.");
            assertThat(contract.isActive()).isTrue();
        }

        @Test
        @DisplayName("Só a prorrogação conta como alteração")
        void acceptsOnlyExtension() {
            assertAccepted(amend(newContract(), null, PaymentMethod.PIX, 2, CURRENT_END.plusWeeks(1), rpgItem(),
                    AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só as parcelas contam como alteração (1, das 2 que faltam)")
        void acceptsOnlyInstallments() {
            assertAccepted(amend(newContract(), null, PaymentMethod.PIX, 1, null, rpgItem(), AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só a forma de pagamento conta como alteração")
        void acceptsOnlyPaymentMethod() {
            assertAccepted(amend(newContract(), null, PaymentMethod.CASH, 2, null, rpgItem(), AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só o beneficiário conta como alteração")
        void acceptsOnlyBeneficiary() {
            assertAccepted(amend(newContract(), beneficiary, PaymentMethod.PIX, 2, null, rpgItem(), AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só a frequência de um item conta como alteração")
        void acceptsOnlyFrequency() {
            List<ContractItemDraft> items = List.of(
                    ContractItemDraft.withDurationPrice(rpg, rpgPrice, SessionFrequency.THREE_TIMES_A_WEEK));

            assertAccepted(amend(newContract(), null, PaymentMethod.PIX, 2, null, items, AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só a técnica de um item conta como alteração")
        void acceptsOnlyTechnique() {
            List<ContractItemDraft> items = List.of(
                    ContractItemDraft.withDurationPrice(otherRpgTechnique, rpgPrice, SessionFrequency.TWICE_A_WEEK));

            assertAccepted(amend(newContract(), null, PaymentMethod.PIX, 2, null, items, AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Só a linha de preço de um item conta como alteração")
        void acceptsOnlyPriceLine() {
            List<ContractItemDraft> items = List.of(
                    ContractItemDraft.withDurationPrice(rpg, rpgShorterPrice, SessionFrequency.TWICE_A_WEEK));

            assertAccepted(amend(newContract(), null, PaymentMethod.PIX, 2, null, items, AMENDMENT_DATE));
        }

        @Test
        @DisplayName("Item removido conta como alteração")
        void acceptsRemovedItem() {
            ContractEntity contract = ContractEntity.create(holder, null, plan, START, PaymentMethod.PIX, 3,
                    List.of(rpgDraft(), chiropracticDraft(SessionFrequency.TWICE_A_WEEK)), null, START);

            assertAccepted(amend(contract, null, PaymentMethod.PIX, 2, null, rpgItem(), AMENDMENT_DATE));
        }
    }

    @Nested
    @DisplayName("Número e data da versão")
    class VersionNumberAndDate {

        @Test
        @DisplayName("Contrato novo: número 0 e criado na data do cadastro")
        void newContractIsTheOriginalVersion() {
            ContractEntity contract = newContract();

            assertThat(contract.getAmendmentNumber()).isZero();
            assertThat(contract.getCreatedAt()).isEqualTo(START);
        }

        @Test
        @DisplayName("Cada aditivo soma 1 ao número e é criado na data do aditivo")
        void eachAmendmentIncrementsTheNumber() {
            LocalDate secondAmendmentDate = AMENDMENT_DATE.plusDays(10);
            ContractEntity original = newContract();

            ContractEntity first = amend(original, null, PaymentMethod.CASH, 2, null, rpgItem(), AMENDMENT_DATE);
            // Em 11/12 o dia 02 já passou: o 1º vencimento sugerido é 02/01, e até 24/01 cabe 1 parcela
            ContractEntity second = amend(first, null, PaymentMethod.PIX, 1, null, rpgItem(), secondAmendmentDate);

            assertThat(first.getAmendmentNumber()).isEqualTo(1);
            assertThat(first.getCreatedAt()).isEqualTo(AMENDMENT_DATE);
            assertThat(second.getAmendmentNumber()).isEqualTo(2);
            assertThat(second.getCreatedAt()).isEqualTo(secondAmendmentDate);
            assertThat(second.getVersionChain()).containsExactly(second, first, original);
        }
    }

    @Nested
    @DisplayName("Fim do aditivo (a mesma conta da prévia)")
    class AmendedEndDate {

        @Test
        @DisplayName("Item por duração incluído conta a partir da data do aditivo, sem alterar a versão")
        void newDurationItemCountsFromAmendmentDate() {
            ContractEntity contract = newContract();
            List<ContractItemDraft> items = List.of(rpgDraft(), chiropracticDraft(SessionFrequency.TWICE_A_WEEK));

            LocalDate previewEnd = contract.calculateAmendedEndDate(items, null, AMENDMENT_DATE);

            assertThat(previewEnd).isEqualTo(ContractEntity.endDateFor(AMENDMENT_DATE, 12));
            assertThat(contract.isActive()).isTrue();
            assertThat(amend(contract, null, PaymentMethod.PIX, 3, null, items, AMENDMENT_DATE).getEndDate())
                    .isEqualTo(previewEnd);
        }

        @Test
        @DisplayName("Recusa o fim pedido igual ao atual")
        void rejectsRequestedEndNotAfterCurrent() {
            ContractEntity contract = newContract();

            assertThatThrownBy(() -> contract.calculateAmendedEndDate(rpgItem(), CURRENT_END, AMENDMENT_DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A nova data de término precisa ser posterior ao fim atual do contrato.");
        }

        @Test
        @DisplayName("Versão substituída não pode ser alterada (nem na prévia)")
        void supersededVersionIsNotAmendable() {
            ContractEntity contract = newContract();
            amend(contract, null, PaymentMethod.CASH, 2, null, rpgItem(), AMENDMENT_DATE);

            assertThatThrownBy(contract::requireAmendable)
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Só a versão ativa do contrato pode ser alterada.");
        }
    }

    private ContractEntity newContract() {
        ContractEntity contract = ContractEntity.create(holder, null, plan, START, PaymentMethod.PIX, 3, rpgItem(),
                null, START);
        assertThat(contract.getEndDate()).isEqualTo(CURRENT_END);
        return contract;
    }

    // O aditivo com o 1º vencimento sugerido e sem nada pago (as regras do pago e do vencimento
    // estão no ContractEntityDueDateTest)
    private static ContractEntity amend(ContractEntity contract, PatientEntity beneficiary, PaymentMethod method,
            int installments, LocalDate endDate, List<ContractItemDraft> items, LocalDate amendmentDate) {
        return contract.amend(beneficiary, method, installments, endDate, items, null, NOTHING_PAID, amendmentDate);
    }

    private List<ContractItemDraft> rpgItem() {
        return List.of(rpgDraft());
    }

    private ContractItemDraft rpgDraft() {
        return ContractItemDraft.withDurationPrice(rpg, rpgPrice, SessionFrequency.TWICE_A_WEEK);
    }

    private ContractItemDraft chiropracticDraft(SessionFrequency frequency) {
        return ContractItemDraft.withDurationPrice(chiropractic, chiropracticPrice, frequency);
    }

    private static void assertAccepted(ContractEntity amended) {
        assertThat(amended.isActive()).isTrue();
        assertThat(amended.getAmendmentNumber()).isEqualTo(1);
    }

    private static PatientEntity patient(String name, String cpf) {
        return new PatientEntity(name, cpf, LocalDate.of(1990, 1, 1), null, null, null, null);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
