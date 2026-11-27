package br.com.senai.sublime_app.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Collator;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import br.com.senai.sublime_app.ApiScenario;
import br.com.senai.sublime_app.ApiTestClient.ApiResponse;
import br.com.senai.sublime_app.IntegrationTest;

// A conta pela API: a conta do contrato (o consumido, o pago, o saldo, os itens, os avisos e as
// sugestões), o Grupo, o avulso, as abas Parcelas e Sessões e a lista do Financeiro. As variações
// das contas estão nos testes de unidade (ContractAccountTest e os outros do billing.domain); aqui,
// o JSON de cada endpoint, com os dados criados pela API. O contrato padrão do cenário: Evolução,
// RPG 60 min semanal = 12 × R$ 167,00 = R$ 2.004,00 em 3 parcelas de R$ 668,00. Ver "Payment" no
// domain-model
@IntegrationTest
class BillingAccountApiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private ApiScenario scenario;
    private LocalDate start;

    @BeforeEach
    void setUp() {
        scenario = new ApiScenario(context);
        // 1º vencimento (a 1ª parcela) há 20 dias; a 2ª ainda não venceu
        start = scenario.today.minusDays(20);
    }

    @Test
    @DisplayName("Conta do contrato: 6 sessões e R$ 668,00 pagos → consumido R$ 1.002,00, saldo − R$ 334,00, devendo")
    void contractAccount() {
        Long ana = scenario.patient("Ana Conta");
        Long contract = scenario.contract(ana, 20);
        scenario.launchMany(ana, contract, "RPG", start, 6);
        scenario.pay(ana, contract, "668.00");

        ApiResponse account = scenario.account(ana);

        assertThat(account.decimal("$.contracts[0].consumed")).isEqualByComparingTo("1002.00");
        assertThat(account.decimal("$.contracts[0].paid")).isEqualByComparingTo("668.00");
        assertThat(account.<Integer>read("$.contracts[0].paidPercentage")).isEqualTo(33);
        assertThat(account.decimal("$.contracts[0].balance")).isEqualByComparingTo("-334.00");
        assertThat(account.<String>read("$.contracts[0].status")).isEqualTo("OWING");
        assertThat(account.<String>read("$.contracts[0].contractStatus")).isEqualTo("CURRENT");
        assertThat(account.<Integer>read("$.contracts[0].sessionsUsed")).isEqualTo(6);
        assertThat(account.<Integer>read("$.contracts[0].installments.paidCount")).isEqualTo(1);
        assertThat(account.<Integer>read("$.contracts[0].installments.totalCount")).isEqualTo(3);
        assertThat(account.<String>read("$.contracts[0].items[0].techniqueName")).isEqualTo("RPG");
        assertThat(account.<Integer>read("$.contracts[0].items[0].durationMinutes")).isEqualTo(60);
        assertThat(account.<String>read("$.contracts[0].items[0].frequency")).isEqualTo("ONCE_A_WEEK");
        assertThat(account.<Integer>read("$.contracts[0].items[0].used")).isEqualTo(6);
        assertThat(account.<Integer>read("$.contracts[0].items[0].contracted")).isEqualTo(12);
        assertThat(account.<Object>read("$.singleSession")).isNull();
    }

    @Test
    @DisplayName("Falta e desmarcação fora do prazo somam; o inativado sai; o beneficiário soma na conta do titular")
    void chargedStatusesAndBeneficiary() {
        Long ana = scenario.patient("Ana Titular");
        Long joao = scenario.patient("João Beneficiário");
        Map<String, Object> request = scenario.contractBody(ana, start);
        request.put("beneficiaryId", joao);
        Long contract = scenario.contract(request).id();
        scenario.launch(joao, contract, "RPG", start.plusDays(1), "ATTENDED");
        scenario.launch(ana, contract, "RPG", start.plusDays(2), "NO_SHOW");
        scenario.launch(ana, contract, "RPG", start.plusDays(3), "CANCELED_LATE");
        Long inactivated = scenario.launch(ana, contract, "RPG", start.plusDays(4), "ATTENDED");
        scenario.api.delete("/api/consultations/" + inactivated).expect(204);

        ApiResponse holder = scenario.account(ana);
        ApiResponse beneficiary = scenario.account(joao);
        ApiResponse sessions = scenario.api.get("/api/billing/contracts/" + contract + "/sessions").expect(200);

        // 3 × R$ 167,00
        assertThat(holder.decimal("$.contracts[0].consumed")).isEqualByComparingTo("501.00");
        assertThat(beneficiary.<List<Object>>read("$.contracts")).isEmpty();
        assertThat(beneficiary.<Object>read("$.singleSession")).isNull();
        assertThat(sessions.<Boolean>read("$.hasBeneficiary")).isTrue();
        assertThat(sessions.<List<String>>read("$.sessions[*].patientName"))
                .containsExactly("João Beneficiário", "Ana Titular", "Ana Titular");
    }

    @Test
    @DisplayName("Além do contratado e de-para: 14 RPG + 1 Quiropraxia → R$ 334,00 a mais, o aviso, a 13ª além e o de-para sem número")
    void beyondContractAndEquivalentPlan() {
        Long ana = scenario.patient("Ana Excesso");
        Long contract = scenario.contract(ana, 20);
        scenario.launchMany(ana, contract, "RPG", start, 14);
        scenario.launch(ana, contract, "Quiropraxia", start.plusDays(15));

        ApiResponse account = scenario.account(ana);
        ApiResponse sessions = scenario.api.get("/api/billing/contracts/" + contract + "/sessions").expect(200);

        assertThat(account.decimal("$.contracts[0].consumed")).isEqualByComparingTo("2470.00");
        assertThat(account.<Integer>read("$.contracts[0].items[0].extraCount")).isEqualTo(2);
        assertThat(account.decimal("$.contracts[0].items[0].extraValue")).isEqualByComparingTo("334.00");
        assertThat(account.<List<String>>read("$.contracts[0].warnings[*].type")).contains("BEYOND_CONTRACT");
        assertThat(sessions.<Integer>read("$.sessions[12].number")).isEqualTo(13);
        assertThat(sessions.<Integer>read("$.sessions[12].contractedCount")).isEqualTo(12);
        assertThat(sessions.<String>read("$.sessions[12].tag")).isEqualTo("BEYOND_CONTRACT");
        assertThat(sessions.<Object>read("$.sessions[14].number")).isNull();
        assertThat(sessions.<String>read("$.sessions[14].tag")).isEqualTo("OUT_OF_CONTRACT");
    }

    @Test
    @DisplayName("Grupo: Trimestral 2x com 10 aulas → duas mensalidades vencidas (R$ 1.140,71), as aulas não somam, 16 aulas restantes")
    void groupAccount() {
        Long bia = scenario.patient("Bia Grupo");
        LocalDate groupStart = scenario.today.minusDays(35);
        Map<String, Object> request = scenario.contractBody(bia, groupStart);
        request.put("planId", scenario.planId("Trimestral"));
        request.put("items", List.of(scenario.groupItem()));
        Long contract = scenario.contract(request).id();
        scenario.launchMany(bia, contract, "Pilates em Grupo", groupStart, 10);

        ApiResponse account = scenario.account(bia);
        ApiResponse balance = scenario.api.get("/api/consultations/session-balance?contractId=" + contract).expect(200);

        assertThat(account.decimal("$.contracts[0].groupFeesDue")).isEqualByComparingTo("1140.71");
        assertThat(account.decimal("$.contracts[0].consumed")).isEqualByComparingTo("0.00");
        assertThat(account.<Integer>read("$.contracts[0].items[0].weeklyFrequency")).isEqualTo(2);
        assertThat(account.<Object>read("$.contracts[0].items[0].usedValue")).isNull();
        assertThat(balance.<Boolean>read("$[0].groupItem")).isTrue();
        assertThat(balance.<Integer>read("$[0].remainingSessions")).isEqualTo(16);
    }

    @Test
    @DisplayName("Avulso: R$ 220,00 com a sugestão do débito; pago, saldo zero. Paciente sem nada: conta vazia")
    void singleSessionAndEmpty() {
        Long diego = scenario.patient("Diego Avulso");
        scenario.launch(diego, null, "RPG", scenario.today.minusDays(2));

        ApiResponse open = scenario.account(diego);
        scenario.pay(diego, null, "220.00");
        ApiResponse settled = scenario.account(diego);
        ApiResponse empty = scenario.account(scenario.patient("Rafael Sem Conta"));

        assertThat(open.decimal("$.singleSession.due")).isEqualByComparingTo("220.00");
        assertThat(open.<String>read("$.singleSession.paymentSuggestions[0].type")).isEqualTo("SINGLE_SESSION_DUE");
        assertThat(open.decimal("$.singleSession.paymentSuggestions[0].amount")).isEqualByComparingTo("220.00");
        assertThat(settled.decimal("$.singleSession.balance")).isEqualByComparingTo("0.00");
        assertThat(empty.<List<Object>>read("$.contracts")).isEmpty();
        assertThat(empty.<Object>read("$.singleSession")).isNull();
    }

    @Test
    @DisplayName("Sugestões e parcelas: R$ 556,66 da 1ª (vencida) → parcela em aberto R$ 111,34 (com a data), todas R$ 1.447,34")
    void suggestionsAndInstallments() {
        Long ana = scenario.patient("Ana Sugestões");
        Long contract = scenario.contract(ana, 20);
        scenario.pay(ana, contract, "556.66");

        ApiResponse account = scenario.account(ana);
        ApiResponse installments = scenario.api.get("/api/billing/contracts/" + contract + "/installments").expect(200);

        assertThat(account.<String>read("$.contracts[0].paymentSuggestions[0].type")).isEqualTo("OPEN_INSTALLMENT");
        assertThat(account.decimal("$.contracts[0].paymentSuggestions[0].amount")).isEqualByComparingTo("111.34");
        assertThat(account.<String>read("$.contracts[0].paymentSuggestions[0].dueDate")).isEqualTo(start.toString());
        assertThat(account.decimal("$.contracts[0].paymentSuggestions[1].amount")).isEqualByComparingTo("1447.34");
        assertThat(account.<String>read("$.contracts[0].status")).isEqualTo("LATE");
        assertThat(installments.<List<String>>read("$[0].installments[*].status"))
                .containsExactly("OVERDUE", "NEXT", "UPCOMING");
        assertThat(installments.decimal("$[0].installments[0].remaining")).isEqualByComparingTo("111.34");
    }

    @Test
    @DisplayName("Parcelas depois do aditivo (pelo id da versão antiga): a nova primeiro; na original, as não pagas substituídas")
    void installmentsAfterAmendment() {
        Long ana = scenario.patient("Ana Versões");
        Long original = scenario.contract(ana, 20);
        Long payment = scenario.pay(ana, original, "668.00");
        Long amended = scenario.amend(original, "PIX", 1, null,
                List.of(scenario.rpgItem(), scenario.chiropracticItem("ONCE_A_WEEK"))).expect(201).id();

        ApiResponse versions = scenario.api.get("/api/billing/contracts/" + original + "/installments").expect(200);
        // O pagamento de antes do aditivo inativado: as parcelas do aditivo continuam somando R$ 2.920,00
        scenario.api.delete("/api/payments/" + payment).expect(204);
        ApiResponse account = scenario.account(ana);

        assertThat(versions.longAt("$[0].contractId")).isEqualTo(amended);
        assertThat(versions.<Boolean>read("$[0].current")).isTrue();
        assertThat(versions.decimal("$[0].paidBeforeAmendment")).isEqualByComparingTo("668.00");
        assertThat(versions.decimal("$[0].installmentBase")).isEqualByComparingTo("2920.00");
        assertThat(versions.<List<String>>read("$[1].installments[*].status"))
                .containsExactly("PAID", "REPLACED", "REPLACED");
        // Uma parcela só: "a parcela em aberto" e "todas" têm o mesmo valor, e a sugestão não se repete
        assertThat(account.<List<String>>read("$.contracts[0].paymentSuggestions[*].type"))
                .containsExactly("OPEN_INSTALLMENT");
        assertThat(account.decimal("$.contracts[0].paymentSuggestions[0].amount")).isEqualByComparingTo("2920.00");
    }

    @Test
    @DisplayName("Sessões: numeradas pela data (lançadas fora de ordem); inativar a 1ª renumera as seguintes")
    void sessionNumbering() {
        Long ana = scenario.patient("Ana Sessões");
        Long contract = scenario.contract(ana, 25);
        Long at10 = scenario.launch(ana, contract, "RPG", scenario.today.minusDays(10));
        Long at20 = scenario.launch(ana, contract, "RPG", scenario.today.minusDays(20));
        Long at15 = scenario.launch(ana, contract, "RPG", scenario.today.minusDays(15));
        String path = "/api/billing/contracts/" + contract + "/sessions";

        ApiResponse before = scenario.api.get(path).expect(200);
        scenario.api.delete("/api/consultations/" + at20).expect(204);
        ApiResponse after = scenario.api.get(path).expect(200);

        assertThat(before.<List<Integer>>read("$.sessions[*].consultationId"))
                .containsExactly(at20.intValue(), at15.intValue(), at10.intValue());
        assertThat(before.<List<Integer>>read("$.sessions[*].number")).containsExactly(1, 2, 3);
        assertThat(after.<List<Integer>>read("$.sessions[*].consultationId"))
                .containsExactly(at15.intValue(), at10.intValue());
        assertThat(after.<List<Integer>>read("$.sessions[*].number")).containsExactly(1, 2);
    }

    @Test
    @DisplayName("Lista do Financeiro: quem entra, a situação (atrasado com crédito, devendo, em dia), a soma das contas e a ordem")
    void accountList() {
        Long late = scenario.patient("Rafael Atrasado");
        scenario.pay(late, scenario.contract(late, 20), "300.00");
        Long owing = scenario.patient("Carla Devendo");
        Long owingContract = scenario.contract(owing, 20);
        scenario.launchMany(owing, owingContract, "RPG", start, 6);
        scenario.pay(owing, owingContract, "668.00");
        Long upToDate = scenario.patient("Bruno Em Dia");
        Long upToDateContract = scenario.contract(upToDate, 20);
        scenario.launchMany(upToDate, upToDateContract, "RPG", start, 4);
        scenario.pay(upToDate, upToDateContract, "668.00");
        // Contrato com crédito (R$ 334,00) e um avulso em aberto (R$ 220,00), antes do contrato
        Long mixed = scenario.patient("Gabi Mista");
        scenario.launch(mixed, null, "RPG", scenario.today.minusDays(40));
        Long mixedContract = scenario.contract(mixed, 20);
        scenario.pay(mixed, mixedContract, "668.00");
        scenario.launchMany(mixed, mixedContract, "RPG", start, 2);
        Long nothing = scenario.patient("Sem Nada");

        ApiResponse list = scenario.api.get("/api/billing/accounts").expect(200);

        assertThat(row(list, late, "status")).isEqualTo("LATE");
        assertThat(row(list, late, "hasOverdue")).isEqualTo(true);
        assertThat(row(list, late, "balance").toString()).isEqualTo("300.0");
        assertThat(row(list, owing, "status")).isEqualTo("OWING");
        assertThat(row(list, upToDate, "status")).isEqualTo("UP_TO_DATE");
        assertThat(row(list, mixed, "status")).isEqualTo("OWING");
        assertThat(row(list, mixed, "balance").toString()).isEqualTo("114.0");
        assertThat(list.<List<Object>>read("$[?(@.patientId == " + nothing + ")]")).isEmpty();
        assertListOrder(list);
    }

    // Um campo da linha do paciente na lista
    private static Object row(ApiResponse list, Long patientId, String field) {
        List<Object> values = list.read("$[?(@.patientId == " + patientId + ")]." + field);
        assertThat(values).as("linha do paciente %d", patientId).hasSize(1);
        return values.getFirst();
    }

    // A ordem da lista inteira (com os pacientes dos outros testes): os atrasados pela vencida mais
    // antiga, os devendo pelo maior saldo devedor, os outros por nome (palavra a palavra)
    private static void assertListOrder(ApiResponse list) {
        List<Map<String, Object>> rows = list.read("$[*]");
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        for (int index = 1; index < rows.size(); index++) {
            Map<String, Object> previous = rows.get(index - 1);
            Map<String, Object> current = rows.get(index);
            int previousRank = rank(previous);
            int currentRank = rank(current);
            assertThat(previousRank).as("ordem dos grupos em %s e %s", previous, current).isLessThanOrEqualTo(currentRank);
            if (previousRank != currentRank) {
                continue;
            }
            if (currentRank == 0) {
                assertThat((String) previous.get("oldestOverdueDate")).isLessThanOrEqualTo((String) current.get("oldestOverdueDate"));
            } else if (currentRank == 1) {
                assertThat(Double.parseDouble(previous.get("balance").toString()))
                        .isLessThanOrEqualTo(Double.parseDouble(current.get("balance").toString()));
            } else {
                assertThat(compareByWords(collator, (String) previous.get("patientName"),
                        (String) current.get("patientName"))).as("%s antes de %s", previous.get("patientName"),
                                current.get("patientName")).isLessThanOrEqualTo(0);
            }
        }
    }

    private static int rank(Map<String, Object> row) {
        return switch ((String) row.get("status")) {
            case "LATE" -> 0;
            case "OWING" -> 1;
            default -> 2;
        };
    }

    private static int compareByWords(Collator collator, String first, String second) {
        String[] firstWords = first.trim().split("\\s+");
        String[] secondWords = second.trim().split("\\s+");
        for (int index = 0; index < Math.min(firstWords.length, secondWords.length); index++) {
            int result = collator.compare(firstWords[index], secondWords[index]);
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(firstWords.length, secondWords.length);
    }
}
