package br.com.senai.sublime_app.provider.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Liberação de lançamentos retroativos para um prestador, dada pelo admin. O prestador
// só lança com a data de hoje; com a liberação, lança em qualquer data passada, uma
// unidade por lançamento. Vale no dia em que foi dada (até 23:59) e até a quantidade
// acabar. Liberar de novo cria outra (vale a mais recente do dia). Ver "Quem lança" em
// Consultation, no domain-model.
@Entity
@Table(name = "retroactive_launch_grant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RetroactiveLaunchGrantEntity {

    public static final int MAX_ALLOWED_COUNT = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    // O admin que liberou
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by_user_id", nullable = false)
    private UserEntity grantedBy;

    // O dia em que a liberação vale (o dia em que foi dada)
    @Column(name = "granted_on", nullable = false)
    private LocalDate grantedOn;

    @Column(name = "allowed_count", nullable = false)
    private int allowedCount;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    public RetroactiveLaunchGrantEntity(ProviderEntity provider, UserEntity grantedBy, LocalDate grantedOn,
            int allowedCount) {
        validateActiveProvider(provider);
        validateAllowedCount(allowedCount);
        this.provider = provider;
        this.grantedBy = grantedBy;
        this.grantedOn = grantedOn;
        this.allowedCount = allowedCount;
        this.usedCount = 0;
    }

    // Vale na data e ainda tem unidade
    public boolean isUsableOn(LocalDate date) {
        return grantedOn.equals(date) && getRemaining() > 0;
    }

    public int getRemaining() {
        return allowedCount - usedCount;
    }

    // Um lançamento retroativo usa uma unidade (desfazer o lançamento não a devolve)
    public void consume() {
        requireRemaining();
        this.usedCount++;
    }

    private void requireRemaining() {
        if (getRemaining() <= 0) {
            throw new BusinessRuleException("A liberação de lançamentos retroativos já foi usada.");
        }
    }

    // Prestador inativo não lança: liberar para ele não teria uso
    private static void validateActiveProvider(ProviderEntity provider) {
        if (!provider.isActive()) {
            throw new BusinessRuleException("O prestador está inativo.");
        }
    }

    private static void validateAllowedCount(int allowedCount) {
        if (allowedCount < 1 || allowedCount > MAX_ALLOWED_COUNT) {
            throw new BusinessRuleException(
                    "A quantidade de lançamentos retroativos deve ser de 1 a " + MAX_ALLOWED_COUNT + ".");
        }
    }
}
