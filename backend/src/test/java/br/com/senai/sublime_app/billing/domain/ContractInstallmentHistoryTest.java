package br.com.senai.sublime_app.billing.domain;

import static br.com.senai.sublime_app.billing.domain.BillingTestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.billing.domain.InstallmentState.Status;
import br.com.senai.sublime_app.contract.domain.ContractEntity;

// As parcelas de cada versão do contrato (a aba Parcelas). O cenário está no BillingTestData:
// C-Ana em 3 parcelas de R$ 668,00 (02/11, 02/12, 02/01).
class ContractInstallmentHistoryTest {

    private static final LocalDate AMENDMENT_DATE = LocalDate.of(2026, 12, 1);

    private BillingTestData data;

    @BeforeEach
    void setUp() {
        data = new BillingTestData();
    }

    @Test
    @DisplayName("Na 2ª parcela, sem pagar a 1ª: 1ª atrasada, 2ª a próxima, 3ª a vencer")
    void overdueFirst() {
        ContractInstallmentHistory history = ContractInstallmentHistory.of(data.anaContract(), money("0.00"),
                LocalDate.of(2026, 11, 20));

        ContractInstallmentHistory.Version version = history.versions().getFirst();
        assertThat(version.current()).isTrue();
        assertThat(version.installments()).extracting(InstallmentState::status)
                .containsExactly(Status.OVERDUE, Status.NEXT, Status.UPCOMING);
    }

    @Test
    @DisplayName("Depois do 2º vencimento sem pagar: as duas atrasadas")
    void twoOverdue() {
        ContractInstallmentHistory history = ContractInstallmentHistory.of(data.anaContract(), money("0.00"),
                LocalDate.of(2026, 12, 10));

        assertThat(history.versions().getFirst().installments()).extracting(InstallmentState::status)
                .containsExactly(Status.OVERDUE, Status.OVERDUE, Status.NEXT);
    }

    @Test
    @DisplayName("R$ 1.000,00 pagos: 1ª paga, 2ª com R$ 332,00 (pela ordem de vencimento)")
    void paidDistributedByDueDate() {
        ContractInstallmentHistory history = ContractInstallmentHistory.of(data.anaContract(), money("1000.00"),
                LocalDate.of(2026, 11, 20));

        assertThat(history.versions().getFirst().installments())
                .extracting(InstallmentState::status, installment -> installment.paid().amount())
                .containsExactly(
                        tuple(Status.PAID, new BigDecimal("668.00")),
                        tuple(Status.NEXT, new BigDecimal("332.00")),
                        tuple(Status.UPCOMING, new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("Depois do aditivo que inclui Quiropraxia: a mais nova primeiro; na original, as não pagas substituídas")
    void afterAmendment() {
        // Com R$ 668,00 pagos, inclui Quiropraxia 30 min semanal (12 × R$ 132,00); total R$ 3.588,00
        ContractEntity original = data.anaContract();
        ContractEntity amended = data.amend(original, 2, List.of(data.rpgDraft(), data.chiropracticDraft()),
                "668.00", AMENDMENT_DATE);

        ContractInstallmentHistory history = ContractInstallmentHistory.of(amended, money("668.00"), AMENDMENT_DATE);

        assertThat(history.versions()).extracting(ContractInstallmentHistory.Version::contract,
                ContractInstallmentHistory.Version::current)
                .containsExactly(tuple(amended, true), tuple(original, false));
        ContractInstallmentHistory.Version current = history.versions().getFirst();
        assertThat(current.paidBeforeAmendment().amount()).isEqualByComparingTo("668.00");
        assertThat(current.contract().getInstallmentBase().amount()).isEqualByComparingTo("2920.00");
        assertThat(current.dueDay()).isEqualTo(2);
        assertThat(current.installments()).extracting(InstallmentState::status)
                .containsExactly(Status.NEXT, Status.UPCOMING);
        ContractInstallmentHistory.Version previous = history.versions().get(1);
        assertThat(previous.paidBeforeAmendment().amount()).isEqualByComparingTo("0.00");
        assertThat(previous.installments()).extracting(InstallmentState::status)
                .containsExactly(Status.PAID, Status.REPLACED, Status.REPLACED);
    }

    @Test
    @DisplayName("Um pagamento depois do aditivo não muda as parcelas da versão substituída")
    void paymentAfterAmendmentGoesToCurrent() {
        ContractEntity original = data.anaContract();
        ContractEntity amended = data.amend(original, 2, List.of(data.rpgDraft(), data.chiropracticDraft()),
                "668.00", AMENDMENT_DATE);

        // + R$ 1.460,00 depois do aditivo: a 1ª do aditivo (R$ 2.920,00 em 2 = R$ 1.460,00) paga
        ContractInstallmentHistory history = ContractInstallmentHistory.of(amended, money("2128.00"),
                AMENDMENT_DATE);

        assertThat(history.versions().getFirst().installments()).extracting(InstallmentState::status)
                .containsExactly(Status.PAID, Status.NEXT);
        assertThat(history.versions().get(1).installments()).extracting(InstallmentState::status)
                .containsExactly(Status.PAID, Status.REPLACED, Status.REPLACED);
    }

    @Test
    @DisplayName("Todas as parcelas em aberto pagas no crédito 3x: todas pagas na hora")
    void allPaidAtOnceByCard() {
        // O pago é o bruto do pagamento, mesmo parcelado no cartão
        ContractInstallmentHistory history = ContractInstallmentHistory.of(data.anaContract(), money("2004.00"),
                LocalDate.of(2026, 11, 2));

        assertThat(history.versions().getFirst().installments()).extracting(InstallmentState::status)
                .containsOnly(Status.PAID);
    }

    @Test
    @DisplayName("Aditivo em que o pago cobre o total novo: 1 parcela de R$ 0,00, paga")
    void nothingToSplit() {
        // Original com RPG e Quiropraxia (R$ 3.588,00); com R$ 2.004,00 pagos, a Quiropraxia sai
        ContractEntity original = data.contract(List.of(data.rpgDraft(), data.chiropracticDraft()));
        ContractEntity amended = data.amend(original, 1, List.of(data.rpgDraft()), "2004.00", AMENDMENT_DATE);

        ContractInstallmentHistory history = ContractInstallmentHistory.of(amended, money("2004.00"), AMENDMENT_DATE);

        assertThat(history.versions().getFirst().installments())
                .extracting(InstallmentState::status, installment -> installment.value().amount())
                .containsExactly(tuple(Status.PAID, new BigDecimal("0.00")));
    }
}
