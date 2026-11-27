package br.com.senai.sublime_app.billing.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.billing.domain.BalanceTransferEntity;
import br.com.senai.sublime_app.billing.domain.ContractAccount;
import br.com.senai.sublime_app.billing.domain.ContractInstallmentHistory;
import br.com.senai.sublime_app.billing.domain.ContractSessions;
import br.com.senai.sublime_app.billing.domain.PatientAccountSummary;
import br.com.senai.sublime_app.billing.domain.PaymentEntity;
import br.com.senai.sublime_app.billing.domain.SingleSessionAccount;
import br.com.senai.sublime_app.billing.dto.AccountListItemResponseDTO;
import br.com.senai.sublime_app.billing.dto.BalanceTransferResponseDTO;
import br.com.senai.sublime_app.billing.dto.ContractSessionsResponseDTO;
import br.com.senai.sublime_app.billing.dto.ContractVersionInstallmentsResponseDTO;
import br.com.senai.sublime_app.billing.dto.PatientAccountResponseDTO;
import br.com.senai.sublime_app.billing.repository.BalanceTransferRepository;
import br.com.senai.sublime_app.billing.repository.PaymentRepository;
import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.repository.ConsultationRepository;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

// A conta do paciente, a lista do Financeiro e as abas Parcelas e Sessões. As contas ficam no
// domínio (ContractAccount, SingleSessionAccount, PatientAccountSummary); aqui, as buscas, o
// agrupamento pela cadeia de versões (cada aditivo é uma versão nova) e o "hoje" (no fuso da
// clínica, ADR 0034). Calculado na leitura, nada é gravado.
@Service
public class BillingAccountService {

    private final ContractRepository contractRepository;
    private final ConsultationRepository consultationRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceTransferRepository balanceTransferRepository;
    private final PatientRepository patientRepository;

    public BillingAccountService(ContractRepository contractRepository, ConsultationRepository consultationRepository,
            PaymentRepository paymentRepository, BalanceTransferRepository balanceTransferRepository,
            PatientRepository patientRepository) {
        this.contractRepository = contractRepository;
        this.consultationRepository = consultationRepository;
        this.paymentRepository = paymentRepository;
        this.balanceTransferRepository = balanceTransferRepository;
        this.patientRepository = patientRepository;
    }

    // A lista do Financeiro em cinco consultas, qualquer que seja o número de pacientes: os
    // contratos (com os itens), o consumo, os pagamentos, as transferências de saldo e os
    // pacientes só do avulso
    @Transactional(readOnly = true)
    public List<AccountListItemResponseDTO> findAccounts() {
        LocalDate today = LocalDate.now();
        List<ContractEntity> currents = currentVersionsOf(contractRepository.findAllWithItems());
        ChainGrouping chains = ChainGrouping.of(currents);

        Map<Long, List<ConsultationConsumption>> singleConsumptionByPatient = new HashMap<>();
        for (ConsultationConsumption row : consultationRepository.findAllActiveConsumption()) {
            if (row.contractId() == null) {
                singleConsumptionByPatient.computeIfAbsent(row.patientId(), id -> new ArrayList<>()).add(row);
            } else {
                chains.addConsumption(row);
            }
        }
        Map<Long, List<PaymentEntity>> singlePaymentsByPatient = new HashMap<>();
        for (PaymentEntity payment : paymentRepository.findByActiveTrue()) {
            if (payment.getContract() == null) {
                singlePaymentsByPatient.computeIfAbsent(payment.getPatient().getId(), id -> new ArrayList<>())
                        .add(payment);
            } else {
                chains.addPayment(payment);
            }
        }
        balanceTransferRepository.findAll().forEach(chains::addTransfer);

        Map<Long, List<ContractAccount>> accountsByPatient = currents.stream()
                .map(contract -> chains.accountOf(contract, today))
                .collect(Collectors.groupingBy(account -> account.contract().getPatient().getId()));
        Map<Long, PatientEntity> patients = patientsOf(currents, singleConsumptionByPatient.keySet(),
                singlePaymentsByPatient.keySet());

        return patients.values().stream()
                .map(patient -> PatientAccountSummary.of(patient,
                        accountsByPatient.getOrDefault(patient.getId(), List.of()),
                        SingleSessionAccount.of(singleConsumptionByPatient.getOrDefault(patient.getId(), List.of()),
                                PaymentEntity.sumOfAmounts(
                                        singlePaymentsByPatient.getOrDefault(patient.getId(), List.of())))))
                .filter(PatientAccountSummary::isListed)
                .sorted(PatientAccountSummary.listOrder())
                .map(AccountListItemResponseDTO::fromSummary)
                .toList();
    }

    // A conta do paciente: os contratos em que ele é titular (os que aparecem na conta) e o avulso
    @Transactional(readOnly = true)
    public PatientAccountResponseDTO findPatientAccount(Long patientId) {
        LocalDate today = LocalDate.now();
        PatientEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com id: " + patientId));
        List<ContractEntity> currents = contractRepository.findCurrentVersionsByPatientId(patientId);
        Map<Long, ContractAccount> accountById = accountsOf(currents, today);

        SingleSessionAccount singleSession = SingleSessionAccount.of(
                consultationRepository.findConsumptionWithoutContractByPatientId(patientId),
                PaymentEntity.sumOfAmounts(paymentRepository.findByPatientIdAndContractIsNullAndActiveTrue(patientId)));
        List<ContractAccount> accounts = currents.stream().map(contract -> accountById.get(contract.getId())).toList();
        return PatientAccountResponseDTO.fromSummary(PatientAccountSummary.of(patient, accounts, singleSession));
    }

    // A conta de cada versão atual informada (pelo id dela): o consumo, os pagamentos e as
    // transferências das cadeias, numa consulta de cada. Também para o status do contrato e o
    // crédito do titular (BillingContractBalanceProvider)
    @Transactional(readOnly = true)
    public Map<Long, ContractAccount> accountsOf(List<ContractEntity> currents, LocalDate today) {
        ChainGrouping chains = ChainGrouping.of(currents);
        List<Long> chainIds = chains.versionIds();
        if (!chainIds.isEmpty()) {
            consultationRepository.findConsumptionByContractIdIn(chainIds).forEach(chains::addConsumption);
            paymentRepository.findByContractIdInAndActiveTrue(chainIds).forEach(chains::addPayment);
            balanceTransferRepository.findByContractIdIn(chainIds).forEach(chains::addTransfer);
        }
        Map<Long, ContractAccount> accounts = new HashMap<>();
        currents.forEach(contract -> accounts.put(contract.getId(), chains.accountOf(contract, today)));
        return accounts;
    }

    // As parcelas de cada versão, da mais nova para a mais antiga. Qualquer versão da cadeia
    // leva à atual. O pago das parcelas: os pagamentos + o crédito recebido (o enviado não tira o
    // pago delas)
    @Transactional(readOnly = true)
    public List<ContractVersionInstallmentsResponseDTO> findInstallments(Long contractId) {
        ContractEntity current = currentVersionOf(getContractOrThrow(contractId));
        List<Long> chainIds = current.getVersionChainIds();
        Money payments = PaymentEntity.sumOfAmounts(paymentRepository.findByContractIdInAndActiveTrue(chainIds));
        Money creditReceived = BalanceTransferEntity.sumOfAmounts(balanceTransferRepository.findByContractIdIn(chainIds)
                .stream().filter(transfer -> chainIds.contains(transfer.getToContract().getId())).toList());
        return ContractInstallmentHistory.of(current, Money.of(payments.amount().add(creditReceived.amount())),
                LocalDate.now())
                .versions().stream()
                .map(ContractVersionInstallmentsResponseDTO::fromVersion)
                .toList();
    }

    // As transferências de saldo dos contratos do titular (a aba Pagamentos)
    @Transactional(readOnly = true)
    public List<BalanceTransferResponseDTO> findTransfers(Long patientId) {
        return balanceTransferRepository.findByPatientId(patientId).stream()
                .map(BalanceTransferResponseDTO::fromEntity)
                .toList();
    }

    // Os atendimentos ativos da cadeia, numerados. Qualquer versão da cadeia leva à atual
    @Transactional(readOnly = true)
    public ContractSessionsResponseDTO findSessions(Long contractId) {
        ContractEntity current = currentVersionOf(getContractOrThrow(contractId));
        return ContractSessionsResponseDTO.fromSessions(ContractSessions.of(current,
                consultationRepository.findByContractIdInAndActiveTrueOrderByOccurredAtAscIdAsc(
                        current.getVersionChainIds())));
    }

    // ---------------------------------------------------------------------------

    // As versões que nenhum aditivo substituiu (o id da anterior vem do proxy, sem consulta)
    private static List<ContractEntity> currentVersionsOf(List<ContractEntity> allVersions) {
        Set<Long> superseded = allVersions.stream()
                .map(ContractEntity::getPreviousContract)
                .filter(Objects::nonNull)
                .map(ContractEntity::getId)
                .collect(Collectors.toSet());
        return allVersions.stream().filter(contract -> !superseded.contains(contract.getId())).toList();
    }

    // Segue os aditivos até a versão atual (uma consulta por aditivo seguinte)
    private ContractEntity currentVersionOf(ContractEntity version) {
        ContractEntity current = version;
        for (Optional<ContractEntity> next = contractRepository.findByPreviousContractId(current.getId());
                next.isPresent(); next = contractRepository.findByPreviousContractId(current.getId())) {
            current = next.get();
        }
        return current;
    }

    // Os titulares dos contratos e os pacientes só do avulso (buscados numa consulta)
    private Map<Long, PatientEntity> patientsOf(List<ContractEntity> currents, Set<Long> singleConsumptionPatientIds,
            Set<Long> singlePaymentPatientIds) {
        Map<Long, PatientEntity> patients = new HashMap<>();
        currents.forEach(contract -> patients.put(contract.getPatient().getId(), contract.getPatient()));
        Set<Long> missing = new HashSet<>(singleConsumptionPatientIds);
        missing.addAll(singlePaymentPatientIds);
        missing.removeAll(patients.keySet());
        if (!missing.isEmpty()) {
            patientRepository.findAllById(missing).forEach(patient -> patients.put(patient.getId(), patient));
        }
        return patients;
    }

    private ContractEntity getContractOrThrow(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com id: " + id));
    }

    // O consumo, os pagamentos e as transferências de cada versão agrupados pela versão atual da
    // cadeia (a transferência conta como recebida na cadeia de destino e enviada na de origem)
    private static final class ChainGrouping {

        private final Map<Long, Long> currentIdByVersionId;
        private final Map<Long, List<ConsultationConsumption>> consumptionByCurrent = new HashMap<>();
        private final Map<Long, List<PaymentEntity>> paymentsByCurrent = new HashMap<>();
        private final Map<Long, List<BalanceTransferEntity>> receivedByCurrent = new HashMap<>();
        private final Map<Long, List<BalanceTransferEntity>> transferredByCurrent = new HashMap<>();

        private ChainGrouping(Map<Long, Long> currentIdByVersionId) {
            this.currentIdByVersionId = currentIdByVersionId;
        }

        static ChainGrouping of(List<ContractEntity> currents) {
            Map<Long, Long> currentIdByVersionId = new HashMap<>();
            for (ContractEntity current : currents) {
                current.getVersionChainIds().forEach(versionId -> currentIdByVersionId.put(versionId, current.getId()));
            }
            return new ChainGrouping(currentIdByVersionId);
        }

        List<Long> versionIds() {
            return List.copyOf(currentIdByVersionId.keySet());
        }

        void addConsumption(ConsultationConsumption row) {
            add(consumptionByCurrent, row.contractId(), row);
        }

        void addPayment(PaymentEntity payment) {
            add(paymentsByCurrent, payment.getContract().getId(), payment);
        }

        void addTransfer(BalanceTransferEntity transfer) {
            add(receivedByCurrent, transfer.getToContract().getId(), transfer);
            add(transferredByCurrent, transfer.getFromContract().getId(), transfer);
        }

        ContractAccount accountOf(ContractEntity current, LocalDate today) {
            Long id = current.getId();
            return ContractAccount.of(current, consumptionByCurrent.getOrDefault(id, List.of()),
                    PaymentEntity.sumOfAmounts(paymentsByCurrent.getOrDefault(id, List.of())),
                    BalanceTransferEntity.sumOfAmounts(receivedByCurrent.getOrDefault(id, List.of())),
                    BalanceTransferEntity.sumOfAmounts(transferredByCurrent.getOrDefault(id, List.of())), today);
        }

        private <T> void add(Map<Long, List<T>> byCurrent, Long versionId, T value) {
            Long currentId = currentIdByVersionId.get(versionId);
            if (currentId != null) {
                byCurrent.computeIfAbsent(currentId, id -> new ArrayList<>()).add(value);
            }
        }
    }
}
