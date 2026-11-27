package br.com.senai.sublime_app.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.senai.sublime_app.billing.domain.BalanceTransferEntity;
import br.com.senai.sublime_app.billing.domain.ContractAccount;
import br.com.senai.sublime_app.billing.repository.BalanceTransferRepository;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.contract.service.ContractBalanceProvider;
import br.com.senai.sublime_app.pricing.domain.Money;

// O saldo da conta dos contratos e o crédito do titular, para o contract (o status e o cadastro):
// o contract declara a interface e o billing a implementa, para a dependência continuar apontando
// para cima (ADR 0048). As contas são as do BillingAccountService
@Component
public class BillingContractBalanceProvider implements ContractBalanceProvider {

    private final BillingAccountService billingAccountService;
    private final ContractRepository contractRepository;
    private final BalanceTransferRepository balanceTransferRepository;

    public BillingContractBalanceProvider(BillingAccountService billingAccountService,
            ContractRepository contractRepository, BalanceTransferRepository balanceTransferRepository) {
        this.billingAccountService = billingAccountService;
        this.contractRepository = contractRepository;
        this.balanceTransferRepository = balanceTransferRepository;
    }

    // Com mais de um contrato (a lista), todas as versões com os itens são carregadas antes, numa
    // consulta só: as cadeias de versões e as mensalidades do Grupo saem da memória, sem uma
    // consulta por versão anterior (N+1)
    @Override
    public Map<Long, BigDecimal> balancesOf(List<ContractEntity> currentVersions) {
        if (currentVersions.size() > 1) {
            contractRepository.findAllWithItems();
        }
        Map<Long, BigDecimal> balances = new HashMap<>();
        billingAccountService.accountsOf(currentVersions, LocalDate.now())
                .forEach((contractId, account) -> balances.put(contractId, account.balance()));
        return balances;
    }

    @Override
    public Money availableCreditOf(Long patientId) {
        return Money.of(creditsOf(patientId, null).stream()
                .map(ContractAccount::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // Do mais antigo para o mais novo, cada um até o próprio crédito, até o valor aplicado; o que
    // sobrar fica no contrato antigo
    @Override
    public void transferCreditTo(ContractEntity newContract, Money amount) {
        LocalDate today = LocalDate.now();
        BigDecimal remaining = amount.amount();
        for (ContractAccount source : creditsOf(newContract.getPatient().getId(), newContract.getId())) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal taken = source.balance().min(remaining);
            balanceTransferRepository.save(new BalanceTransferEntity(source.contract(), newContract, Money.of(taken),
                    today));
            remaining = remaining.subtract(taken);
        }
    }

    // As contas com crédito do titular nos contratos que não estão vigentes (vencidos ou
    // cancelados), da mais antiga para a mais nova. exceptContractId: o contrato novo, que não é
    // origem do próprio crédito
    private List<ContractAccount> creditsOf(Long patientId, Long exceptContractId) {
        LocalDate today = LocalDate.now();
        List<ContractEntity> notCurrent = contractRepository.findCurrentVersionsByPatientId(patientId).stream()
                .filter(contract -> !contract.isCurrentOn(today))
                .filter(contract -> !contract.getId().equals(exceptContractId))
                .toList();
        if (notCurrent.isEmpty()) {
            return List.of();
        }
        return billingAccountService.accountsOf(notCurrent, today).values().stream()
                .filter(account -> account.balance().signum() > 0)
                .sorted(Comparator.comparing((ContractAccount account) -> account.contract().getStartDate())
                        .thenComparing(account -> account.contract().getId()))
                .toList();
    }
}
