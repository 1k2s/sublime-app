package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemEntity;
import br.com.senai.sublime_app.contract.domain.Installments;
import br.com.senai.sublime_app.contract.enums.ContractStatus;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.pricing.domain.Money;

// A conta de um contrato, calculada na leitura e nunca gravada (ver "Payment" no domain-model):
// - consumido: os atendimentos ativos da cadeia de versões que contam (ConsultationConsumption);
// - mensalidades do Grupo vencidas: cada versão conta as próprias, no período em que valeu (da
//   criação à inativação), com o valor do item do Grupo ÷ os meses da versão, nos vencimentos dela;
//   o total da cadeia fica nos meses do contrato;
// - devido = consumido + mensalidades; saldo = pago − devido (positivo = credor; negativo =
//   devedor). O pago é o bruto dos pagamentos da cadeia + o crédito recebido de contratos
//   anteriores − o crédito enviado a um contrato novo (as transferências de saldo). As parcelas
//   contam os pagamentos e o recebido, não o enviado: as do contrato antigo continuam pagas;
// - as parcelas da versão atual com a situação, os itens (usado, contratado, excesso), os avisos e
//   as sugestões do pagamento.
public final class ContractAccount {

    // Um item da versão atual na conta. durationMinutes e frequency: os do item por duração (nulos
    // no Grupo); weeklyFrequency: a do Grupo (nula no item por duração). usedValue: o consumido do
    // item (nulo no Grupo, que não cobra por aula); extraValue: as sessões a mais × o valor da
    // sessão travada (zero no Grupo)
    public record ItemState(Long pricingGroupId, String techniqueName, Integer durationMinutes,
            SessionFrequency frequency, Integer weeklyFrequency, boolean groupItem, boolean addedInCurrentVersion,
            int used, int contracted, Money usedValue, Money contractedValue, int extraCount, Money extraValue) {
    }

    private final ContractEntity contract;
    private final Money consumed;
    private final Money groupFeesDue;
    private final Money due;
    private final Money paid;
    private final Money creditReceived;
    private final Money creditTransferred;
    private final BigDecimal balance;
    private final int sessionsUsed;
    private final List<InstallmentState> installments;
    private final List<ItemState> items;
    private final List<AccountWarning> warnings;
    private final List<PaymentSuggestion> suggestions;
    private final AccountStatus status;
    private final ContractStatus contractStatus;

    private ContractAccount(ContractEntity contract, List<ConsultationConsumption> chainConsumption,
            Money chainPayments, Money creditReceived, Money creditTransferred, LocalDate today) {
        this.contract = contract;
        this.consumed = ConsultationConsumption.sumConsumed(chainConsumption);
        this.groupFeesDue = groupFeesDueOn(contract, today);
        this.due = Money.of(consumed.amount().add(groupFeesDue.amount()));
        this.creditReceived = creditReceived;
        this.creditTransferred = creditTransferred;
        Money paidTowardInstallments = Money.of(chainPayments.amount().add(creditReceived.amount()));
        // Pode ficar negativo num caso raro (um pagamento inativado depois de o crédito sair): o
        // saldo usa o valor cru; o pago mostrado fica em no mínimo zero
        BigDecimal netPaid = paidTowardInstallments.amount().subtract(creditTransferred.amount());
        this.paid = Money.of(netPaid.max(BigDecimal.ZERO));
        this.balance = netPaid.subtract(due.amount());
        this.sessionsUsed = (int) chainConsumption.stream().filter(ConsultationConsumption::durationBased).count();
        this.installments = InstallmentStates.of(contract, contract.paidTowardInstallments(paidTowardInstallments),
                false, today);
        this.items = itemsOf(contract, chainConsumption);
        this.warnings = warningsOf();
        this.suggestions = suggestionsOf();
        this.status = statusOf();
        this.contractStatus = contract.statusOn(today, false, balance);
    }

    // contract = a versão atual; chainConsumption e chainPaid = de toda a cadeia de versões (sem
    // transferências de saldo)
    public static ContractAccount of(ContractEntity contract, List<ConsultationConsumption> chainConsumption,
            Money chainPaid, LocalDate today) {
        Money zero = Money.of(BigDecimal.ZERO);
        return new ContractAccount(contract, chainConsumption, chainPaid, zero, zero, today);
    }

    // Com as transferências de saldo: o crédito recebido de contratos anteriores e o enviado a um
    // contrato novo (da cadeia toda)
    public static ContractAccount of(ContractEntity contract, List<ConsultationConsumption> chainConsumption,
            Money chainPayments, Money creditReceived, Money creditTransferred, LocalDate today) {
        return new ContractAccount(contract, chainConsumption, chainPayments, creditReceived, creditTransferred,
                today);
    }

    // Aparece na conta do paciente: o contrato ativo sempre; o inativado, só com saldo diferente
    // de zero (a dívida ou o crédito não somem)
    public boolean isShownInAccount() {
        return contract.isActive() || balance.signum() != 0;
    }

    // O pago em % do total do contrato, sem casas (HALF_UP), para a nota do Resumo ("44% do
    // total"). Total zero: 0
    public int paidPercentageOfTotal() {
        BigDecimal total = contract.getTotalValue().amount();
        if (total.signum() == 0) {
            return 0;
        }
        return paid.amount().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    public boolean hasOverdue() {
        return installments.stream().anyMatch(installment -> installment.status() == InstallmentState.Status.OVERDUE);
    }

    public int overdueCount() {
        return (int) overdue().count();
    }

    // O que falta das parcelas vencidas
    public Money overdueAmount() {
        return Money.of(overdue().map(installment -> installment.remaining().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public Optional<LocalDate> oldestOverdueDate() {
        return overdue().map(InstallmentState::dueDate).findFirst();
    }

    // A próxima parcela em aberto que ainda não venceu
    public Optional<InstallmentState> nextDue() {
        return installments.stream()
                .filter(installment -> installment.status() == InstallmentState.Status.NEXT)
                .findFirst();
    }

    private java.util.stream.Stream<InstallmentState> overdue() {
        return installments.stream()
                .filter(installment -> installment.status() == InstallmentState.Status.OVERDUE);
    }

    // ---------------------------------------------------------------------------
    // Cálculos
    // ---------------------------------------------------------------------------

    // As mensalidades do Grupo vencidas até hoje, versão por versão (da mais antiga à atual)
    private static Money groupFeesDueOn(ContractEntity current, LocalDate today) {
        List<ContractEntity> chain = new ArrayList<>(current.getVersionChain());
        Collections.reverse(chain);
        int monthsLeft = Installments.maxFor(current.getDurationInWeeks());
        BigDecimal total = BigDecimal.ZERO;
        for (int position = 0; position < chain.size() && monthsLeft > 0; position++) {
            ContractEntity version = chain.get(position);
            Optional<ContractItemEntity> groupItem = groupItemOf(version);
            if (groupItem.isEmpty()) {
                continue;
            }
            LocalDate validFrom = position == 0 ? null : version.getCreatedAt();
            LocalDate validTo = version.getDeactivatedOn();
            int months = Installments.maxFor(version.getDurationInWeeks());
            List<Money> fees = Installments.split(groupItem.get().getValue().totalValue(), months).values();
            for (int index = 0; index < months && monthsLeft > 0; index++) {
                LocalDate dueDate = Installments.dueDateOf(version.getFirstDueDate(), index);
                if (dueDate.isAfter(today) || (validTo != null && !dueDate.isBefore(validTo))) {
                    break;
                }
                if (validFrom != null && dueDate.isBefore(validFrom)) {
                    continue;
                }
                total = total.add(fees.get(index).amount());
                monthsLeft--;
            }
        }
        return Money.of(total);
    }

    private static Optional<ContractItemEntity> groupItemOf(ContractEntity version) {
        return version.getItems().stream().filter(item -> !item.isDurationBased()).findFirst();
    }

    // Cada item da versão atual: o usado (os atendimentos do caso 1 pelo grupo do item, na cadeia
    // toda), o contratado e o excesso
    private static List<ItemState> itemsOf(ContractEntity contract, List<ConsultationConsumption> chainConsumption) {
        ContractEntity previous = contract.getPreviousContract();
        List<ItemState> states = new ArrayList<>();
        for (ContractItemEntity item : contract.getItems()) {
            Long groupId = item.getPricingGroup().getId();
            List<ConsultationConsumption> used = chainConsumption.stream()
                    .filter(row -> row.pricingSource() == PricingSource.CONTRACT_ITEM
                            && groupId.equals(row.pricingGroupId()))
                    .toList();
            boolean groupItem = !item.isDurationBased();
            int contracted = item.getValue().sessionCount();
            int extraCount = Math.max(0, used.size() - contracted);
            Money usedValue = groupItem ? null : Money.of(used.stream()
                    .map(row -> row.baseValue().amount()).reduce(BigDecimal.ZERO, BigDecimal::add));
            Money extraValue = groupItem
                    ? Money.of(BigDecimal.ZERO)
                    : Money.of(item.getValue().sessionValue().amount().multiply(BigDecimal.valueOf(extraCount)));
            boolean added = previous != null && previous.findItemOf(item.getPricingGroup()).isEmpty();
            Integer durationMinutes = groupItem ? null : item.getSessionDurationPrice().getDurationMinutes();
            Integer weeklyFrequency = groupItem ? item.getSessionFrequencyPrice().getWeeklyFrequency() : null;
            states.add(new ItemState(groupId, item.getTechnique().getName(), durationMinutes, item.getFrequency(),
                    weeklyFrequency, groupItem, added, used.size(), contracted, usedValue,
                    item.getValue().totalValue(), extraCount, extraValue));
        }
        return List.copyOf(states);
    }

    private List<AccountWarning> warningsOf() {
        List<AccountWarning> list = new ArrayList<>();
        if (hasOverdue()) {
            list.add(new AccountWarning(AccountWarning.Type.OVERDUE_INSTALLMENTS, overdueCount(), overdueAmount(),
                    oldestOverdueDate().orElseThrow(), null, null));
        }
        for (ItemState item : items) {
            if (!item.groupItem() && item.extraCount() > 0) {
                list.add(new AccountWarning(AccountWarning.Type.BEYOND_CONTRACT, item.extraCount(), item.extraValue(),
                        null, item.techniqueName(), item.contracted()));
            }
        }
        if (!contract.isActive() && balance.signum() < 0) {
            list.add(new AccountWarning(AccountWarning.Type.INACTIVE_WITH_DEBT, null, Money.of(balance.negate()), null,
                    null, null));
        }
        return List.copyOf(list);
    }

    // A parcela em aberto (a vencida mais antiga ou a próxima), todas as em aberto e o saldo
    // devedor; só as que valem, sem repetir o mesmo valor
    private List<PaymentSuggestion> suggestionsOf() {
        List<PaymentSuggestion> list = PaymentSuggestion.newList();
        List<InstallmentState> open = installments.stream().filter(InstallmentState::isOpen).toList();
        open.stream().findFirst().ifPresent(first -> PaymentSuggestion.addIfUseful(list,
                PaymentSuggestion.Type.OPEN_INSTALLMENT, first.remaining(), first.dueDate()));
        Money allOpen = Money.of(open.stream().map(installment -> installment.remaining().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        PaymentSuggestion.addIfUseful(list, PaymentSuggestion.Type.ALL_OPEN_INSTALLMENTS, allOpen);
        if (balance.signum() < 0) {
            PaymentSuggestion.addIfUseful(list, PaymentSuggestion.Type.BALANCE_DUE, Money.of(balance.negate()));
        }
        return List.copyOf(list);
    }

    private AccountStatus statusOf() {
        if (hasOverdue()) {
            return AccountStatus.LATE;
        }
        if (balance.signum() < 0) {
            return AccountStatus.OWING;
        }
        return balance.signum() > 0 ? AccountStatus.CREDIT : AccountStatus.UP_TO_DATE;
    }

    // ---------------------------------------------------------------------------
    // Leitura
    // ---------------------------------------------------------------------------

    public ContractEntity contract() {
        return contract;
    }

    public Money consumed() {
        return consumed;
    }

    public Money groupFeesDue() {
        return groupFeesDue;
    }

    public Money due() {
        return due;
    }

    // Os pagamentos + o crédito recebido − o crédito enviado
    public Money paid() {
        return paid;
    }

    // O crédito recebido de contratos anteriores (transferências de saldo)
    public Money creditReceived() {
        return creditReceived;
    }

    // O crédito enviado a um contrato novo
    public Money creditTransferred() {
        return creditTransferred;
    }

    // O status do contrato (vigente, encerrado, vencido ou cancelado), com o saldo desta conta
    public ContractStatus contractStatus() {
        return contractStatus;
    }

    // pago − devido: positivo = saldo credor; negativo = saldo devedor
    public BigDecimal balance() {
        return balance;
    }

    // Os atendimentos por duração da cadeia (o "10 sessões" do Resumo)
    public int sessionsUsed() {
        return sessionsUsed;
    }

    public List<InstallmentState> installments() {
        return installments;
    }

    public List<ItemState> items() {
        return items;
    }

    public List<AccountWarning> warnings() {
        return warnings;
    }

    public List<PaymentSuggestion> suggestions() {
        return suggestions;
    }

    public AccountStatus status() {
        return status;
    }
}
