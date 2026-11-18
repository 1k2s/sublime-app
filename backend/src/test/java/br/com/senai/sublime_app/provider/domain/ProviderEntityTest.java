package br.com.senai.sublime_app.provider.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

// Regras da ProviderEntity (ver a seção Provider no domain-model). Teste de unidade puro:
// sem Spring e sem banco. O ReflectionTestUtils só preenche o id, que em produção vem do banco.
class ProviderEntityTest {

    private static final String REFERRAL_BELOW_COMMISSION =
            "O percentual de pacientes trazidos pelo prestador não pode ser menor que o percentual padrão.";

    @Nested
    @DisplayName("Cadastro")
    class Creation {

        @Test
        @DisplayName("Percentuais não informados recebem o padrão da clínica (40 e 45)")
        void appliesDefaultPercentagesWhenNotInformed() {
            ProviderEntity provider = new ProviderEntity(activeUser(), "Ana", null, null);

            assertThat(provider.getCommissionPercentage()).isEqualTo(percentage("40"));
            assertThat(provider.getReferralCommissionPercentage()).isEqualTo(percentage("45"));
            assertThat(provider.isActive()).isTrue();
        }

        @Test
        @DisplayName("Percentuais informados são mantidos (ex: estagiário com 30 e 30)")
        void keepsInformedPercentages() {
            ProviderEntity provider = new ProviderEntity(activeUser(), "Estagiário",
                    percentage("30"), percentage("30"));

            assertThat(provider.getCommissionPercentage()).isEqualTo(percentage("30"));
            assertThat(provider.getReferralCommissionPercentage()).isEqualTo(percentage("30"));
        }

        @Test
        @DisplayName("Recusa percentual de indicação menor que o padrão")
        void rejectsReferralBelowCommission() {
            assertThatThrownBy(() -> new ProviderEntity(activeUser(), "Ana", percentage("50"), percentage("45")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage(REFERRAL_BELOW_COMMISSION);
        }

        @Test
        @DisplayName("Recusa a indicação informada abaixo do padrão aplicado (40)")
        void rejectsReferralBelowAppliedDefault() {
            assertThatThrownBy(() -> new ProviderEntity(activeUser(), "Ana", null, percentage("35")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage(REFERRAL_BELOW_COMMISSION);
        }

        @Test
        @DisplayName("Recusa prestador sem usuário")
        void rejectsMissingUser() {
            assertThatThrownBy(() -> new ProviderEntity(null, "Ana", null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O prestador deve estar vinculado a um usuário.");
        }

        @Test
        @DisplayName("Recusa usuário inativo")
        void rejectsInactiveUser() {
            UserEntity user = activeUser();
            user.deactivate();

            assertThatThrownBy(() -> new ProviderEntity(user, "Ana", null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O usuário está inativo.");
        }

        @Test
        @DisplayName("Recusa nome nulo ou em branco")
        void rejectsMissingName() {
            assertThatThrownBy(() -> new ProviderEntity(activeUser(), null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O nome do prestador é obrigatório.");
            assertThatThrownBy(() -> new ProviderEntity(activeUser(), "   ", null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O nome do prestador é obrigatório.");
        }
    }

    @Nested
    @DisplayName("Alteração")
    class Update {

        @Test
        @DisplayName("Altera o nome e os dois percentuais")
        void updatesNameAndPercentages() {
            ProviderEntity provider = newProvider();

            provider.update("Ana Souza", percentage("35"), percentage("50"));

            assertThat(provider.getName()).isEqualTo("Ana Souza");
            assertThat(provider.getCommissionPercentage()).isEqualTo(percentage("35"));
            assertThat(provider.getReferralCommissionPercentage()).isEqualTo(percentage("50"));
        }

        @Test
        @DisplayName("Exige os dois percentuais: o padrão só vale no cadastro")
        void requiresBothPercentages() {
            ProviderEntity provider = newProvider();

            assertThatThrownBy(() -> provider.update("Ana", null, percentage("45")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Os dois percentuais de comissão são obrigatórios na alteração do prestador.");
            assertThatThrownBy(() -> provider.update("Ana", percentage("40"), null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Os dois percentuais de comissão são obrigatórios na alteração do prestador.");
        }

        @Test
        @DisplayName("Recusa percentual de indicação menor que o padrão")
        void rejectsReferralBelowCommission() {
            ProviderEntity provider = newProvider();

            assertThatThrownBy(() -> provider.update("Ana", percentage("45"), percentage("40")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage(REFERRAL_BELOW_COMMISSION);
        }

        @Test
        @DisplayName("Recusa nome em branco")
        void rejectsBlankName() {
            ProviderEntity provider = newProvider();

            assertThatThrownBy(() -> provider.update(" ", percentage("40"), percentage("45")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O nome do prestador é obrigatório.");
        }
    }

    @Nested
    @DisplayName("Percentual pela origem do paciente")
    class CommissionByPatientOrigin {

        @Test
        @DisplayName("Paciente trazido por ele: percentual de indicação")
        void usesReferralWhenProviderIsTheOrigin() {
            ProviderEntity provider = newProviderWithId(1L);

            assertThat(provider.commissionPercentageFor(provider)).isEqualTo(percentage("45"));
        }

        @Test
        @DisplayName("Mesmo prestador carregado em outra instância (ex: proxy): percentual de indicação")
        void usesReferralWhenOriginIsAnotherInstanceWithSameId() {
            ProviderEntity provider = newProviderWithId(1L);
            ProviderEntity sameProviderLoadedAgain = newProviderWithId(1L);

            assertThat(provider.commissionPercentageFor(sameProviderLoadedAgain)).isEqualTo(percentage("45"));
        }

        @Test
        @DisplayName("Paciente trazido por outro prestador: percentual padrão")
        void usesCommissionWhenOriginIsAnotherProvider() {
            ProviderEntity provider = newProviderWithId(1L);
            ProviderEntity otherProvider = newProviderWithId(2L);

            assertThat(provider.commissionPercentageFor(otherProvider)).isEqualTo(percentage("40"));
        }

        @Test
        @DisplayName("Paciente da clínica (sem origem): percentual padrão")
        void usesCommissionWhenPatientIsFromTheClinic() {
            ProviderEntity provider = newProviderWithId(1L);

            assertThat(provider.commissionPercentageFor(null)).isEqualTo(percentage("40"));
        }

        @Test
        @DisplayName("Prestador de origem sem id não quebra: percentual padrão")
        void usesCommissionWhenOriginHasNoId() {
            ProviderEntity provider = newProviderWithId(1L);
            ProviderEntity unsavedProvider = newProvider();

            assertThat(provider.commissionPercentageFor(unsavedProvider)).isEqualTo(percentage("40"));
        }

        @Test
        @DisplayName("Prestador sem id como origem dele mesmo: percentual de indicação")
        void usesReferralWhenUnsavedProviderIsItsOwnOrigin() {
            ProviderEntity unsavedProvider = newProvider();

            assertThat(unsavedProvider.commissionPercentageFor(unsavedProvider)).isEqualTo(percentage("45"));
        }

        @Test
        @DisplayName("Dois prestadores sem id não são tratados como o mesmo")
        void doesNotMatchTwoUnsavedProviders() {
            ProviderEntity provider = newProvider();
            ProviderEntity otherUnsavedProvider = newProvider();

            assertThat(provider.commissionPercentageFor(otherUnsavedProvider)).isEqualTo(percentage("40"));
        }
    }

    @Nested
    @DisplayName("Inativar e reativar")
    class ActivationLifecycle {

        @Test
        @DisplayName("Inativa e reativa o prestador")
        void deactivatesAndActivates() {
            ProviderEntity provider = newProvider();

            provider.deactivate();
            assertThat(provider.isActive()).isFalse();

            provider.activate();
            assertThat(provider.isActive()).isTrue();
        }

        @Test
        @DisplayName("Recusa inativar um prestador já inativo")
        void rejectsDeactivatingTwice() {
            ProviderEntity provider = newProvider();
            provider.deactivate();

            assertThatThrownBy(provider::deactivate)
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("O prestador já está inativo.");
        }

        @Test
        @DisplayName("Recusa reativar um prestador já ativo")
        void rejectsActivatingActiveProvider() {
            ProviderEntity provider = newProvider();

            assertThatThrownBy(provider::activate)
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("O prestador já está ativo.");
        }

        @Test
        @DisplayName("Recusa reativar com o usuário inativo")
        void rejectsActivatingWithInactiveUser() {
            UserEntity user = activeUser();
            ProviderEntity provider = new ProviderEntity(user, "Ana", null, null);
            provider.deactivate();
            user.deactivate();

            assertThatThrownBy(provider::activate)
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("O usuário está inativo.");
            assertThat(provider.isActive()).isFalse();
        }
    }

    private static UserEntity activeUser() {
        return new UserEntity("prestador@sublime.test", "hash-qualquer", Role.PROVIDER);
    }

    private static ProviderEntity newProvider() {
        return new ProviderEntity(activeUser(), "Ana", null, null);
    }

    private static ProviderEntity newProviderWithId(Long id) {
        ProviderEntity provider = newProvider();
        ReflectionTestUtils.setField(provider, "id", id);
        return provider;
    }

    private static Percentage percentage(String value) {
        return Percentage.of(new BigDecimal(value));
    }
}
