package br.com.senai.sublime_app.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.enums.PaymentMethod;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Pagamento do paciente, de valor livre: as parcelas do contrato são só a referência do que se
// espera receber. Nunca é editado: para corrigir um registro errado, inativa e registra de novo
// (inativar não é estornar). O bruto é o que o paciente pagou e entra na conta dele; o líquido é
// o que a clínica recebe depois da taxa da maquininha. Ver "Payment" no domain-model.
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentEntity {

    public static final int MAX_CARD_INSTALLMENTS = 12;

    // As formas aceitas no pagamento: as do cadastro de contrato (o boleto entra com um gateway)
    private static final Set<PaymentMethod> ACCEPTED_METHODS = Set.of(
            PaymentMethod.PIX, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD, PaymentMethod.CASH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Dono da conta: o titular do contrato ou, sem contrato, quem foi atendido no avulso
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    // Nulo no avulso. A conta soma os pagamentos da cadeia de versões do contrato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    // Quem registrou (o admin logado)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_user_id", nullable = false)
    private UserEntity registeredBy;

    // Bruto: o que o paciente pagou (convertido para DECIMAL pelo MoneyConverter)
    @Column(nullable = false, precision = 10, scale = 2)
    private Money amount;

    // Líquido: o que a clínica recebe, de 0 ao bruto
    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private Money netAmount;

    // Parcelas no cartão (1 = à vista); mais de 1 só no crédito
    @Column(name = "card_installments", nullable = false)
    private int cardInstallments;

    @Column(name = "paid_at", nullable = false)
    private LocalDate paidAt;

    // A forma de pagamento real (a do contrato é só a combinada). varchar, não o enum nativo do
    // MySQL (ADR 0021)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    // Soft delete: pagamento inativo não conta na conta nem no faturamento
    @Column(nullable = false)
    private boolean active = true;

    // today = hoje no fuso da clínica, passado pelo service
    public PaymentEntity(PatientEntity patient, ContractEntity contract, UserEntity registeredBy, Money amount,
            Money netAmount, int cardInstallments, LocalDate paidAt, PaymentMethod paymentMethod, LocalDate today) {
        validateTerms(amount, netAmount, paymentMethod, cardInstallments, paidAt, today);
        validateContractBelongsToPatient(contract, patient);
        validateRegisteredBy(registeredBy);
        this.patient = patient;
        this.contract = contract;
        this.registeredBy = registeredBy;
        this.amount = amount;
        this.netAmount = netAmount;
        this.cardInstallments = cardInstallments;
        this.paidAt = paidAt;
        this.paymentMethod = paymentMethod;
        this.active = true;
    }

    // Corrige um registro errado (não é estorno: o estorno é outro movimento)
    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // Taxa da maquininha: o bruto − o líquido
    public Money getFee() {
        return feeOf(amount, netAmount);
    }

    public static Money feeOf(Money amount, Money netAmount) {
        return Money.of(amount.amount().subtract(netAmount.amount()));
    }

    // A soma dos brutos (o que entra na conta do paciente). Zero na lista vazia
    public static Money sumOfAmounts(List<PaymentEntity> payments) {
        return Money.of(payments.stream()
                .map(payment -> payment.amount.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // ---------------------------------------------------------------------------
    // Validações
    // ---------------------------------------------------------------------------

    // Os termos do pagamento (valores, forma, parcelas do cartão, data). Público para a prévia do
    // pagamento, que não cria a entidade, validar com as mesmas regras do registro
    public static void validateTerms(Money amount, Money netAmount, PaymentMethod paymentMethod,
            int cardInstallments, LocalDate paidAt, LocalDate today) {
        requirePositiveAmount(amount);
        validateNetAmount(amount, netAmount);
        validatePaymentMethod(paymentMethod);
        validateCardInstallments(paymentMethod, cardInstallments);
        validatePaidAt(paidAt, today);
    }

    private static void requirePositiveAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new BusinessRuleException("O valor do pagamento deve ser maior que zero.");
        }
    }

    private static void validateNetAmount(Money amount, Money netAmount) {
        if (netAmount == null || netAmount.amount().compareTo(amount.amount()) > 0) {
            throw new BusinessRuleException("O valor líquido deve ser de R$ 0,00 até o valor pago.");
        }
    }

    private static void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (!ACCEPTED_METHODS.contains(paymentMethod)) {
            throw new BusinessRuleException(
                    "Forma de pagamento não aceita: use Pix, cartão de crédito, cartão de débito ou dinheiro.");
        }
    }

    private static void validateCardInstallments(PaymentMethod paymentMethod, int cardInstallments) {
        if (cardInstallments < 1 || cardInstallments > MAX_CARD_INSTALLMENTS) {
            throw new BusinessRuleException("O cartão pode ser parcelado de 1 a 12 vezes.");
        }
        if (cardInstallments > 1 && paymentMethod != PaymentMethod.CREDIT_CARD) {
            throw new BusinessRuleException("Só o cartão de crédito pode ser parcelado.");
        }
    }

    private static void validatePaidAt(LocalDate paidAt, LocalDate today) {
        if (paidAt == null || paidAt.isAfter(today)) {
            throw new BusinessRuleException("A data do pagamento não pode ser futura.");
        }
    }

    // O contrato precisa ser do paciente, como titular. Contrato inativo é aceito: o paciente pode
    // quitar uma dívida de um contrato que já terminou. Compara ids: a relação pode vir como proxy
    private static void validateContractBelongsToPatient(ContractEntity contract, PatientEntity patient) {
        if (contract != null && !contract.getPatient().getId().equals(patient.getId())) {
            throw new BusinessRuleException("O contrato não pertence a este paciente.");
        }
    }

    private static void validateRegisteredBy(UserEntity registeredBy) {
        if (registeredBy == null) {
            throw new BusinessRuleException("O usuário que registra o pagamento é obrigatório.");
        }
    }

    private void requireActive() {
        if (!active) {
            throw new ConflictException("O pagamento já está inativo.");
        }
    }
}
