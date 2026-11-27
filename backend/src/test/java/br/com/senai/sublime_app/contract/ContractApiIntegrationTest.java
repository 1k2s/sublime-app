package br.com.senai.sublime_app.contract;

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

// O contrato pela API: o cadastro com o vencimento e o cronograma, a prévia, o aditivo com o pago
// (o que falta, o máximo e o sugerido), o "sem alteração", o cancelamento e o status. As variações
// das regras estão nos testes de unidade (ContractEntity...Test); aqui, o contrato de cada
// endpoint (o JSON, o código, a mensagem). Ver "Contract e ContractItem" no domain-model
@IntegrationTest
class ContractApiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private ApiScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ApiScenario(context);
    }

    @Test
    @DisplayName("Cadastro: sem 1º vencimento vale o início; 3 × R$ 668,00 nos meses seguidos; status vigente")
    void createWithSchedule() {
        LocalDate start = scenario.today.minusDays(10);

        ApiResponse created = scenario.contract(scenario.contractBody(scenario.patient("Ana"), start));

        assertThat(created.<String>read("$.firstDueDate")).isEqualTo(start.toString());
        assertThat(created.decimal("$.installmentBase")).isEqualByComparingTo("2004.00");
        assertThat(created.<List<String>>read("$.installmentSchedule[*].dueDate"))
                .containsExactly(start.toString(), start.plusMonths(1).toString(), start.plusMonths(2).toString());
        assertThat(created.<List<Object>>read("$.installmentSchedule[*].value"))
                .allSatisfy(value -> assertThat(value.toString()).isEqualTo("668.0"));
        assertThat(created.<String>read("$.status")).isEqualTo("CURRENT");
        assertThat(created.<Object>read("$.deactivatedOn")).isNull();
    }

    @Test
    @DisplayName("Cadastro: 1º vencimento fora do intervalo (antes do início ou mais de um mês depois) → 400")
    void firstDueDateOutOfRange() {
        LocalDate start = scenario.today.minusDays(10);
        String message = "O 1º vencimento deve ser entre o início do contrato e um mês depois.";

        Map<String, Object> before = scenario.contractBody(scenario.patient("Bia"), start);
        before.put("firstDueDate", start.minusDays(1).toString());
        Map<String, Object> after = scenario.contractBody(scenario.patient("Caio"), start);
        after.put("firstDueDate", start.plusMonths(1).plusDays(1).toString());

        scenario.api.post("/api/contracts", before).expectError(400, message);
        scenario.api.post("/api/contracts", after).expectError(400, message);
    }

    @Test
    @DisplayName("Prévia: o 1º vencimento em 31/01 dá 31/01, 28/02 e 31/03 (cada um a partir do 1º)")
    void previewEndOfMonth() {
        ApiResponse preview = scenario.api.post("/api/contracts/preview", body("planId", scenario.planId("Evolução"),
                "startDate", "2027-01-15", "items", List.of(scenario.rpgItem()), "installments", 3,
                "firstDueDate", "2027-01-31")).expect(200);

        assertThat(preview.<List<String>>read("$.installmentSchedule[*].dueDate"))
                .containsExactly("2027-01-31", "2027-02-28", "2027-03-31");
    }

    @Test
    @DisplayName("Aditivo: a prévia com o pago; o 1º vencimento antes de hoje e parcelas acima do máximo → 400; a versão anterior substituída")
    void amendmentWithPaid() {
        Long ana = scenario.patient("Ana Aditivo");
        Long original = scenario.contract(ana, 20);
        scenario.pay(ana, original, "668.00");
        List<Map<String, Object>> items = List.of(scenario.rpgItem(), scenario.chiropracticItem("ONCE_A_WEEK"));

        ApiResponse preview = scenario.api.post("/api/contracts/" + original + "/amendments/preview",
                body("endDate", null, "items", items, "installments", 1, "firstDueDate", null)).expect(200);
        // Inclui Quiropraxia 30 min semanal (12 × R$ 132,00): total R$ 3.588,00, parcelado R$ 2.920,00
        assertThat(preview.decimal("$.totalValue")).isEqualByComparingTo("3588.00");
        assertThat(preview.decimal("$.paidSoFar")).isEqualByComparingTo("668.00");
        assertThat(preview.decimal("$.installmentBase")).isEqualByComparingTo("2920.00");
        assertThat(preview.<String>read("$.suggestedFirstDueDate")).isNotNull();
        int max = preview.read("$.maxInstallments");

        scenario.amend(original, "PIX", 1, scenario.today.minusDays(1).toString(), items)
                .expectError(400, "No aditivo, o 1º vencimento deve ser a partir da data do aditivo.");
        scenario.amend(original, "PIX", max + 1, null, items)
                .expectError(400, "O número de parcelas deve ser de 1 a " + max + " (meses do contrato).");
        ApiResponse amended = scenario.amend(original, "PIX", max, null, items).expect(201);

        ApiResponse previous = scenario.api.get("/api/contracts/" + original).expect(200);
        assertThat(previous.<String>read("$.deactivatedOn")).isEqualTo(amended.read("$.createdAt"));
        assertThat(previous.<Boolean>read("$.superseded")).isTrue();
        assertThat(previous.<String>read("$.status")).isEqualTo("SUPERSEDED");
        assertThat(amended.decimal("$.installmentBase")).isEqualByComparingTo("2920.00");
    }

    @Test
    @DisplayName("Aditivo só mantendo o dia e as parcelas que faltam → 400 sem alteração; para 1 parcela → 201")
    void amendmentWithoutChanges() {
        Long patient = scenario.patient("Davi Aditivo");
        ApiResponse contract = scenario.contract(scenario.contractBody(patient, scenario.today.minusDays(20)));
        scenario.pay(patient, contract.id(), "668.00");
        List<String> dueDates = contract.read("$.installmentSchedule[*].dueDate");
        List<String> remaining = dueDates.stream().filter(date -> date.compareTo(scenario.today.toString()) >= 0).toList();

        scenario.amend(contract.id(), "PIX", remaining.size(), remaining.getFirst(), List.of(scenario.rpgItem()))
                .expectError(400, "Nenhuma alteração no contrato.");
        scenario.amend(contract.id(), "PIX", 1, remaining.getFirst(), List.of(scenario.rpgItem())).expect(201);
    }

    @Test
    @DisplayName("Cancelar: 204, o status cancelado e a data; de novo → 409")
    void cancel() {
        Long contract = scenario.contract(scenario.patient("Eva"), 10);

        scenario.api.delete("/api/contracts/" + contract).expect(204);
        ApiResponse canceled = scenario.api.get("/api/contracts/" + contract).expect(200);

        assertThat(canceled.<String>read("$.status")).isEqualTo("CANCELED");
        assertThat(canceled.<String>read("$.deactivatedOn")).isEqualTo(scenario.today.toString());
        scenario.api.delete("/api/contracts/" + contract).expect(409);
        assertThat(scenario.api.get("/api/contracts").expect(200)
                .<List<String>>read("$[?(@.id == " + contract + ")].status")).containsExactly("CANCELED");
    }
}
