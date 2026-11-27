package br.com.senai.sublime_app.billing.domain;

import static br.com.senai.sublime_app.billing.domain.BillingTestData.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.senai.sublime_app.billing.domain.ContractSessions.Session;
import br.com.senai.sublime_app.billing.domain.ContractSessions.Tag;
import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;

// A aba Sessões: a numeração "7ª de 6" e as etiquetas. O cenário está no BillingTestData (C-Ana:
// RPG 60 min semanal, 12 sessões, de 02/11/2026). Os atendimentos são de datas já passadas, porque
// o atendimento não aceita data futura.
class ContractSessionsTest {

    private BillingTestData data;

    @BeforeEach
    void setUp() {
        data = new BillingTestData();
    }

    @Test
    @DisplayName("Lançadas fora de ordem: numeradas pela data e, no mesmo dia, pelo lançamento")
    void numberedByDateThenLaunch() {
        ContractEntity contract = data.anaContract();
        ConsultationEntity day20 = rpg(contract, 20);
        ConsultationEntity day10 = rpg(contract, 10);
        ConsultationEntity day15First = rpg(contract, 15);
        ConsultationEntity day15Second = rpg(contract, 15);

        ContractSessions sessions = ContractSessions.of(contract, List.of(day20, day15Second, day10, day15First));

        assertThat(sessions.sessions()).extracting(Session::consultation, Session::number, Session::contractedCount)
                .containsExactly(
                        tuple(day10, 1, 12),
                        tuple(day15First, 2, 12),
                        tuple(day15Second, 3, 12),
                        tuple(day20, 4, 12));
        assertThat(sessions.sessions()).extracting(Session::tag).containsOnlyNulls();
    }

    @Test
    @DisplayName("A 13ª sessão de RPG (12 contratadas): além do contratado")
    void beyondContract() {
        ContractEntity contract = data.anaContract();
        List<ConsultationEntity> consultations = new ArrayList<>();
        for (int day = 3; day <= 15; day++) {
            consultations.add(rpg(contract, day));
        }

        Session last = ContractSessions.of(contract, consultations).sessions().getLast();

        assertThat(last.number()).isEqualTo(13);
        assertThat(last.contractedCount()).isEqualTo(12);
        assertThat(last.tag()).isEqualTo(Tag.BEYOND_CONTRACT);
    }

    @Test
    @DisplayName("Grupo: a aula acima das contratadas fica além das aulas previstas")
    void beyondClasses() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        ContractEntity contract = ContractEntity.create(data.ana, null, data.quarterly, start, PaymentMethod.PIX, 3,
                List.of(data.groupDraft()), null, start);
        List<ConsultationEntity> classes = new ArrayList<>();
        for (int index = 0; index < 27; index++) {
            classes.add(data.consultation(contract, data.ana, data.groupPilates, start.plusDays(index)));
        }

        List<Session> sessions = ContractSessions.of(contract, classes).sessions();

        assertThat(sessions.get(25).tag()).isNull();
        assertThat(sessions.getLast()).extracting(Session::number, Session::contractedCount, Session::tag)
                .containsExactly(27, 26, Tag.BEYOND_CLASSES);
    }

    @Test
    @DisplayName("O de-para: sem número, fora do contrato; não conta na numeração do item")
    void equivalentPlanOutOfContract() {
        ContractEntity contract = data.anaContract();
        ConsultationEntity first = rpg(contract, 3);
        ConsultationEntity chiropractic = data.consultation(contract, data.ana, data.chiropractic, day(4));
        ConsultationEntity second = rpg(contract, 5);

        List<Session> sessions = ContractSessions.of(contract, List.of(first, chiropractic, second)).sessions();

        assertThat(sessions).extracting(Session::number, Session::tag)
                .containsExactly(tuple(1, null), tuple(null, Tag.OUT_OF_CONTRACT), tuple(2, null));
    }

    @Test
    @DisplayName("Item removido por aditivo: as sessões continuam, numeradas, com a etiqueta")
    void removedItem() {
        ContractEntity original = data.contract(List.of(data.rpgDraft(), data.chiropracticDraft()));
        ConsultationEntity chiropractic = data.consultation(original, data.ana, data.chiropractic, day(3));
        ConsultationEntity rpg = rpg(original, 4);
        ContractEntity amended = data.amend(original, 2, List.of(data.rpgDraft()), "0.00", day(20));

        List<Session> sessions = ContractSessions.of(amended, List.of(chiropractic, rpg)).sessions();

        assertThat(sessions).extracting(Session::consultation, Session::number, Session::contractedCount,
                Session::tag)
                .containsExactly(tuple(chiropractic, 1, null, Tag.REMOVED_ITEM), tuple(rpg, 1, 12, null));
    }

    @Test
    @DisplayName("Inativar a 1ª sessão: ela sai e as seguintes viram 1ª e 2ª")
    void inactiveRenumbers() {
        ContractEntity contract = data.anaContract();
        ConsultationEntity first = rpg(contract, 3);
        ConsultationEntity second = rpg(contract, 4);
        ConsultationEntity third = rpg(contract, 5);
        first.deactivate();

        List<Session> sessions = ContractSessions.of(contract, List.of(first, second, third)).sessions();

        assertThat(sessions).extracting(Session::consultation, Session::number)
                .containsExactly(tuple(second, 1), tuple(third, 2));
    }

    @Test
    @DisplayName("Com beneficiário: a sessão diz quem foi atendido; a numeração é do contrato")
    void beneficiary() {
        ContractEntity contract = BillingTestData.withId(ContractEntity.create(data.ana, data.bia, data.evolution,
                START, PaymentMethod.PIX, 3, List.of(data.rpgDraft()), null, START), 600L);
        ConsultationEntity holder = rpg(contract, 3);
        ConsultationEntity beneficiary = data.consultation(contract, data.bia, data.rpg, day(4));

        ContractSessions sessions = ContractSessions.of(contract, List.of(holder, beneficiary));

        assertThat(sessions.hasBeneficiary()).isTrue();
        assertThat(sessions.sessions()).extracting(session -> session.consultation().getPatient().getName(),
                Session::number)
                .containsExactly(tuple("Ana", 1), tuple("Bia", 2));
        assertThat(ContractSessions.of(data.anaContract(), List.of()).hasBeneficiary()).isFalse();
    }

    private ConsultationEntity rpg(ContractEntity contract, int dayOfNovember) {
        return data.consultation(contract, data.ana, data.rpg, day(dayOfNovember));
    }

    private static LocalDate day(int dayOfNovember) {
        return LocalDate.of(2026, 11, dayOfNovember);
    }
}
