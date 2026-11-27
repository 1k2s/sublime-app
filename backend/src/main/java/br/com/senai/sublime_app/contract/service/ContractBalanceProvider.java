package br.com.senai.sublime_app.contract.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.pricing.domain.Money;

// O saldo da conta dos contratos e o crédito do paciente, que o status do contrato e o cadastro
// precisam. A conta é do billing, acima do contract no mapa de módulos: o contract declara esta
// interface e o billing a implementa (inversão de dependência, ADR 0050 e ADR 0051), como o
// PaidAmountProvider. Ver "Status do contrato" em Contract e "Crédito de contratos anteriores" em
// Payment, no domain-model
public interface ContractBalanceProvider {

    // O saldo (pago − devido) da conta de cada contrato, pela versão atual (o id dela), numa
    // leitura agrupada (a lista de contratos não faz uma consulta por contrato)
    Map<Long, BigDecimal> balancesOf(List<ContractEntity> currentVersions);

    // O crédito disponível do titular: a soma dos saldos credores dos contratos dele que não estão
    // vigentes (vencidos ou cancelados)
    Money availableCreditOf(Long patientId);

    // Transfere o crédito aplicado para o contrato novo: dos contratos com crédito do titular, do
    // mais antigo para o mais novo, até o valor (gravado como transferência de saldo no billing)
    void transferCreditTo(ContractEntity newContract, Money amount);
}
