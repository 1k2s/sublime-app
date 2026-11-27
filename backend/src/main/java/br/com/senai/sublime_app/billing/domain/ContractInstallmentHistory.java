package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.pricing.domain.Money;

// As parcelas de cada versão do contrato, da mais nova para a mais antiga (a aba Parcelas da
// conta). Ver "Valor e parcelas" em Contract, no domain-model.
// O pago que vale para as parcelas de cada versão:
// - a atual: o pago da cadeia menos o pago antes do aditivo, limitado ao valor parcelado;
// - uma substituída: o pago antes do aditivo seguinte menos o pago antes dela (o que entrou
//   enquanto ela valia), limitado ao valor parcelado; o que não foi pago nela é "substituída",
//   porque o aditivo parcelou de novo o que faltava.
// O pago antes do aditivo de uma versão é o total menos o valor parcelado (zero na original).
public final class ContractInstallmentHistory {

    public record Version(ContractEntity contract, boolean current, Money paidBeforeAmendment,
            List<InstallmentState> installments) {

        // O dia de vencimento da versão (o dia do 1º vencimento)
        public int dueDay() {
            return contract.getFirstDueDate().getDayOfMonth();
        }
    }

    private final List<Version> versions;

    private ContractInstallmentHistory(List<Version> versions) {
        this.versions = versions;
    }

    // current = a versão atual; chainPaid = o bruto dos pagamentos ativos da cadeia
    public static ContractInstallmentHistory of(ContractEntity current, Money chainPaid, LocalDate today) {
        List<ContractEntity> chain = current.getVersionChain();
        List<Version> versions = new ArrayList<>();
        for (int position = 0; position < chain.size(); position++) {
            ContractEntity version = chain.get(position);
            boolean isCurrent = position == 0;
            Money paidTowardInstallments = isCurrent
                    ? version.paidTowardInstallments(chainPaid)
                    : paidWhileValid(version, chain.get(position - 1));
            versions.add(new Version(version, isCurrent, Money.of(paidBefore(version)),
                    InstallmentStates.of(version, paidTowardInstallments, !isCurrent, today)));
        }
        return new ContractInstallmentHistory(List.copyOf(versions));
    }

    public List<Version> versions() {
        return versions;
    }

    // O que entrou nas parcelas da versão enquanto ela valia: do aditivo dela ao seguinte
    private static Money paidWhileValid(ContractEntity version, ContractEntity successor) {
        BigDecimal paid = paidBefore(successor).subtract(paidBefore(version));
        return Money.of(paid.max(BigDecimal.ZERO).min(version.getInstallmentBase().amount()));
    }

    private static BigDecimal paidBefore(ContractEntity version) {
        return version.getTotalValue().amount().subtract(version.getInstallmentBase().amount());
    }
}
