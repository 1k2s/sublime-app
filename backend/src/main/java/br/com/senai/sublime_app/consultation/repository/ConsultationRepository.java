package br.com.senai.sublime_app.consultation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.PricingSource;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long> {

    // Sessões usadas de um item de contrato: atendimentos ativos que debitaram o item
    // (CONTRACT_ITEM) em qualquer versão da cadeia do contrato (cada aditivo é uma
    // versão nova) e com linha do grupo do item (contrato + grupo = item). Só itens
    // por duração têm saldo, por isso o grupo vem da linha por duração.
    long countByContractIdInAndPricingSourceAndActiveTrueAndSessionDurationPricePricingGroupId(
            List<Long> contractIds, PricingSource pricingSource, Long pricingGroupId);

    // Listagem com filtros opcionais (nulo = sem filtro), ativos e inativos, do mais
    // recente para o mais antigo. As relações exibidas na resposta vêm junto
    // (EntityGraph): sem isso, o Hibernate faria consultas extras por atendimento (N+1).
    @EntityGraph(attributePaths = { "patient", "provider", "technique",
            "sessionDurationPrice.plan", "sessionFrequencyPrice.plan" })
    @Query("""
            select c from ConsultationEntity c
            where (:providerId is null or c.provider.id = :providerId)
              and (:patientId is null or c.patient.id = :patientId)
              and (:from is null or c.occurredAt >= :from)
              and (:to is null or c.occurredAt <= :to)
            order by c.occurredAt desc, c.id desc
            """)
    List<ConsultationEntity> search(@Param("providerId") Long providerId, @Param("patientId") Long patientId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
