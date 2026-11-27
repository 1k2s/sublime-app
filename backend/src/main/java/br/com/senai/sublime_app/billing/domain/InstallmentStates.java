package br.com.senai.sublime_app.billing.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.Installments;
import br.com.senai.sublime_app.pricing.domain.Money;

// A situação das parcelas de uma versão do contrato, dado o pago que vale para ela (distribuído
// pela ordem de vencimento, no Installments). Ver "Situação das parcelas" em Payment, no domain-model.
// - paga: o pago cobre o valor;
// - na versão substituída por aditivo: o que não foi pago é "substituída" (o aditivo parcelou de
//   novo o que faltava);
// - com o contrato inativado: o que venceria a partir da inativação é "cancelada" (nada vence);
// - com a vigência terminada (o fim antes de hoje): o que ficou em aberto é "não cobrada" (a conta
//   do contrato terminado vale só pelo saldo, e ele não fica "atrasado" por uma parcela);
// - senão, vencida antes de hoje é "atrasada"; a primeira das outras é a "próxima".
final class InstallmentStates {

    private InstallmentStates() {
    }

    static List<InstallmentState> of(ContractEntity version, Money paidTowardInstallments, boolean superseded,
            LocalDate today) {
        List<Installments.Scheduled> schedule = version.getInstallmentSchedule();
        List<Money> paid = version.getInstallmentValues().distribute(paidTowardInstallments);
        LocalDate deactivatedOn = version.getDeactivatedOn();
        boolean ended = version.getEndDate().isBefore(today);
        List<InstallmentState> states = new ArrayList<>();
        boolean nextFound = false;
        for (int index = 0; index < schedule.size(); index++) {
            Installments.Scheduled installment = schedule.get(index);
            Money installmentPaid = paid.get(index);
            InstallmentState.Status status;
            if (installmentPaid.amount().compareTo(installment.value().amount()) >= 0) {
                status = InstallmentState.Status.PAID;
            } else if (superseded) {
                status = InstallmentState.Status.REPLACED;
            } else if (deactivatedOn != null && !installment.dueDate().isBefore(deactivatedOn)) {
                status = InstallmentState.Status.CANCELED;
            } else if (ended) {
                status = InstallmentState.Status.ENDED;
            } else if (installment.dueDate().isBefore(today)) {
                status = InstallmentState.Status.OVERDUE;
            } else if (!nextFound) {
                status = InstallmentState.Status.NEXT;
                nextFound = true;
            } else {
                status = InstallmentState.Status.UPCOMING;
            }
            states.add(new InstallmentState(installment.number(), installment.dueDate(), installment.value(),
                    installmentPaid, status));
        }
        return states;
    }
}
