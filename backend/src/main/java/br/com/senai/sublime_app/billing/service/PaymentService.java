package br.com.senai.sublime_app.billing.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.billing.domain.PaymentEntity;
import br.com.senai.sublime_app.billing.domain.Receivables;
import br.com.senai.sublime_app.billing.dto.PaymentPreviewRequestDTO;
import br.com.senai.sublime_app.billing.dto.PaymentPreviewResponseDTO;
import br.com.senai.sublime_app.billing.dto.PaymentRequestDTO;
import br.com.senai.sublime_app.billing.dto.PaymentResponseDTO;
import br.com.senai.sublime_app.billing.repository.PaymentRepository;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.repository.ContractRepository;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.pricing.domain.Money;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.repository.UserRepository;

// Registro, prévia, listagem e inativação de pagamentos. As regras ficam na PaymentEntity; aqui,
// as buscas (404) e o "hoje" (no fuso da clínica, ADR 0034).
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository, PatientRepository patientRepository,
            ContractRepository contractRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentResponseDTO register(PaymentRequestDTO dto, CurrentUser currentUser) {
        PatientEntity patient = getPatientOrThrow(dto.patientId());
        ContractEntity contract = dto.contractId() != null ? getContractOrThrow(dto.contractId()) : null;
        UserEntity registeredBy = userRepository.getReferenceById(currentUser.userId());
        PaymentEntity payment = new PaymentEntity(patient, contract, registeredBy, Money.of(dto.amount()),
                Money.of(dto.netAmount()), dto.cardInstallmentsOrOne(), dto.paidAt(), dto.paymentMethod(),
                LocalDate.now());
        return PaymentResponseDTO.fromEntity(paymentRepository.save(payment));
    }

    // A taxa e a previsão de recebimento (nada é gravado), com as mesmas regras do registro
    public PaymentPreviewResponseDTO preview(PaymentPreviewRequestDTO dto) {
        Money amount = Money.of(dto.amount());
        Money netAmount = Money.of(dto.netAmount());
        PaymentEntity.validateTerms(amount, netAmount, dto.paymentMethod(), dto.cardInstallmentsOrOne(),
                dto.paidAt(), LocalDate.now());
        Money fee = PaymentEntity.feeOf(amount, netAmount);
        Receivables receivables = Receivables.of(netAmount, dto.paymentMethod(), dto.cardInstallmentsOrOne(),
                dto.paidAt());
        return new PaymentPreviewResponseDTO(fee.amount(), Receivables.feePercentage(amount, fee),
                PaymentPreviewResponseDTO.fromReceivables(receivables));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> search(Long patientId, Long contractId) {
        return paymentRepository.search(patientId, contractId).stream()
                .map(PaymentResponseDTO::fromEntity)
                .toList();
    }

    // Soft delete: corrige um registro errado (não é estorno). Gravado pelo dirty checking
    @Transactional
    public void deactivate(Long id) {
        PaymentEntity payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com id: " + id));
        payment.deactivate();
    }

    private PatientEntity getPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com id: " + id));
    }

    private ContractEntity getContractOrThrow(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com id: " + id));
    }
}
