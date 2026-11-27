package br.com.senai.sublime_app.billing.domain;

import static br.com.senai.sublime_app.billing.domain.BillingTestData.START;
import static br.com.senai.sublime_app.billing.domain.BillingTestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.billing.domain.InstallmentState.Status;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.ContractStatus;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;

// O encerramento e o crédito (ver "Status do contrato" em Contract e "Crédito de contratos
// anteriores" em Payment, no domain-model): o status pela vigência e pelo saldo, o crédito aplicado
// no cadastro, a conta com as transferências e as parcelas depois do fim. O cenário está no
// BillingTestData: C-Ana, RPG 12 × R$ 167,00 = R$ 2.004,00, de 02/11/2026 a 24/01/2027, em 3
// parcelas de R$ 668,00
class CreditAndContractStatusTest {

    // Depois do fim do C-Ana (24/01/2027)
    private static final LocalDate AFTER_END = LocalDate.of(2027, 2, 1);
    private static final Money ZERO = Money.of(BigDecimal.ZERO);

    private BillingTestData data;

    @BeforeEach
    void setUp() {
        data = new BillingTestData();
    }

    @Test
    @DisplayName("Status: vigente na vigência; depois do fim, encerrado com o saldo zerado e vencido com pendência")
    void statusByTermAndBalance() {
        ContractEntity contract = data.anaContract();

        assertThat(contract.statusOn(LocalDate.of(2026, 12, 1), false, null)).isEqualTo(ContractStatus.CURRENT);
        assertThat(contract.statusOn(AFTER_END, false, BigDecimal.ZERO)).isEqualTo(ContractStatus.CLOSED);
        assertThat(contract.statusOn(AFTER_END, false, new BigDecimal("334.00"))).isEqualTo(ContractStatus.EXPIRED);
        assertThat(contract.statusOn(AFTER_END, false, new BigDecimal("-334.00"))).isEqualTo(ContractStatus.EXPIRED);
        assertThat(contract.statusOn(AFTER_END, true, BigDecimal.ZERO)).isEqualTo(ContractStatus.SUPERSEDED);
    }

    @Test
    @DisplayName("Cancelado: o inativado à mão, com ou sem saldo")
    void canceledStatus() {
        ContractEntity contract = data.anaContract();
        contract.deactivate(LocalDate.of(2026, 11, 20));

        assertThat(contract.statusOn(LocalDate.of(2026, 12, 1), false, null)).isEqualTo(ContractStatus.CANCELED);
        assertThat(contract.statusOn(AFTER_END, false, new BigDecimal("100.00"))).isEqualTo(ContractStatus.CANCELED);
    }

    @Test
    @DisplayName("Crédito de R$ 334,00 no cadastro: o valor parcelado é o total − o crédito")
    void creditReducesInstallmentBase() {
        ContractEntity contract = withCredit("334.00");

        assertThat(contract.getInstallmentBase().amount()).isEqualByComparingTo("1670.00");
        assertThat(contract.getCreditApplied().amount()).isEqualByComparingTo("334.00");
        assertThat(contract.getInstallmentValues().values()).extracting(Money::amount)
                .containsExactly(new BigDecimal("556.68"), new BigDecimal("556.66"), new BigDecimal("556.66"));
    }

    @Test
    @DisplayName("Crédito maior que o total: aplica só o total, e fica 1 parcela de R$ 0,00")
    void creditCoversTheTotal() {
        ContractEntity contract = withCredit("3000.00");

        assertThat(contract.getCreditApplied().amount()).isEqualByComparingTo("2004.00");
        assertThat(contract.getInstallmentBase().amount()).isEqualByComparingTo("0.00");
        assertThat(contract.getInstallments()).isEqualTo(1);
    }

    @Test
    @DisplayName("O contrato novo: o crédito recebido conta como pago, e as parcelas dividem só o resto")
    void newContractReceivesCredit() {
        ContractEntity contract = withCredit("334.00");

        // Recebeu R$ 334,00, sem pagamento ainda, nenhuma sessão: saldo credor do crédito
        ContractAccount account = ContractAccount.of(contract, List.of(), ZERO, money("334.00"), ZERO, START);

        assertThat(account.paid().amount()).isEqualByComparingTo("334.00");
        assertThat(account.balance()).isEqualByComparingTo("334.00");
        assertThat(account.creditReceived().amount()).isEqualByComparingTo("334.00");
        // O crédito cobre o "pago antes" (total − valor parcelado): as parcelas continuam em aberto
        assertThat(account.installments()).extracting(InstallmentState::status)
                .containsExactly(Status.NEXT, Status.UPCOMING, Status.UPCOMING);
    }

    @Test
    @DisplayName("O contrato antigo: o crédito enviado zera o saldo, e as parcelas continuam pagas")
    void oldContractTransfersCredit() {
        ContractEntity old = data.anaContract();

        // Pagou R$ 2.004,00, usou 10 sessões (R$ 1.670,00): R$ 334,00 de crédito, enviados ao novo
        ContractAccount before = ContractAccount.of(old, data.rpgSessions(10), money("2004.00"), ZERO, ZERO,
                AFTER_END);
        ContractAccount after = ContractAccount.of(old, data.rpgSessions(10), money("2004.00"), ZERO,
                money("334.00"), AFTER_END);

        assertThat(before.balance()).isEqualByComparingTo("334.00");
        assertThat(before.contractStatus()).isEqualTo(ContractStatus.EXPIRED);
        assertThat(after.balance()).isEqualByComparingTo("0.00");
        assertThat(after.paid().amount()).isEqualByComparingTo("1670.00");
        assertThat(after.contractStatus()).isEqualTo(ContractStatus.CLOSED);
        assertThat(after.installments()).extracting(InstallmentState::status).containsOnly(Status.PAID);
    }

    @Test
    @DisplayName("Depois do fim, a parcela em aberto é não cobrada: a conta vale pelo saldo, sem atraso")
    void installmentsAfterTheEnd() {
        // Pagou 2 parcelas (R$ 1.336,00) e usou só 6 sessões (R$ 1.002,00): R$ 334,00 de crédito
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(6), money("1336.00"),
                AFTER_END);

        assertThat(account.installments()).extracting(InstallmentState::status)
                .containsExactly(Status.PAID, Status.PAID, Status.ENDED);
        assertThat(account.hasOverdue()).isFalse();
        assertThat(account.status()).isEqualTo(AccountStatus.CREDIT);
        assertThat(account.contractStatus()).isEqualTo(ContractStatus.EXPIRED);
    }

    @Test
    @DisplayName("Quem termina devendo aparece como devendo, pelo saldo")
    void owingAfterTheEnd() {
        // Pagou 1 parcela (R$ 668,00) e usou as 12 sessões (R$ 2.004,00)
        ContractAccount account = ContractAccount.of(data.anaContract(), data.rpgSessions(12), money("668.00"),
                AFTER_END);

        assertThat(account.status()).isEqualTo(AccountStatus.OWING);
        assertThat(account.balance()).isEqualByComparingTo("-1336.00");
    }

    @Test
    @DisplayName("A transferência: dois contratos diferentes, do mesmo titular, com valor positivo")
    void transferValidations() {
        ContractEntity old = data.anaContract();
        ContractEntity anotherOfAna = data.anaContract();
        PatientEntity other = BillingTestData.withId(
                new PatientEntity("Bruno", "99999999999", LocalDate.of(1980, 1, 1), null, null, null, null), 999L);
        ContractEntity otherPatients = BillingTestData.withId(ContractEntity.create(other, null, data.evolution, START,
                PaymentMethod.PIX, 3, List.of(data.rpgDraft()), null, START), 998L);

        assertThatThrownBy(() -> new BalanceTransferEntity(old, old, money("10.00"), START))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A transferência de saldo precisa de dois contratos diferentes.");
        assertThatThrownBy(() -> new BalanceTransferEntity(old, otherPatients, money("10.00"), START))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O crédito só pode ir para um contrato do mesmo titular.");
        assertThatThrownBy(() -> new BalanceTransferEntity(old, anotherOfAna, ZERO, START))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O valor da transferência de saldo deve ser maior que zero.");
    }

    // Evolução de 02/11/2026, RPG semanal, 3 parcelas, com o crédito disponível informado
    private ContractEntity withCredit(String credit) {
        return BillingTestData.withId(ContractEntity.create(data.ana, null, data.evolution, START, PaymentMethod.PIX, 3,
                List.of(data.rpgDraft()), null, money(credit), START), 700L);
    }
}
