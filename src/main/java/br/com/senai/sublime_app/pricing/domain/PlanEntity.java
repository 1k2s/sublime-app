package br.com.senai.sublime_app.pricing.domain;

import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Nullable: os planos do Pilates em Grupo são mensalidade, não pacote de sessões.
    // A obrigatoriedade para preço por duração é validada na linha de preço, que
    // sabe em qual grupo o plano é usado (o plano não sabe).
    @Column(name = "session_count")
    private Integer sessionCount;

    @Column(nullable = false)
    private boolean active = true;

    // Construtor publico para criar um plano com os dados obrigatórios
    public PlanEntity(String name, Integer sessionCount) {
        validateNameAndSessionCount(name, sessionCount);
        this.name = name;
        this.sessionCount = sessionCount;
        this.active = true;
    }

    // Invariantes do plano. sessionCount nulo é válido (plano do Pilates em Grupo);
    // quando informado, precisa ter ao menos 1 sessão — o saldo parte dele.
    private static void validateNameAndSessionCount(String name, Integer sessionCount) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do plano é obrigatório.");
        }
        if (sessionCount != null && sessionCount < 1) {
            throw new BusinessRuleException("A quantidade de sessões, quando informada, deve ser de no mínimo 1.");
        }
    }

}
