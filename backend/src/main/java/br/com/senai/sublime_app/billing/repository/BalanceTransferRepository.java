package br.com.senai.sublime_app.billing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.senai.sublime_app.billing.domain.BalanceTransferEntity;

// As transferências de saldo entre contratos (o crédito aplicado no cadastro). As somas são em
// Java, como as dos pagamentos (BalanceTransferEntity.sumOfAmounts)
public interface BalanceTransferRepository extends JpaRepository<BalanceTransferEntity, Long> {

    // As que saem ou entram em qualquer versão das cadeias informadas
    @Query("""
            select t from BalanceTransferEntity t
            where t.fromContract.id in :contractIds or t.toContract.id in :contractIds
            """)
    List<BalanceTransferEntity> findByContractIdIn(@Param("contractIds") List<Long> contractIds);

    // As dos contratos do titular, da mais recente para a mais antiga, com os dois contratos e o
    // plano deles (a aba Pagamentos mostra os rótulos; sem isso, uma consulta por transferência)
    @EntityGraph(attributePaths = { "fromContract", "fromContract.plan", "toContract", "toContract.plan" })
    @Query("""
            select t from BalanceTransferEntity t
            where t.toContract.patient.id = :patientId
            order by t.transferredAt desc, t.id desc
            """)
    List<BalanceTransferEntity> findByPatientId(@Param("patientId") Long patientId);
}
