package br.com.senai.sublime_app.provider.domain;

import java.math.BigDecimal;
import java.util.Objects;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "provider")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderEntity {

    // Padrão da clínica, aplicado no cadastro quando o percentual não é informado.
    // Prestadores já cadastrados mantêm o que foi combinado com eles (mudar o padrão
    // não os altera). Ver domain-model.md, seção Provider.
    public static final Percentage DEFAULT_COMMISSION_PERCENTAGE = Percentage.of(new BigDecimal("40"));
    public static final Percentage DEFAULT_REFERRAL_COMMISSION_PERCENTAGE = Percentage.of(new BigDecimal("45"));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // 1:1 (lazy para carregar o usuário só quando necessário); optional false: não permite nulo
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 100, nullable = false)
    private String name;

    // Percentual padrão: pacientes da clínica ou trazidos por outro prestador.
    // Convertido para DECIMAL pelo PercentageConverter (autoApply).
    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private Percentage commissionPercentage;

    // Percentual dos pacientes que ESTE prestador trouxe (titular do contrato com
    // referringProvider = ele). Nunca menor que o padrão.
    @Column(name = "referral_commission_percentage", nullable = false, precision = 5, scale = 2)
    private Percentage referralCommissionPercentage;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um provider. Percentual não informado recebe o
    // padrão da clínica (exceção caso a caso — ex: estagiário — é informar o valor).
    public ProviderEntity(UserEntity user, String name, Percentage commissionPercentage,
            Percentage referralCommissionPercentage) {
        Percentage commission = Objects.requireNonNullElse(commissionPercentage, DEFAULT_COMMISSION_PERCENTAGE);
        Percentage referral = Objects.requireNonNullElse(referralCommissionPercentage, DEFAULT_REFERRAL_COMMISSION_PERCENTAGE);
        validateActiveUser(user);
        validateName(name);
        validateReferralNotBelowCommission(commission, referral);
        this.user = user;
        this.name = name;
        this.commissionPercentage = commission;
        this.referralCommissionPercentage = referral;
        this.active = true;
    }

    // update publico: na alteração os dois percentuais são obrigatórios — o padrão só
    // vale no cadastro (senão um campo esquecido devolveria um estagiário ao padrão)
    public void update(String name, Percentage commissionPercentage, Percentage referralCommissionPercentage) {
        validateName(name);
        validateCommissionsInformed(commissionPercentage, referralCommissionPercentage);
        validateReferralNotBelowCommission(commissionPercentage, referralCommissionPercentage);
        this.name = name;
        this.commissionPercentage = commissionPercentage;
        this.referralCommissionPercentage = referralCommissionPercentage;
    }

    public void deactivate() {
        requireActive();
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    // Inativar duas vezes não é uma operação válida: 409 avisa que o estado já era esse
    private void requireActive() {
        if (!active) {
            throw new ConflictException("O prestador já está inativo.");
        }
    }

    // Todo prestador se autentica por um User; o vínculo é fixo (update não o recebe).
    // Usuário inativo (soft delete) existe, mas não pode ENTRAR num prestador novo —
    // mesma lógica do contrato com paciente/técnica inativos. O inverso é permitido:
    // inativar o usuário de um prestador ativo é como o administrador remove o acesso.
    private static void validateActiveUser(UserEntity user) {
        if (user == null) {
            throw new BusinessRuleException("O prestador deve estar vinculado a um usuário.");
        }
        if (!user.isActive()) {
            throw new BusinessRuleException("O usuário está inativo.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do prestador é obrigatório.");
        }
    }

    // O intervalo 0–100 e as 2 casas são garantidos pelo próprio Percentage; aqui só
    // a obrigatoriedade (na alteração, que não aplica o padrão)
    private static void validateCommissionsInformed(Percentage commission, Percentage referral) {
        if (commission == null || referral == null) {
            throw new BusinessRuleException("Os dois percentuais de comissão são obrigatórios na alteração do prestador.");
        }
    }

    // Quem trouxe o paciente nunca recebe menos que o padrão dele
    private static void validateReferralNotBelowCommission(Percentage commission, Percentage referral) {
        if (referral.isLessThan(commission)) {
            throw new BusinessRuleException(
                    "O percentual de pacientes trazidos pelo prestador não pode ser menor que o percentual padrão.");
        }
    }
}
