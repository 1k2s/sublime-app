package br.com.senai.sublime_app.billing.domain;

import static br.com.senai.sublime_app.billing.domain.BillingTestData.STANDARD_GROUP_ID;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.START;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.patient.domain.PatientEntity;

// O avulso e as contas do paciente juntas (a lista do Financeiro e a conta). O cenário está no
// BillingTestData: C-Ana em 3 parcelas de R$ 668,00 (02/11, 02/12, 02/01).
class PatientAccountSummaryTest {

    private static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);

    private BillingTestData data;
    private SingleSessionAccount noSingleSession;

    @BeforeEach
    void setUp() {
        data = new BillingTestData();
        noSingleSession = SingleSessionAccount.of(List.of(), money("0.00"));
    }

    @Test
    @DisplayName("Avulso 60 min: devido R$ 220,00 com a sugestão; pago R$ 220,00, saldo zero")
    void singleSession() {
        SingleSessionAccount open = SingleSessionAccount.of(singleSessions(1), money("0.00"));
        SingleSessionAccount settled = SingleSessionAccount.of(singleSessions(1), money("220.00"));

        assertThat(open.due().amount()).isEqualByComparingTo("220.00");
        assertThat(open.suggestions()).extracting(PaymentSuggestion::type, suggestion -> suggestion.amount().amount())
                .containsExactly(tuple(PaymentSuggestion.Type.SINGLE_SESSION_DUE, new java.math.BigDecimal("220.00")));
        assertThat(open.warnings()).extracting(AccountWarning::type)
                .containsExactly(AccountWarning.Type.SINGLE_SESSION_OPEN);
        assertThat(open.status()).isEqualTo(AccountStatus.OWING);
        assertThat(settled.balance()).isEqualByComparingTo("0.00");
        assertThat(settled.suggestions()).isEmpty();
        assertThat(settled.status()).isEqualTo(AccountStatus.UP_TO_DATE);
    }

    @Test
    @DisplayName("Sem contrato na conta nem avulso: fora da lista")
    void notListed() {
        ContractEntity settledInactive = data.anaContract();
        settledInactive.deactivate(LocalDate.of(2026, 11, 20));
        ContractAccount account = ContractAccount.of(settledInactive, data.rpgSessions(4), money("668.00"), NOV_10);

        assertThat(PatientAccountSummary.of(data.ana, List.of(), noSingleSession).isListed()).isFalse();
        assertThat(PatientAccountSummary.of(data.ana, List.of(account), noSingleSession).isListed()).isFalse();
        assertThat(PatientAccountSummary.of(data.ana, List.of(),
                SingleSessionAccount.of(List.of(), money("100.00"))).isListed()).isTrue();
    }

    @Test
    @DisplayName("Prioridade: atrasado (mesmo com crédito) → devendo → com crédito → em dia")
    void statusPriority() {
        // Pagou R$ 300,00, nada consumido, e a 1ª parcela (02/11) venceu
        assertThat(summary(contractAccount(0, "300.00", NOV_10)).status()).isEqualTo(AccountStatus.LATE);
        assertThat(summary(contractAccount(6, "668.00", START)).status()).isEqualTo(AccountStatus.OWING);
        assertThat(summary(contractAccount(2, "668.00", START)).status()).isEqualTo(AccountStatus.CREDIT);
        assertThat(summary(contractAccount(4, "668.00", START)).status()).isEqualTo(AccountStatus.UP_TO_DATE);
    }

    @Test
    @DisplayName("Duas parcelas vencidas: a quantidade, a soma e a mais antiga")
    void twoOverdue() {
        PatientAccountSummary summary = summary(contractAccount(0, "0.00", LocalDate.of(2026, 12, 10)));

        assertThat(summary.overdueCount()).isEqualTo(2);
        assertThat(summary.overdueAmount().amount()).isEqualByComparingTo("1336.00");
        assertThat(summary.oldestOverdueDate()).contains(START);
        assertThat(summary.nextDue()).get().extracting(PatientAccountSummary.NextDue::dueDate).isEqualTo(START);
    }

    @Test
    @DisplayName("Sem vencida: o próximo vencimento é a próxima parcela, com o que falta dela")
    void nextDueWithoutOverdue() {
        PatientAccountSummary summary = summary(contractAccount(0, "1000.00", NOV_10));

        assertThat(summary.nextDue()).get()
                .satisfies(next -> {
                    assertThat(next.dueDate()).isEqualTo(LocalDate.of(2026, 12, 2));
                    assertThat(next.amount().amount()).isEqualByComparingTo("336.00");
                });
    }

    @Test
    @DisplayName("Ordem: atrasados pela vencida mais antiga; devendo pelo maior saldo devedor; os outros por nome")
    void listOrder() {
        PatientAccountSummary lateSince02Nov = summaryOf("Zeca", contractAccount(0, "0.00", NOV_10));
        PatientAccountSummary lateSince02Dec = summaryOf("Amanda", lateFromDecember());
        PatientAccountSummary owing334 = summaryOf("Carla", contractAccount(6, "668.00", START));
        PatientAccountSummary owing668 = summaryOf("Davi", contractAccount(8, "668.00", START));
        PatientAccountSummary credit = summaryOf("Bruno", contractAccount(2, "668.00", START));
        PatientAccountSummary upToDate = summaryOf("Álvaro", contractAccount(4, "668.00", START));

        List<PatientAccountSummary> list = new ArrayList<>(
                List.of(credit, owing334, upToDate, lateSince02Dec, owing668, lateSince02Nov));
        list.sort(PatientAccountSummary.listOrder());

        assertThat(list).extracting(summary -> summary.patient().getName())
                .containsExactly("Zeca", "Amanda", "Davi", "Carla", "Álvaro", "Bruno");
    }

    @Test
    @DisplayName("Por nome, palavra a palavra: \"Ana Zélia\" antes de \"Anabela\"; \"Ana D1\" antes de \"Ana D14\"")
    void listOrderByNameWordByWord() {
        List<PatientAccountSummary> list = new ArrayList<>(List.of(
                summaryOf("Anabela", contractAccount(4, "668.00", START)),
                summaryOf("Ana D14", contractAccount(4, "668.00", START)),
                summaryOf("Ana Zélia", contractAccount(4, "668.00", START)),
                summaryOf("Ana D1", contractAccount(4, "668.00", START))));
        list.sort(PatientAccountSummary.listOrder());

        assertThat(list).extracting(summary -> summary.patient().getName())
                .containsExactly("Ana D1", "Ana D14", "Ana Zélia", "Anabela");
    }

    @Test
    @DisplayName("O atrasado com saldo credor está em Atrasados e em Com crédito (pelos campos)")
    void lateWithCreditInTwoFilters() {
        PatientAccountSummary summary = summary(contractAccount(0, "300.00", NOV_10));

        assertThat(summary.hasOverdue()).isTrue();
        assertThat(summary.balance()).isPositive();
    }

    @Test
    @DisplayName("C-Ana em dia e um avulso em aberto: o saldo é a soma; a situação, conta a conta")
    void contractAndSingleSession() {
        ContractAccount contract = contractAccount(2, "668.00", START);
        SingleSessionAccount single = SingleSessionAccount.of(singleSessions(1), money("0.00"));

        PatientAccountSummary summary = PatientAccountSummary.of(data.ana, List.of(contract), single);

        // Contrato: + R$ 334,00 (credor); avulso: − R$ 220,00 (devedor)
        assertThat(summary.balance()).isEqualByComparingTo("114.00");
        assertThat(summary.status()).isEqualTo(AccountStatus.OWING);
        assertThat(summary.due().amount()).isEqualByComparingTo("554.00");
        assertThat(summary.paid().amount()).isEqualByComparingTo("668.00");
        assertThat(summary.warnings()).extracting(AccountWarning::type)
                .containsExactly(AccountWarning.Type.SINGLE_SESSION_OPEN);
    }

    @Test
    @DisplayName("Na conta, o contrato ativo primeiro e o inativado só com saldo")
    void contractsShownActiveFirst() {
        ContractEntity inactive = data.anaContract();
        inactive.deactivate(LocalDate.of(2026, 11, 20));
        ContractAccount inactiveWithDebt = ContractAccount.of(inactive, data.rpgSessions(4), money("0.01"), NOV_10);
        ContractAccount active = contractAccount(4, "668.00", START);

        PatientAccountSummary summary = PatientAccountSummary.of(data.ana, List.of(inactiveWithDebt, active),
                noSingleSession);

        assertThat(summary.contracts()).containsExactly(active, inactiveWithDebt);
    }

    // ---------------------------------------------------------------------------

    private ContractAccount contractAccount(int rpgSessions, String paid, LocalDate today) {
        return ContractAccount.of(data.anaContract(), data.rpgSessions(rpgSessions), money(paid), today);
    }

    // A 1ª paga, a 2ª (02/12) vencida em 10/12
    private ContractAccount lateFromDecember() {
        return ContractAccount.of(data.anaContract(), List.of(), money("668.00"), LocalDate.of(2026, 12, 10));
    }

    private PatientAccountSummary summary(ContractAccount account) {
        return PatientAccountSummary.of(data.ana, List.of(account), noSingleSession);
    }

    private PatientAccountSummary summaryOf(String name, ContractAccount account) {
        PatientEntity patient = new PatientEntity(name, "33333333333", LocalDate.of(1990, 1, 1), null, null, null,
                null);
        return PatientAccountSummary.of(patient, List.of(account), noSingleSession);
    }

    private List<ConsultationConsumption> singleSessions(int count) {
        List<ConsultationConsumption> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(data.consumption(PricingSource.SINGLE_SESSION, "220.00", true, STANDARD_GROUP_ID));
        }
        return rows;
    }
}
