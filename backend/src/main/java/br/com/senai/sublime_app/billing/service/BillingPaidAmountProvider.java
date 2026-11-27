package br.com.senai.sublime_app.billing.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.senai.sublime_app.billing.domain.BalanceTransferEntity;
import br.com.senai.sublime_app.billing.domain.PaymentEntity;
import br.com.senai.sublime_app.billing.repository.BalanceTransferRepository;
import br.com.senai.sublime_app.billing.repository.PaymentRepository;
import br.com.senai.sublime_app.contract.service.PaidAmountProvider;
import br.com.senai.sublime_app.pricing.domain.Money;

// O pago de um contrato para o aditivo: o contract declara a interface e o billing a implementa,
// para a dependência continuar apontando para cima (ver o PaidAmountProvider). O pago é o dos
// pagamentos ativos + o crédito recebido de contratos anteriores − o enviado a um contrato novo
@Component
public class BillingPaidAmountProvider implements PaidAmountProvider {

    private final PaymentRepository paymentRepository;
    private final BalanceTransferRepository balanceTransferRepository;

    public BillingPaidAmountProvider(PaymentRepository paymentRepository,
            BalanceTransferRepository balanceTransferRepository) {
        this.paymentRepository = paymentRepository;
        this.balanceTransferRepository = balanceTransferRepository;
    }

    @Override
    public Money paidOn(List<Long> contractChainIds) {
        BigDecimal payments = PaymentEntity.sumOfAmounts(
                paymentRepository.findByContractIdInAndActiveTrue(contractChainIds)).amount();
        List<BalanceTransferEntity> transfers = balanceTransferRepository.findByContractIdIn(contractChainIds);
        BigDecimal received = sumWhere(transfers, contractChainIds, true);
        BigDecimal transferred = sumWhere(transfers, contractChainIds, false);
        return Money.of(payments.add(received).subtract(transferred).max(BigDecimal.ZERO));
    }

    // As recebidas (destino na cadeia) ou as enviadas (origem na cadeia)
    private static BigDecimal sumWhere(List<BalanceTransferEntity> transfers, List<Long> chainIds, boolean received) {
        return BalanceTransferEntity.sumOfAmounts(transfers.stream()
                .filter(transfer -> chainIds.contains(received
                        ? transfer.getToContract().getId()
                        : transfer.getFromContract().getId()))
                .toList()).amount();
    }
}
