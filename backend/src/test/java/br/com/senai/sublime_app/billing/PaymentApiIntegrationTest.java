package br.com.senai.sublime_app.billing;

import static br.com.senai.sublime_app.ApiTestClient.body;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import br.com.senai.sublime_app.ApiScenario;
import br.com.senai.sublime_app.ApiTestClient.ApiResponse;
import br.com.senai.sublime_app.IntegrationTest;

// O pagamento pela API: o registro e a listagem, as validações (a mensagem de cada uma), o contrato
// inativo e o avulso, a inativação e a prévia da taxa e da previsão de recebimento. As variações
// estão nos testes de unidade (PaymentEntityTest, ReceivablesTest). Ver "Payment" no domain-model
@IntegrationTest
class PaymentApiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private ApiScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ApiScenario(context);
    }

    @Test
    @DisplayName("Registrar: 201, e a listagem do paciente traz o pagamento com o contrato e a taxa")
    void registerAndList() {
        Long ana = scenario.patient("Ana Pagamento");
        Long contract = scenario.contract(ana, 10);
        Map<String, Object> card = scenario.paymentBody(ana, contract, "668.00");
        card.put("paymentMethod", "CREDIT_CARD");
        card.put("cardInstallments", 3);
        card.put("netAmount", "641.28");

        scenario.api.post("/api/payments", card).expect(201);
        ApiResponse list = scenario.api.get("/api/payments?patientId=" + ana).expect(200);

        assertThat(list.<Integer>read("$.length()")).isEqualTo(1);
        assertThat(list.<Integer>read("$[0].contractId").longValue()).isEqualTo(contract);
        assertThat(list.<String>read("$[0].contractPlanName")).isEqualTo("Evolução");
        assertThat(list.decimal("$[0].fee")).isEqualByComparingTo("26.72");
        assertThat(list.decimal("$[0].feePercentage")).isEqualByComparingTo("4.0");
        assertThat(list.<Integer>read("$[0].cardInstallments")).isEqualTo(3);
        assertThat(list.<Boolean>read("$[0].active")).isTrue();
    }

    static Stream<Arguments> invalidPayments() {
        return Stream.of(
                invalid("valor zero", body -> {
                    body.put("amount", "0");
                    body.put("netAmount", "0");
                }, "O valor do pagamento deve ser maior que zero."),
                invalid("líquido maior que o bruto", body -> body.put("netAmount", "700.00"),
                        "O valor líquido deve ser de R$ 0,00 até o valor pago."),
                invalid("líquido negativo", body -> body.put("netAmount", "-1"),
                        "O valor líquido deve ser de R$ 0,00 até o valor pago."),
                invalid("parcelado no Pix", body -> body.put("cardInstallments", 3),
                        "Só o cartão de crédito pode ser parcelado."),
                invalid("13x no crédito", body -> {
                    body.put("paymentMethod", "CREDIT_CARD");
                    body.put("cardInstallments", 13);
                }, "O cartão pode ser parcelado de 1 a 12 vezes."),
                invalid("boleto", body -> body.put("paymentMethod", "BANK_SLIP"),
                        "Forma de pagamento não aceita: use Pix, cartão de crédito, cartão de débito ou dinheiro."),
                invalid("data futura", body -> body.put("paidAt", LocalDate.now().plusDays(1).toString()),
                        "A data do pagamento não pode ser futura."));
    }

    private static Arguments invalid(String description, Consumer<Map<String, Object>> change, String message) {
        return Arguments.of(description, change, message);
    }

    @ParameterizedTest(name = "{0} → 400")
    @MethodSource("invalidPayments")
    @DisplayName("Registrar com dados inválidos: 400 com a mensagem")
    void invalidPayment(String description, Consumer<Map<String, Object>> change, String message) {
        Long patient = scenario.patient("Bia Pagamento");
        Map<String, Object> payment = scenario.paymentBody(patient, scenario.contract(patient, 10), "668.00");
        change.accept(payment);

        scenario.api.post("/api/payments", payment).expectError(400, message);
    }

    @Test
    @DisplayName("Registrar: o contrato de outro paciente → 400; o contrato cancelado e o avulso → 201")
    void contractOwnership() {
        Long ana = scenario.patient("Ana Dona");
        Long diego = scenario.patient("Diego Outro");
        Long anaContract = scenario.contract(ana, 10);
        Long diegoContract = scenario.contract(diego, 10);
        scenario.api.delete("/api/contracts/" + diegoContract).expect(204);

        scenario.api.post("/api/payments", scenario.paymentBody(diego, anaContract, "100.00"))
                .expectError(400, "O contrato não pertence a este paciente.");
        scenario.api.post("/api/payments", scenario.paymentBody(diego, diegoContract, "100.00")).expect(201);
        scenario.api.post("/api/payments", scenario.paymentBody(diego, null, "100.00")).expect(201);
    }

    @Test
    @DisplayName("Inativar: 204 e o pagamento fica inativo na listagem; de novo → 409; inexistente → 404")
    void deactivate() {
        Long patient = scenario.patient("Caio Inativar");
        Long payment = scenario.pay(patient, scenario.contract(patient, 10), "668.00");

        scenario.api.delete("/api/payments/" + payment).expect(204);
        scenario.api.delete("/api/payments/" + payment).expectError(409, "O pagamento já está inativo.");
        scenario.api.delete("/api/payments/999999999").expect(404);
        assertThat(scenario.api.get("/api/payments?patientId=" + patient).expect(200)
                .<Boolean>read("$[0].active")).isFalse();
    }

    @Test
    @DisplayName("Prévia: crédito 3x de R$ 1.670,00 com líquido R$ 1.603,20 → taxa R$ 66,80 (4%) e 3 × R$ 534,40 nos meses seguintes")
    void previewCreditCard() {
        LocalDate paidAt = scenario.today;

        ApiResponse preview = scenario.api.post("/api/payments/preview", body("amount", "1670.00",
                "netAmount", "1603.20", "paymentMethod", "CREDIT_CARD", "cardInstallments", 3,
                "paidAt", paidAt.toString())).expect(200);

        assertThat(preview.decimal("$.fee")).isEqualByComparingTo("66.80");
        assertThat(preview.decimal("$.feePercentage")).isEqualByComparingTo("4.0");
        assertThat(preview.<List<String>>read("$.receivables[*].date")).containsExactly(
                paidAt.plusMonths(1).toString(), paidAt.plusMonths(2).toString(), paidAt.plusMonths(3).toString());
        assertThat(preview.<List<Object>>read("$.receivables[*].amount"))
                .allSatisfy(amount -> assertThat(amount.toString()).isEqualTo("534.4"));
    }

    @Test
    @DisplayName("Prévia: o Pix é recebido na data, sem taxa; o crédito 1x em 31/01 é recebido em 28/02")
    void previewPixAndEndOfMonth() {
        ApiResponse pix = scenario.api.post("/api/payments/preview", body("amount", "668.00", "netAmount", "668.00",
                "paymentMethod", "PIX", "cardInstallments", null, "paidAt", scenario.today.toString())).expect(200);
        ApiResponse card = scenario.api.post("/api/payments/preview", body("amount", "668.00", "netAmount", "641.28",
                "paymentMethod", "CREDIT_CARD", "cardInstallments", 1, "paidAt", "2026-01-31")).expect(200);

        assertThat(pix.decimal("$.fee")).isEqualByComparingTo("0.00");
        assertThat(pix.<List<String>>read("$.receivables[*].date")).containsExactly(scenario.today.toString());
        assertThat(card.<List<String>>read("$.receivables[*].date")).containsExactly("2026-02-28");
    }
}
