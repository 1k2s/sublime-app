package br.com.senai.sublime_app.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemDraft;
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
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// Regras do pagamento (ver "Payment" no domain-model): valor livre, bruto e líquido, parcelas só no
// crédito, data até hoje, contrato do paciente como titular e inativação para corrigir. Teste de
// unidade puro: sem Spring e sem banco. O ReflectionTestUtils só preenche os ids, que em produção
// vêm do banco.
class PaymentEntityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    private PatientEntity ana;
    private PatientEntity diego;
    private ContractEntity anaContract;
    private UserEntity admin;

    @BeforeEach
    void setUp() {
        ana = withId(new PatientEntity("Ana", "11111111111", LocalDate.of(1990, 1, 1), null, null, null, null), 1L);
        diego = withId(new PatientEntity("Diego", "22222222222", LocalDate.of(1990, 1, 1), null, null, null, null), 2L);
        admin = withId(new UserEntity("admin@clinica.com", "hash", Role.ADMIN), 10L);

        PlanEntity evolution = withId(new PlanEntity("Evolução", PlanType.PACKAGE, 12, null), 20L);
        PricingGroupEntity standardGroup = withId(
                new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED), 21L);
        TechniqueEntity rpg = withId(new TechniqueEntity("RPG", standardGroup), 22L);
        SessionDurationPriceEntity rpgPrice = withId(
                new SessionDurationPriceEntity(standardGroup, evolution, 60, money("167.00"), TODAY), 23L);
        anaContract = withId(ContractEntity.create(ana, null, evolution, TODAY, PaymentMethod.PIX, 3,
                List.of(ContractItemDraft.withDurationPrice(rpg, rpgPrice, SessionFrequency.ONCE_A_WEEK)), null, TODAY),
                30L);
    }

    @Test
    @DisplayName("Registra um Pix no contrato do titular, ativo")
    void registersPixOnHolderContract() {
        PaymentEntity payment = pay(anaContract, "668.00", "668.00", PaymentMethod.PIX, 1, TODAY);

        assertThat(payment.isActive()).isTrue();
        assertThat(payment.getFee().amount()).isEqualByComparingTo("0.00");
        assertThat(payment.getRegisteredBy()).isSameAs(admin);
    }

    @Test
    @DisplayName("Recusa o valor zero")
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> pay(anaContract, "0.00", "0.00", PaymentMethod.PIX, 1, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O valor do pagamento deve ser maior que zero.");
    }

    @Test
    @DisplayName("Recusa o líquido maior que o bruto")
    void rejectsNetAboveGross() {
        assertThatThrownBy(() -> pay(anaContract, "100.00", "100.01", PaymentMethod.CREDIT_CARD, 1, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O valor líquido deve ser de R$ 0,00 até o valor pago.");
    }

    @Test
    @DisplayName("Aceita o líquido zero e o líquido igual ao bruto")
    void acceptsNetFromZeroToGross() {
        assertThat(pay(anaContract, "100.00", "0.00", PaymentMethod.CREDIT_CARD, 1, TODAY).getFee().amount())
                .isEqualByComparingTo("100.00");
        assertThat(pay(anaContract, "100.00", "100.00", PaymentMethod.PIX, 1, TODAY).getFee().amount())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Recusa parcelar fora do crédito")
    void rejectsInstallmentsOutsideCredit() {
        assertThatThrownBy(() -> pay(anaContract, "300.00", "300.00", PaymentMethod.PIX, 3, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Só o cartão de crédito pode ser parcelado.");
    }

    @Test
    @DisplayName("Recusa o crédito em 13 vezes (e em 0)")
    void rejectsCardInstallmentsOutOfRange() {
        assertThatThrownBy(() -> pay(anaContract, "300.00", "290.00", PaymentMethod.CREDIT_CARD, 13, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O cartão pode ser parcelado de 1 a 12 vezes.");
        assertThatThrownBy(() -> pay(anaContract, "300.00", "290.00", PaymentMethod.CREDIT_CARD, 0, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O cartão pode ser parcelado de 1 a 12 vezes.");
    }

    @Test
    @DisplayName("Recusa a data futura; aceita a de hoje e a passada")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> pay(anaContract, "100.00", "100.00", PaymentMethod.PIX, 1, TODAY.plusDays(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A data do pagamento não pode ser futura.");
        assertThat(pay(anaContract, "100.00", "100.00", PaymentMethod.PIX, 1, TODAY.minusMonths(2)).isActive())
                .isTrue();
    }

    @Test
    @DisplayName("Recusa o contrato de outro paciente")
    void rejectsContractOfAnotherPatient() {
        assertThatThrownBy(() -> new PaymentEntity(diego, anaContract, admin, money("100.00"), money("100.00"), 1,
                TODAY, PaymentMethod.PIX, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O contrato não pertence a este paciente.");
    }

    @Test
    @DisplayName("Aceita o pagamento num contrato inativado")
    void acceptsInactiveContract() {
        anaContract.deactivate(TODAY);

        assertThat(pay(anaContract, "100.00", "100.00", PaymentMethod.PIX, 1, TODAY).isActive()).isTrue();
    }

    @Test
    @DisplayName("Aceita o pagamento sem contrato (avulso)")
    void acceptsPaymentWithoutContract() {
        PaymentEntity payment = new PaymentEntity(diego, null, admin, money("220.00"), money("220.00"), 1, TODAY,
                PaymentMethod.CASH, TODAY);

        assertThat(payment.getContract()).isNull();
    }

    @Test
    @DisplayName("Recusa o boleto")
    void rejectsBankSlip() {
        assertThatThrownBy(() -> pay(anaContract, "100.00", "100.00", PaymentMethod.BANK_SLIP, 1, TODAY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Forma de pagamento não aceita: use Pix, cartão de crédito, cartão de débito ou dinheiro.");
    }

    @Test
    @DisplayName("Inativa; de novo, 409")
    void deactivatesOnce() {
        PaymentEntity payment = pay(anaContract, "100.00", "100.00", PaymentMethod.PIX, 1, TODAY);

        payment.deactivate();

        assertThat(payment.isActive()).isFalse();
        assertThatThrownBy(payment::deactivate)
                .isInstanceOf(ConflictException.class)
                .hasMessage("O pagamento já está inativo.");
    }

    @Test
    @DisplayName("Soma só os brutos (o que entra na conta do paciente)")
    void sumsGrossAmounts() {
        List<PaymentEntity> payments = List.of(
                pay(anaContract, "668.00", "641.28", PaymentMethod.CREDIT_CARD, 1, TODAY),
                pay(anaContract, "668.00", "668.00", PaymentMethod.PIX, 1, TODAY));

        assertThat(PaymentEntity.sumOfAmounts(payments).amount()).isEqualByComparingTo("1336.00");
        assertThat(PaymentEntity.sumOfAmounts(List.of()).amount()).isEqualByComparingTo("0.00");
    }

    private PaymentEntity pay(ContractEntity contract, String amount, String netAmount, PaymentMethod method,
            int cardInstallments, LocalDate paidAt) {
        return new PaymentEntity(ana, contract, admin, money(amount), money(netAmount), cardInstallments, paidAt,
                method, TODAY);
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
