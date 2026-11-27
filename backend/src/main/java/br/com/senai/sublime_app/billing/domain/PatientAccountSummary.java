package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;

// As contas de um paciente juntas: os contratos que aparecem na conta e o avulso (contas
// separadas). Ver "Conta do paciente" em Payment, no domain-model:
// - o saldo é a soma das contas (a visão geral da lista);
// - a situação é avaliada conta a conta: atrasado (alguma parcela vencida em aberto) → devendo
//   (alguma conta com saldo devedor) → com crédito (alguma com saldo credor) → em dia;
// - o próximo vencimento: a parcela vencida mais antiga entre os contratos; sem vencida, a
//   próxima a vencer;
// - os avisos de todas as contas.
public final class PatientAccountSummary {

    // A data e o que falta da parcela
    public record NextDue(LocalDate dueDate, Money amount) {
    }

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final PatientEntity patient;
    private final List<ContractAccount> contracts;
    private final SingleSessionAccount singleSession;
    private final BigDecimal balance;
    private final AccountStatus status;

    private PatientAccountSummary(PatientEntity patient, List<ContractAccount> contracts,
            SingleSessionAccount singleSession) {
        this.patient = patient;
        this.contracts = contracts;
        this.singleSession = singleSession;
        this.balance = sum(contracts.stream().map(ContractAccount::balance), singleSession.balance());
        this.status = statusOf();
    }

    // contractAccounts = as contas dos contratos em que o paciente é titular (as que não aparecem
    // na conta ficam de fora); o vigente primeiro, depois do mais novo para o mais antigo
    public static PatientAccountSummary of(PatientEntity patient, List<ContractAccount> contractAccounts,
            SingleSessionAccount singleSession) {
        List<ContractAccount> shown = contractAccounts.stream()
                .filter(ContractAccount::isShownInAccount)
                .sorted(Comparator.comparing((ContractAccount account) -> !account.contract().isActive())
                        .thenComparing(account -> account.contract().getStartDate(), Comparator.reverseOrder()))
                .toList();
        return new PatientAccountSummary(patient, shown, singleSession);
    }

    // A ordem da lista do Financeiro: os atrasados, da parcela vencida mais antiga para a mais
    // recente; os devendo, do maior saldo devedor para o menor; os outros por nome. Um comparador
    // novo a cada chamada (o Collator não é seguro entre threads)
    public static Comparator<PatientAccountSummary> listOrder() {
        Comparator<PatientAccountSummary> byName = Comparator.comparing(summary -> summary.patient().getName(),
                wordByWord(Collator.getInstance(PT_BR)));
        return Comparator.comparingInt(PatientAccountSummary::listRank)
                .thenComparing((first, second) -> switch (first.status()) {
                    case LATE -> first.oldestOverdueDate().orElseThrow()
                            .compareTo(second.oldestOverdueDate().orElseThrow());
                    case OWING -> first.balance().compareTo(second.balance());
                    case CREDIT, UP_TO_DATE -> 0;
                })
                .thenComparing(byName);
    }

    // Os nomes palavra a palavra, como numa lista telefônica: o Collator sozinho ignora os
    // espaços ("Ana Zélia" viria depois de "Anabela"). Ele continua comparando cada palavra,
    // para os acentos ("Álvaro" junto dos "A")
    private static Comparator<String> wordByWord(Collator collator) {
        return (first, second) -> {
            String[] firstWords = first.trim().split("\\s+");
            String[] secondWords = second.trim().split("\\s+");
            for (int index = 0; index < Math.min(firstWords.length, secondWords.length); index++) {
                int result = collator.compare(firstWords[index], secondWords[index]);
                if (result != 0) {
                    return result;
                }
            }
            return Integer.compare(firstWords.length, secondWords.length);
        };
    }

    // Entra na lista do Financeiro: algum contrato na conta ou avulso lançado ou pago
    public boolean isListed() {
        return !contracts.isEmpty() || singleSession.exists();
    }

    public Money due() {
        return Money.of(sum(contracts.stream().map(account -> account.due().amount()), singleSession.due().amount()));
    }

    public Money paid() {
        return Money.of(sum(contracts.stream().map(account -> account.paid().amount()),
                singleSession.paid().amount()));
    }

    // A soma dos saldos (positivo = credor; negativo = devedor)
    public BigDecimal balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    private AccountStatus statusOf() {
        if (hasOverdue()) {
            return AccountStatus.LATE;
        }
        List<BigDecimal> balances = new ArrayList<>(contracts.stream().map(ContractAccount::balance).toList());
        balances.add(singleSession.balance());
        if (balances.stream().anyMatch(balance -> balance.signum() < 0)) {
            return AccountStatus.OWING;
        }
        return balances.stream().anyMatch(balance -> balance.signum() > 0)
                ? AccountStatus.CREDIT
                : AccountStatus.UP_TO_DATE;
    }

    public boolean hasOverdue() {
        return contracts.stream().anyMatch(ContractAccount::hasOverdue);
    }

    public int overdueCount() {
        return contracts.stream().mapToInt(ContractAccount::overdueCount).sum();
    }

    public Money overdueAmount() {
        return Money.of(sum(contracts.stream().map(account -> account.overdueAmount().amount()), BigDecimal.ZERO));
    }

    public Optional<LocalDate> oldestOverdueDate() {
        return contracts.stream().flatMap(account -> account.oldestOverdueDate().stream())
                .min(Comparator.naturalOrder());
    }

    public Optional<NextDue> nextDue() {
        Stream<InstallmentState> overdue = contracts.stream().flatMap(account -> account.installments().stream())
                .filter(installment -> installment.status() == InstallmentState.Status.OVERDUE);
        Optional<InstallmentState> oldest = overdue.min(Comparator.comparing(InstallmentState::dueDate));
        Optional<InstallmentState> next = oldest.isPresent()
                ? oldest
                : contracts.stream().flatMap(account -> account.nextDue().stream())
                        .min(Comparator.comparing(InstallmentState::dueDate));
        return next.map(installment -> new NextDue(installment.dueDate(), installment.remaining()));
    }

    public List<AccountWarning> warnings() {
        List<AccountWarning> list = new ArrayList<>();
        contracts.forEach(account -> list.addAll(account.warnings()));
        list.addAll(singleSession.warnings());
        return List.copyOf(list);
    }

    public PatientEntity patient() {
        return patient;
    }

    // As contas dos contratos que aparecem na conta, o vigente primeiro
    public List<ContractAccount> contracts() {
        return contracts;
    }

    public SingleSessionAccount singleSession() {
        return singleSession;
    }

    // 0 = atrasado; 1 = devendo; 2 = os outros
    private int listRank() {
        return switch (status()) {
            case LATE -> 0;
            case OWING -> 1;
            case CREDIT, UP_TO_DATE -> 2;
        };
    }

    private static BigDecimal sum(Stream<BigDecimal> values, BigDecimal extra) {
        return values.reduce(extra, BigDecimal::add);
    }
}
