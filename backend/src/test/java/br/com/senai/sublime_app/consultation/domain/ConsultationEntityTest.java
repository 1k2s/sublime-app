package br.com.senai.sublime_app.consultation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;
import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;
import br.com.senai.sublime_app.pricing.enums.PricingModel;
import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// Regras da ConsultationEntity (ver a seção Consultation no domain-model). Teste de unidade puro:
// sem Spring e sem banco. As datas chegam prontas, como o service as passa (nunca o now()).
// O ReflectionTestUtils só preenche o id do grupo de preço, que a regra do preço compara.
class ConsultationEntityTest {

    private static final LocalDate OCCURRED_AT = LocalDate.of(2026, 11, 20);
    private static final int DURATION_MINUTES = 30;

    @Nested
    @DisplayName("Data do lançamento")
    class LaunchedAt {

        @Test
        @DisplayName("Guarda a data e a hora do lançamento recebidas do service")
        void keepsInformedLaunchedAt() {
            LocalDateTime launchedAt = LocalDateTime.of(2026, 11, 24, 10, 42, 15);

            ConsultationEntity consultation = newConsultation(launchedAt);

            assertThat(consultation.getLaunchedAt()).isEqualTo(launchedAt);
        }

        @Test
        @DisplayName("Lançamento retroativo: a data do lançamento é a do dia em que foi lançado, não a do atendimento")
        void keepsLaunchedAtApartFromOccurredAt() {
            LocalDateTime launchedAt = LocalDateTime.of(2026, 11, 24, 10, 42, 15);

            ConsultationEntity consultation = newConsultation(launchedAt);

            assertThat(consultation.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(consultation.getLaunchedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 24));
        }

        @Test
        @DisplayName("Recusa lançamento sem a data do lançamento")
        void rejectsMissingLaunchedAt() {
            assertThatThrownBy(() -> newConsultation(null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A data do lançamento é obrigatória.");
        }
    }

    // Um Avulso válido (paciente sem contrato, preço por duração), mudando só a data do lançamento
    private static ConsultationEntity newConsultation(LocalDateTime launchedAt) {
        PricingGroupEntity pricingGroup = new PricingGroupEntity("Pilates em Dupla", PricingModel.DURATION_BASED);
        ReflectionTestUtils.setField(pricingGroup, "id", 1L);
        TechniqueEntity technique = new TechniqueEntity("Pilates em Dupla", pricingGroup);
        PlanEntity singleSession = new PlanEntity("Avulso", PlanType.SINGLE_SESSION, 1, null);
        SessionDurationPriceEntity price = new SessionDurationPriceEntity(pricingGroup, singleSession,
                DURATION_MINUTES, Money.of(new BigDecimal("158.00")), LocalDate.of(2026, 1, 1));
        UserEntity user = new UserEntity("prestador@sublime.test", "hash-qualquer", Role.PROVIDER);
        ProviderEntity provider = new ProviderEntity(user, "Ana Souza", null, null);
        PatientEntity patient = new PatientEntity("Carla Mendes", "11122233344", LocalDate.of(1988, 4, 12),
                null, null, null, null);

        return new ConsultationEntity(patient, null, provider, user, launchedAt, technique, DURATION_MINUTES,
                OCCURRED_AT, ConsultationStatus.ATTENDED, price, null);
    }
}
