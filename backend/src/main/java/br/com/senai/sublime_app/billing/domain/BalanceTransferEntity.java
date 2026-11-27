package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
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

// O crédito de um contrato aplicado em outro, no cadastro do contrato novo: sai da conta do antigo
// e entra na do novo como pago. Não é faturamento (não entrou dinheiro). Imutável: se o contrato
// novo for cancelado, o crédito fica nele como saldo credor e volta a estar disponível. Ver "Crédito
// de contratos anteriores" em Payment, no domain-model
@Entity
@Table(name = "balance_transfer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BalanceTransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // A versão atual do contrato de onde o crédito sai
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_contract_id", nullable = false)
    private ContractEntity fromContract;

    // O contrato novo, onde o crédito entra
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_contract_id", nullable = false)
    private ContractEntity toContract;

    @Column(nullable = false, precision = 10, scale = 2)
    private Money amount;

    @Column(name = "transferred_at", nullable = false)
    private LocalDate transferredAt;

    public BalanceTransferEntity(ContractEntity fromContract, ContractEntity toContract, Money amount,
            LocalDate transferredAt) {
        validateContracts(fromContract, toContract);
        requirePositiveAmount(amount);
        this.fromContract = fromContract;
        this.toContract = toContract;
        this.amount = amount;
        this.transferredAt = transferredAt;
    }

    // A soma das transferências (as recebidas ou as enviadas por uma conta)
    public static Money sumOfAmounts(List<BalanceTransferEntity> transfers) {
        return Money.of(transfers.stream()
                .map(transfer -> transfer.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // Dois contratos diferentes, do mesmo titular (o crédito é do paciente)
    private static void validateContracts(ContractEntity fromContract, ContractEntity toContract) {
        if (fromContract == null || toContract == null || fromContract.getId().equals(toContract.getId())) {
            throw new BusinessRuleException("A transferência de saldo precisa de dois contratos diferentes.");
        }
        if (!fromContract.getPatient().getId().equals(toContract.getPatient().getId())) {
            throw new BusinessRuleException("O crédito só pode ir para um contrato do mesmo titular.");
        }
    }

    private static void requirePositiveAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new BusinessRuleException("O valor da transferência de saldo deve ser maior que zero.");
        }
    }
}
