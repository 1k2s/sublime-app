package br.com.senai.sublime_app.provider.domain;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

   
    @OneToOne(fetch = FetchType.LAZY, optional = false) // cardinalidade 1:1 (lazy para pegar somente o usuario do bando) optional false para não permitir nulo
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 100, nullable = false)
    private String name;

    // Convertido para DECIMAL pelo PercentageConverter (autoApply)
    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private Percentage commissionPercentage;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um provider com os dados obrigatórios
    public ProviderEntity(UserEntity user, String name, Percentage commissionPercentage) {
        validateActiveUser(user);
        validateName(name);
        validateCommission(commissionPercentage);
        this.user = user;
        this.name = name;
        this.commissionPercentage = commissionPercentage;
        this.active = true;
    }

    //update publico para atualizar os dados do provider
    public void update(String name, Percentage commissionPercentage) {
        validateName(name);
        validateCommission(commissionPercentage);
        this.name = name;
        this.commissionPercentage = commissionPercentage;
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

    // A comissão entra no cálculo do repasse de cada atendimento. O intervalo 0–100 e
    // as 2 casas são garantidos pelo próprio Percentage; aqui só a obrigatoriedade.
    private static void validateCommission(Percentage commissionPercentage) {
        if (commissionPercentage == null) {
            throw new BusinessRuleException("O percentual de comissão é obrigatório.");
        }
    }
}
