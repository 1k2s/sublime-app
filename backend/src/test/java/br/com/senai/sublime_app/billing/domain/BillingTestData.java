package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemDraft;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// O cenário dos testes de domínio do billing (os valores do seed):
// - Evolução (pacote, 12 sessões): RPG 60 min = R$ 167,00; Quiropraxia 30 min = R$ 132,00;
// - Trimestral 2x (mensalidade, 13 semanas): Pilates em Grupo, R$ 65,81 a aula;
// - C-Ana: Evolução, RPG 60 min semanal = 12 × R$ 167,00 = R$ 2.004,00, de 02/11/2026, em 3
//   parcelas de R$ 668,00 (02/11, 02/12, 02/01).
// Uma instância nova por teste.
final class BillingTestData {

    static final LocalDate START = LocalDate.of(2026, 11, 2);
    static final Long STANDARD_GROUP_ID = 10L;
    static final Long SPECIALIZED_GROUP_ID = 11L;
    static final Long GROUP_GROUP_ID = 12L;

    private final AtomicLong ids = new AtomicLong(1000);

    final PlanEntity evolution;
    final PlanEntity quarterly;
    final TechniqueEntity rpg;
    final TechniqueEntity chiropractic;
    final TechniqueEntity groupPilates;
    final SessionDurationPriceEntity rpgPrice;
    final SessionDurationPriceEntity chiropracticPrice;
    final SessionFrequencyPriceEntity groupPrice;
    final PatientEntity ana;
    // a beneficiária (filha da Ana)
    final PatientEntity bia;
    final UserEntity admin;
    final ProviderEntity provider;

    BillingTestData() {
        evolution = withId(new PlanEntity("Evolução", PlanType.PACKAGE, 12, null), 1L);
        quarterly = withId(new PlanEntity("Trimestral", PlanType.MONTHLY_FEE, null, 13), 2L);
        quarterly.defineEquivalent(evolution);
        PricingGroupEntity standard = withId(
                new PricingGroupEntity("Individual Padrão", PricingModel.DURATION_BASED), STANDARD_GROUP_ID);
        PricingGroupEntity specialized = withId(
                new PricingGroupEntity("Individual Especializado", PricingModel.DURATION_BASED), SPECIALIZED_GROUP_ID);
        PricingGroupEntity group = withId(new PricingGroupEntity("Em Grupo", PricingModel.FREQUENCY_BASED),
                GROUP_GROUP_ID);
        rpg = withId(new TechniqueEntity("RPG", standard), 20L);
        groupPilates = withId(new TechniqueEntity("Pilates em Grupo", group), 21L);
        chiropractic = withId(new TechniqueEntity("Quiropraxia", specialized), 22L);
        rpgPrice = withId(new SessionDurationPriceEntity(standard, evolution, 60, money("167.00"), START), 30L);
        groupPrice = withId(new SessionFrequencyPriceEntity(group, quarterly, 2, money("65.81"), START), 31L);
        chiropracticPrice = withId(
                new SessionDurationPriceEntity(specialized, evolution, 30, money("132.00"), START), 32L);
        ana = withId(new PatientEntity("Ana", "11111111111", LocalDate.of(1990, 1, 1), null, null, null, null), 100L);
        bia = withId(new PatientEntity("Bia", "22222222222", LocalDate.of(2015, 1, 1), null, null, null, null), 101L);
        admin = withId(new UserEntity("admin@sublime.test", "hash", Role.ADMIN), 200L);
        provider = withId(new ProviderEntity(new UserEntity("prestador@sublime.test", "hash", Role.PROVIDER),
                "Bruna", null, null), 300L);
    }

    // Um atendimento realizado, lançado pelo admin, com o preço da técnica (RPG 60 min,
    // Quiropraxia 30 min, Pilates em Grupo pela frequência). id = a ordem do lançamento
    ConsultationEntity consultation(ContractEntity contract, PatientEntity patient, TechniqueEntity technique,
            LocalDate occurredAt) {
        ConsultationEntity consultation;
        if (technique == groupPilates) {
            consultation = new ConsultationEntity(patient, contract, provider, admin, technique, 60, occurredAt,
                    ConsultationStatus.ATTENDED, null, groupPrice);
        } else {
            SessionDurationPriceEntity price = technique == rpg ? rpgPrice : chiropracticPrice;
            consultation = new ConsultationEntity(patient, contract, provider, admin, technique,
                    price.getDurationMinutes(), occurredAt, ConsultationStatus.ATTENDED, price, null);
        }
        return withId(consultation, ids.incrementAndGet());
    }

    ContractEntity anaContract() {
        return contract(List.of(rpgDraft()));
    }

    // Evolução de 02/11/2026 em 3 parcelas, com os itens dados
    ContractEntity contract(List<ContractItemDraft> drafts) {
        return withId(ContractEntity.create(ana, null, evolution, START, PaymentMethod.PIX, 3, drafts, null, START),
                ids.incrementAndGet());
    }

    // Aditivo pelo dinheiro, sem mudar o fim nem o 1º vencimento sugerido
    ContractEntity amend(ContractEntity contract, int installments, List<ContractItemDraft> drafts, String paidSoFar,
            LocalDate date) {
        return withId(contract.amend(null, PaymentMethod.CASH, installments, null, drafts, null, money(paidSoFar),
                date), ids.incrementAndGet());
    }

    List<ConsultationConsumption> rpgSessions(int count) {
        List<ConsultationConsumption> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(consumption(PricingSource.CONTRACT_ITEM, "167.00", true, STANDARD_GROUP_ID));
        }
        return rows;
    }

    ConsultationConsumption consumption(PricingSource source, String value, boolean durationBased,
            Long pricingGroupId) {
        return new ConsultationConsumption(ids.incrementAndGet(), 1L, ana.getId(), source, money(value), durationBased,
                pricingGroupId, START);
    }

    ContractItemDraft rpgDraft() {
        return ContractItemDraft.withDurationPrice(rpg, rpgPrice, SessionFrequency.ONCE_A_WEEK);
    }

    ContractItemDraft chiropracticDraft() {
        return ContractItemDraft.withDurationPrice(chiropractic, chiropracticPrice, SessionFrequency.ONCE_A_WEEK);
    }

    ContractItemDraft groupDraft() {
        return ContractItemDraft.withFrequencyPrice(groupPilates, groupPrice);
    }

    static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }

    static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
