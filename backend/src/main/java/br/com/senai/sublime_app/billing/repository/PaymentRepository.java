package br.com.senai.sublime_app.billing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.senai.sublime_app.billing.domain.PaymentEntity;

// As somas são feitas em Java (PaymentEntity.sumOfAmounts), sobre uma consulta só: somar no JPQL um campo
// Money, que tem AttributeConverter, não tem um tipo de retorno garantido, e o projeto não usa
// SQL nativo.
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    // Pagamentos ativos de qualquer versão da cadeia de um contrato (cada aditivo é uma versão nova)
    List<PaymentEntity> findByContractIdInAndActiveTrue(List<Long> contractIds);

    // Pagamentos ativos do avulso de um paciente (sem contrato)
    List<PaymentEntity> findByPatientIdAndContractIsNullAndActiveTrue(Long patientId);

    // Todos os pagamentos ativos: a lista do Financeiro numa consulta só (sem N+1). Só os ids do
    // paciente e do contrato são lidos (não carregam as relações)
    List<PaymentEntity> findByActiveTrue();

    // Pagamentos de um paciente (ativos e inativos), com o filtro opcional de contrato (nulo = sem
    // filtro), do mais recente para o mais antigo. O contrato e o plano vêm junto (EntityGraph):
    // a resposta mostra o rótulo do contrato, e sem isso seria uma consulta por pagamento (N+1)
    @EntityGraph(attributePaths = { "contract", "contract.plan" })
    @Query("""
            select p from PaymentEntity p
            where p.patient.id = :patientId
              and (:contractId is null or p.contract.id = :contractId)
            order by p.paidAt desc, p.id desc
            """)
    List<PaymentEntity> search(@Param("patientId") Long patientId, @Param("contractId") Long contractId);
}
