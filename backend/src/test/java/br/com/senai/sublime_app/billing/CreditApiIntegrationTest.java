package br.com.senai.sublime_app.billing;

import static br.com.senai.sublime_app.ApiTestClient.body;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import br.com.senai.sublime_app.ApiScenario;
import br.com.senai.sublime_app.ApiTestClient.ApiResponse;
import br.com.senai.sublime_app.IntegrationTest;

// O crédito de contratos anteriores e o status do contrato terminado, pela API: o disponível na
// prévia, o cadastro que o aplica, as transferências, a conta de quem enviou e de quem recebeu, o
// crédito maior que o total e a parcela não cobrada depois do fim. Os contratos terminados são o
// Evolução RPG semanal padrão com início há 100 dias (as 12 semanas já acabaram). Ver "Status do
// contrato" em Contract e "Crédito de contratos anteriores" em Payment, no domain-model
@IntegrationTest
class CreditApiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private ApiScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ApiScenario(context);
    }

    @Test
    @DisplayName("Crédito de R$ 334,00: vencido antes; a prévia e o cadastro aplicam; o antigo fica encerrado e o crédito não volta")
    void creditFromEndedContract() {
        Long ana = scenario.patient("Ana Crédito");
        Long old = endedContract(ana, "2004.00", 10, 100);
        assertThat(status(old)).isEqualTo("EXPIRED");

        ApiResponse preview = preview(ana);
        Map<String, Object> request = scenario.contractBody(ana, scenario.today);
        request.put("applyCredit", true);
        ApiResponse created = scenario.contract(request);
        ApiResponse transfers = scenario.api.get("/api/billing/patients/" + ana + "/transfers").expect(200);
        ApiResponse account = scenario.account(ana);
        ApiResponse oldInstallments = scenario.api.get("/api/billing/contracts/" + old + "/installments").expect(200);

        assertThat(preview.decimal("$.availableCredit")).isEqualByComparingTo("334.00");
        assertThat(preview.decimal("$.creditApplied")).isEqualByComparingTo("334.00");
        assertThat(preview.decimal("$.installmentBase")).isEqualByComparingTo("1670.00");
        assertThat(created.decimal("$.installmentBase")).isEqualByComparingTo("1670.00");
        assertThat(created.<String>read("$.status")).isEqualTo("CURRENT");
        assertThat(transfers.<Integer>read("$.length()")).isEqualTo(1);
        assertThat(transfers.longAt("$[0].fromContractId")).isEqualTo(old);
        assertThat(transfers.longAt("$[0].toContractId")).isEqualTo(created.id());
        assertThat(transfers.decimal("$[0].amount")).isEqualByComparingTo("334.00");
        assertThat(status(old)).isEqualTo("CLOSED");

        String newer = "$.contracts[?(@.contractId == " + created.id() + ")]";
        String older = "$.contracts[?(@.contractId == " + old + ")]";
        assertThat(single(account, newer + ".creditReceived")).isEqualTo("334.0");
        assertThat(single(account, newer + ".balance")).isEqualTo("334.0");
        assertThat(single(account, older + ".creditTransferred")).isEqualTo("334.0");
        assertThat(single(account, older + ".balance")).isEqualTo("0.0");
        assertThat(single(account, older + ".contractStatus")).isEqualTo("CLOSED");
        // O crédito transferido não desfaz as parcelas pagas do antigo
        assertThat(oldInstallments.<List<String>>read("$[0].installments[*].status"))
                .containsExactly("PAID", "PAID", "PAID");
        assertThat(preview(ana).decimal("$.availableCredit")).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Dois contratos terminados com crédito: o novo aplica os dois (R$ 668,00), e os dois ficam encerrados")
    void creditFromTwoEndedContracts() {
        Long bia = scenario.patient("Bia Dois Créditos");
        Long older = endedContract(bia, "2004.00", 10, 200);
        Long newer = endedContract(bia, "2004.00", 10, 100);

        Map<String, Object> request = scenario.contractBody(bia, scenario.today);
        request.put("applyCredit", true);
        ApiResponse created = scenario.contract(request);
        ApiResponse transfers = scenario.api.get("/api/billing/patients/" + bia + "/transfers").expect(200);

        assertThat(created.decimal("$.installmentBase")).isEqualByComparingTo("1336.00");
        assertThat(transfers.<List<Integer>>read("$[*].fromContractId"))
                .containsExactlyInAnyOrder(older.intValue(), newer.intValue());
        assertThat(transfers.<List<Integer>>read("$[*].toContractId")).containsOnly(created.id().intValue());
        assertThat(transfers.<List<Object>>read("$[*].amount"))
                .allSatisfy(amount -> assertThat(amount.toString()).isEqualTo("334.0"));
        assertThat(status(older)).isEqualTo("CLOSED");
        assertThat(status(newer)).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Sem aplicar (a caixa desmarcada): o parcelado é o total e nada é transferido")
    void creditNotApplied() {
        Long caio = scenario.patient("Caio Sem Aplicar");
        Long old = endedContract(caio, "2004.00", 10, 100);

        ApiResponse created = scenario.contract(scenario.contractBody(caio, scenario.today));

        assertThat(created.decimal("$.installmentBase")).isEqualByComparingTo("2004.00");
        assertThat(scenario.api.get("/api/billing/patients/" + caio + "/transfers").expect(200)
                .<List<Object>>read("$")).isEmpty();
        assertThat(status(old)).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Crédito maior que o total: aplica R$ 1.584,00 (o total), e R$ 420,00 continuam no antigo, vencido")
    void creditLargerThanTheTotal() {
        Long davi = scenario.patient("Davi Crédito Maior");
        Long old = endedContract(davi, "2004.00", 0, 100);

        // Só Quiropraxia quinzenal: 12 × R$ 132,00
        Map<String, Object> request = scenario.contractBody(davi, scenario.today);
        request.put("installments", 1);
        request.put("items", List.of(scenario.chiropracticItem("BIWEEKLY")));
        request.put("applyCredit", true);
        ApiResponse created = scenario.contract(request);

        assertThat(created.decimal("$.installmentBase")).isEqualByComparingTo("0.00");
        assertThat(scenario.api.get("/api/billing/patients/" + davi + "/transfers").expect(200)
                .decimal("$[0].amount")).isEqualByComparingTo("1584.00");
        assertThat(single(scenario.account(davi), "$.contracts[?(@.contractId == " + old + ")].balance"))
                .isEqualTo("420.0");
        assertThat(status(old)).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Parcela em aberto depois do fim: não cobrada (ENDED), sem atraso; a conta pelo saldo (crédito)")
    void installmentNotChargedAfterTheEnd() {
        Long eva = scenario.patient("Eva Fim");
        // 2 parcelas pagas (R$ 1.336,00) e 6 sessões (R$ 1.002,00): R$ 334,00 de crédito
        Long ended = endedContract(eva, "1336.00", 6, 100);

        ApiResponse installments = scenario.api.get("/api/billing/contracts/" + ended + "/installments").expect(200);
        ApiResponse account = scenario.account(eva);

        assertThat(installments.<List<String>>read("$[0].installments[*].status"))
                .containsExactly("PAID", "PAID", "ENDED");
        assertThat(account.<String>read("$.contracts[0].status")).isEqualTo("CREDIT");
        assertThat(account.<String>read("$.contracts[0].contractStatus")).isEqualTo("EXPIRED");
        assertThat(account.<Integer>read("$.contracts[0].installments.overdueCount")).isZero();
    }

    // ---------------------------------------------------------------------------
    // Dados de teste
    // ---------------------------------------------------------------------------

    // O contrato padrão com início há N dias (100 ou mais: já terminou), o pago e as sessões dadas
    private Long endedContract(Long patientId, String paid, int sessions, int startDaysAgo) {
        Long contract = scenario.contract(patientId, startDaysAgo);
        scenario.pay(patientId, contract, paid);
        scenario.launchMany(patientId, contract, "RPG", scenario.today.minusDays(startDaysAgo), sessions);
        return contract;
    }

    private ApiResponse preview(Long patientId) {
        return scenario.api.post("/api/contracts/preview", body("planId", scenario.planId("Evolução"),
                "startDate", scenario.today.toString(), "items", List.of(scenario.rpgItem()), "installments", 3,
                "firstDueDate", null, "patientId", patientId, "applyCredit", true)).expect(200);
    }

    private String status(Long contractId) {
        return scenario.api.get("/api/contracts/" + contractId).expect(200).read("$.status");
    }

    // O único valor de um filtro do JsonPath (que sempre devolve uma lista), como texto
    private static String single(ApiResponse response, String path) {
        List<Object> values = response.read(path);
        assertThat(values).as(path).hasSize(1);
        return values.getFirst().toString();
    }
}
