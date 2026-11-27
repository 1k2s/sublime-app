package br.com.senai.sublime_app.billing.domain;

import static br.com.senai.sublime_app.billing.domain.BillingTestData.GROUP_GROUP_ID;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.SPECIALIZED_GROUP_ID;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.START;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.pricing.domain.Money;

// A conta de um contrato (ver "Payment" no domain-model): consumido, mensalidades do Grupo, saldo,
// situação das parcelas, itens, avisos e sugestões. Teste de unidade puro: o consumo é dado, como
// a projeção do banco o traria. O cenário está no BillingTestData.
class ContractAccountTest {

    private BillingTestData data;

    @BeforeEach
    void setUp() {
        data = new BillingTestData();
    }

    @Test
    @DisplayName("4 sessões e R$ 668,00 pagos: consumido R$ 668,00, saldo zero, em dia")
    void upToDate() {
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(4), money("668.00"), START);

        assertThat(account.consumed().amount()).isEqualByComparingTo("668.00");
        assertThat(account.balance()).isEqualByComparingTo("0.00");
        // R$ 668,00 de R$ 2.004,00 = 33,3%
        assertThat(account.paidPercentageOfTotal()).isEqualTo(33);
        assertThat(account.status()).isEqualTo(AccountStatus.UP_TO_DATE);
        assertThat(account.installments()).extracting(InstallmentState::status).containsExactly(
                InstallmentState.Status.PAID, InstallmentState.Status.NEXT, InstallmentState.Status.UPCOMING);
    }

    @Test
    @DisplayName("6 sessões e R$ 668,00 pagos: saldo devedor de R$ 334,00")
    void owing() {
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(6), money("668.00"), START);

        assertThat(account.balance()).isEqualByComparingTo("-334.00");
        assertThat(account.status()).isEqualTo(AccountStatus.OWING);
    }

    @Test
    @DisplayName("14 sessões de RPG: 2 além das 12, R$ 334,00 a mais, com o aviso")
    void beyondContract() {
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(14), money("2004.00"),
                START);

        ContractAccount.ItemState item = account.items().getFirst();
        assertThat(item.durationMinutes()).isEqualTo(60);
        assertThat(item.frequency()).isEqualTo(SessionFrequency.ONCE_A_WEEK);
        assertThat(item.weeklyFrequency()).isNull();
        assertThat(item.used()).isEqualTo(14);
        assertThat(item.extraCount()).isEqualTo(2);
        assertThat(item.extraValue().amount()).isEqualByComparingTo("334.00");
        assertThat(account.warnings()).extracting(AccountWarning::type)
                .contains(AccountWarning.Type.BEYOND_CONTRACT);
    }

    @Test
    @DisplayName("O de-para (a técnica fora do contrato) soma no consumido")
    void equivalentPlanCountsAsConsumed() {
        List<ConsultationConsumption> rows = new ArrayList<>(data.rpgSessions(2));
        rows.add(data.consumption(PricingSource.EQUIVALENT_PLAN, "132.00", true, SPECIALIZED_GROUP_ID));

        ContractAccount account = ContractAccount.of(data.anaContract(), rows, money("0.01"), START);

        assertThat(account.consumed().amount()).isEqualByComparingTo("466.00");
        assertThat(account.items().getFirst().used()).isEqualTo(2);
    }

    @Test
    @DisplayName("Só Trimestral 2x: duas mensalidades vencidas; as aulas não somam")
    void groupFeesDue() {
        ContractEntity contract = groupContract(3);
        List<ConsultationConsumption> classes = List.of(
                data.consumption(PricingSource.CONTRACT_ITEM, "65.81", false, GROUP_GROUP_ID),
                data.consumption(PricingSource.CONTRACT_ITEM, "65.81", false, GROUP_GROUP_ID));

        // Vencimentos 20/10, 20/11 (e 20/12): em 25/11, duas vencidas = 570,36 + 570,35
        ContractAccount account = ContractAccount.of(contract, classes, money("0.01"), LocalDate.of(2026, 11, 25));

        assertThat(account.consumed().amount()).isEqualByComparingTo("0.00");
        assertThat(account.groupFeesDue().amount()).isEqualByComparingTo("1140.71");
        assertThat(account.items().getFirst().groupItem()).isTrue();
        assertThat(account.items().getFirst().weeklyFrequency()).isEqualTo(2);
        assertThat(account.items().getFirst().durationMinutes()).isNull();
        assertThat(account.items().getFirst().used()).isEqualTo(2);
        assertThat(account.items().getFirst().contracted()).isEqualTo(26);
    }

    @Test
    @DisplayName("Contrato misto (Grupo + RPG): devido = o consumo do RPG + as mensalidades vencidas")
    void mixedContract() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        ContractEntity contract = ContractEntity.create(data.ana, null, data.quarterly, start, PaymentMethod.PIX, 3,
                List.of(data.groupDraft(), data.rpgDraft()), null, start);
        List<ConsultationConsumption> rows = new ArrayList<>(data.rpgSessions(2));
        rows.add(data.consumption(PricingSource.CONTRACT_ITEM, "65.81", false, GROUP_GROUP_ID));

        ContractAccount account = ContractAccount.of(contract, rows, money("0.01"), LocalDate.of(2026, 11, 25));

        // RPG: 2 × R$ 167,00; Grupo: 20/10 e 20/11 vencidas = 570,36 + 570,35
        assertThat(account.consumed().amount()).isEqualByComparingTo("334.00");
        assertThat(account.groupFeesDue().amount()).isEqualByComparingTo("1140.71");
        assertThat(account.due().amount()).isEqualByComparingTo("1474.71");
    }

    @Test
    @DisplayName("Pago acima do valor parcelado: as parcelas não passam de pagas; o excedente é saldo credor")
    void paidAboveInstallmentBase() {
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(4), money("2500.00"),
                START);

        assertThat(account.installments()).allSatisfy(installment -> {
            assertThat(installment.status()).isEqualTo(InstallmentState.Status.PAID);
            assertThat(installment.paid().amount()).isEqualByComparingTo(installment.value().amount());
        });
        assertThat(account.balance()).isEqualByComparingTo("1832.00");
        assertThat(account.status()).isEqualTo(AccountStatus.CREDIT);
        assertThat(account.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("Grupo pago à vista: o saldo credor diminui a cada mensalidade vencida")
    void groupPaidUpfrontCreditDecreases() {
        ContractEntity contract = groupContract(1);

        BigDecimal afterFirst = ContractAccount.of(contract, List.of(), money("1711.06"), LocalDate.of(2026, 10, 20))
                .balance();
        BigDecimal afterSecond = ContractAccount.of(contract, List.of(), money("1711.06"), LocalDate.of(2026, 11, 20))
                .balance();

        assertThat(afterFirst).isEqualByComparingTo("1140.70");
        assertThat(afterSecond).isEqualByComparingTo("570.35");
    }

    @Test
    @DisplayName("Inativado: nada vence depois; aparece na conta só com saldo")
    void inactivatedContract() {
        ContractEntity contract = data.anaContract();
        contract.deactivate(LocalDate.of(2026, 11, 20));
        // Ainda na vigência (o fim é 24/01/2027; depois dele, as parcelas em aberto são não cobradas, no CreditAndContractStatusTest)
        LocalDate later = LocalDate.of(2026, 12, 10);

        ContractAccount withDebt = ContractAccount.of(contract, data.rpgSessions(4), money("0.01"), later);
        ContractAccount settled = ContractAccount.of(contract, data.rpgSessions(4), money("668.00"), later);

        // A 1ª (02/11) venceu antes da inativação; as outras seriam depois dela
        assertThat(withDebt.installments()).extracting(InstallmentState::status).containsExactly(
                InstallmentState.Status.OVERDUE, InstallmentState.Status.CANCELED, InstallmentState.Status.CANCELED);
        assertThat(withDebt.warnings()).extracting(AccountWarning::type)
                .contains(AccountWarning.Type.INACTIVE_WITH_DEBT);
        assertThat(withDebt.isShownInAccount()).isTrue();
        assertThat(settled.isShownInAccount()).isFalse();
    }

    @Test
    @DisplayName("Aditivo e o pagamento de antes inativado: a dívida volta às parcelas do aditivo")
    void inactivatedPaymentBeforeAmendment() {
        // Aditivo em 01/12 com R$ 668,00 pagos: parcelado R$ 1.336,00 (o total, R$ 2.004,00, − 668,00)
        ContractEntity amended = data.amend(data.anaContract(), 2, List.of(data.rpgDraft()), "668.00",
                LocalDate.of(2026, 12, 1));

        // O pagamento de R$ 668,00 foi inativado: o pago da cadeia vai a zero
        ContractAccount account = ContractAccount.of(amended, List.of(), Money.of(BigDecimal.ZERO),
                LocalDate.of(2026, 12, 1));

        assertThat(amended.getInstallmentBase().amount()).isEqualByComparingTo("1336.00");
        assertThat(account.suggestions())
                .filteredOn(suggestion -> suggestion.type() == PaymentSuggestion.Type.ALL_OPEN_INSTALLMENTS)
                .singleElement()
                .satisfies(suggestion -> assertThat(suggestion.amount().amount()).isEqualByComparingTo("1336.00"));
    }

    @Test
    @DisplayName("Sugestões com a 1ª vencida e parcial: a parcela em aberto e todas")
    void suggestionsWithPartialOverdue() {
        // Em 10/11 a 1ª (02/11) venceu; pagos R$ 556,66 dela
        ContractAccount account = ContractAccount.of(data.anaContract(), List.of(), money("556.66"),
                LocalDate.of(2026, 11, 10));

        assertThat(account.status()).isEqualTo(AccountStatus.LATE);
        assertThat(account.suggestions()).extracting(PaymentSuggestion::type, suggestion -> suggestion.amount().amount())
                .containsExactly(
                        tuple(PaymentSuggestion.Type.OPEN_INSTALLMENT, new BigDecimal("111.34")),
                        tuple(PaymentSuggestion.Type.ALL_OPEN_INSTALLMENTS, new BigDecimal("1447.34")));
        assertThat(account.suggestions().getFirst().dueDate()).isEqualTo(START);
        assertThat(account.suggestions().get(1).dueDate()).isNull();
        assertThat(account.overdueCount()).isEqualTo(1);
        assertThat(account.overdueAmount().amount()).isEqualByComparingTo("111.34");
    }

    @Test
    @DisplayName("Parcelas quitadas, 2 sessões além: só o saldo devedor")
    void onlyBalanceDue() {
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(14), money("2004.00"),
                START);

        assertThat(account.suggestions()).extracting(PaymentSuggestion::type)
                .containsExactly(PaymentSuggestion.Type.BALANCE_DUE);
        assertThat(account.suggestions().getFirst().amount().amount()).isEqualByComparingTo("334.00");
    }

    @Test
    @DisplayName("Atrasado com saldo credor: pagou R$ 300,00, nada consumido, a 1ª parcela venceu")
    void lateWithCredit() {
        ContractAccount account = ContractAccount.of(data.anaContract(), List.of(), money("300.00"),
                LocalDate.of(2026, 11, 10));

        assertThat(account.status()).isEqualTo(AccountStatus.LATE);
        assertThat(account.balance()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Item incluído pelo aditivo vem marcado")
    void itemAddedInAmendment() {
        ContractEntity amended = data.amend(data.anaContract(), 2, List.of(data.rpgDraft(), data.chiropracticDraft()),
                "0.00", LocalDate.of(2026, 12, 1));

        ContractAccount account = ContractAccount.of(amended, List.of(), Money.of(BigDecimal.ZERO),
                LocalDate.of(2026, 12, 1));

        assertThat(account.items()).extracting(ContractAccount.ItemState::techniqueName,
                ContractAccount.ItemState::addedInCurrentVersion)
                .containsExactlyInAnyOrder(tuple("RPG", false), tuple("Quiropraxia", true));
    }

    // Trimestral 2x de 20/10/2026: 26 aulas × R$ 65,81 = R$ 1.711,06
    private ContractEntity groupContract(int installments) {
        LocalDate start = LocalDate.of(2026, 10, 20);
        return BillingTestData.withId(ContractEntity.create(data.ana, null, data.quarterly, start, PaymentMethod.PIX,
                installments, List.of(data.groupDraft()), null, start), 500L);
    }
}
