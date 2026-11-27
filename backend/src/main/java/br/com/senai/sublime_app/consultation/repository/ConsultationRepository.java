package br.com.senai.sublime_app.consultation.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.senai.sublime_app.consultation.domain.ConsultationConsumption;
import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;
import br.com.senai.sublime_app.consultation.domain.PricingSource;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long> {

    // Sessões usadas de um item de contrato: atendimentos ativos que debitaram o item
    // (CONTRACT_ITEM) em qualquer versão da cadeia do contrato (cada aditivo é uma
    // versão nova) e com linha do grupo do item (contrato + grupo = item). Só itens
    // por duração têm saldo, por isso o grupo vem da linha por duração.
    long countByContractIdInAndPricingSourceAndActiveTrueAndSessionDurationPricePricingGroupId(
            List<Long> contractIds, PricingSource pricingSource, Long pricingGroupId);

    // Aulas do Pilates em Grupo lançadas por um item (o grupo vem da linha por frequência), na
    // cadeia toda: só controle de presença
    long countByContractIdInAndPricingSourceAndActiveTrueAndSessionFrequencyPricePricingGroupId(
            List<Long> contractIds, PricingSource pricingSource, Long pricingGroupId);

    // ---------------------------------------------------------------------------
    // Consumo para a conta do paciente (módulo billing): só os campos da projeção, sem carregar
    // as entidades. Os joins são "left" porque o contrato (avulso) e a linha por duração (Grupo)
    // podem ser nulos. A soma é feita em Java (ConsultationConsumption.sumConsumed): somar no
    // JPQL um campo Money, que tem AttributeConverter, não tem tipo de retorno garantido
    // ---------------------------------------------------------------------------

    // De um contrato: os atendimentos ativos de qualquer versão da cadeia
    @Query("""
            select new br.com.senai.sublime_app.consultation.domain.ConsultationConsumption(
                c.id, ct.id, p.id, c.pricingSource, c.baseValue, case when sdp.id is not null then true else false end,
                coalesce(sdp.pricingGroup.id, sfp.pricingGroup.id), c.occurredAt)
            from ConsultationEntity c join c.patient p left join c.contract ct
                left join c.sessionDurationPrice sdp left join c.sessionFrequencyPrice sfp
            where c.active = true and ct.id in :contractIds
            """)
    List<ConsultationConsumption> findConsumptionByContractIdIn(@Param("contractIds") List<Long> contractIds);

    // Do avulso de um paciente: os atendimentos ativos sem contrato
    @Query("""
            select new br.com.senai.sublime_app.consultation.domain.ConsultationConsumption(
                c.id, ct.id, p.id, c.pricingSource, c.baseValue, case when sdp.id is not null then true else false end,
                coalesce(sdp.pricingGroup.id, sfp.pricingGroup.id), c.occurredAt)
            from ConsultationEntity c join c.patient p left join c.contract ct
                left join c.sessionDurationPrice sdp left join c.sessionFrequencyPrice sfp
            where c.active = true and ct is null and p.id = :patientId
            """)
    List<ConsultationConsumption> findConsumptionWithoutContractByPatientId(@Param("patientId") Long patientId);

    // Todos os atendimentos ativos: a lista do Financeiro numa consulta só (sem N+1)
    @Query("""
            select new br.com.senai.sublime_app.consultation.domain.ConsultationConsumption(
                c.id, ct.id, p.id, c.pricingSource, c.baseValue, case when sdp.id is not null then true else false end,
                coalesce(sdp.pricingGroup.id, sfp.pricingGroup.id), c.occurredAt)
            from ConsultationEntity c join c.patient p left join c.contract ct
                left join c.sessionDurationPrice sdp left join c.sessionFrequencyPrice sfp
            where c.active = true
            """)
    List<ConsultationConsumption> findAllActiveConsumption();

    // Os atendimentos ativos num período (pela data do atendimento, inclusivo): a Produção
    @Query("""
            select new br.com.senai.sublime_app.consultation.domain.ConsultationConsumption(
                c.id, ct.id, p.id, c.pricingSource, c.baseValue, case when sdp.id is not null then true else false end,
                coalesce(sdp.pricingGroup.id, sfp.pricingGroup.id), c.occurredAt)
            from ConsultationEntity c join c.patient p left join c.contract ct
                left join c.sessionDurationPrice sdp left join c.sessionFrequencyPrice sfp
            where c.active = true and c.occurredAt between :from and :to
            """)
    List<ConsultationConsumption> findConsumptionBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Os atendimentos ativos de um contrato (a cadeia toda), na ordem da numeração das sessões:
    // pela data e, no mesmo dia, pela ordem do lançamento. As relações exibidas vêm junto (N+1)
    @EntityGraph(attributePaths = { "patient", "provider", "technique",
            "sessionDurationPrice.pricingGroup", "sessionFrequencyPrice.pricingGroup" })
    List<ConsultationEntity> findByContractIdInAndActiveTrueOrderByOccurredAtAscIdAsc(List<Long> contractIds);

    // Listagem com filtros opcionais (nulo = sem filtro), ativos e inativos, do mais
    // recente para o mais antigo. Dois períodos: from/to pela data do atendimento e
    // launchedFrom/launchedTo pela do lançamento ([início, fim), o fim exclusivo). As
    // relações exibidas na resposta vêm junto (EntityGraph): sem isso, o Hibernate faria
    // consultas extras por atendimento (N+1).
    @EntityGraph(attributePaths = { "patient", "provider", "technique",
            "sessionDurationPrice.plan", "sessionFrequencyPrice.plan" })
    @Query("""
            select c from ConsultationEntity c
            where (:providerId is null or c.provider.id = :providerId)
              and (:patientId is null or c.patient.id = :patientId)
              and (:from is null or c.occurredAt >= :from)
              and (:to is null or c.occurredAt <= :to)
              and (:launchedFrom is null or c.launchedAt >= :launchedFrom)
              and (:launchedBefore is null or c.launchedAt < :launchedBefore)
              and (:status is null or c.status = :status)
            order by c.occurredAt desc, c.id desc
            """)
    List<ConsultationEntity> search(@Param("providerId") Long providerId, @Param("patientId") Long patientId,
            @Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("launchedFrom") LocalDateTime launchedFrom, @Param("launchedBefore") LocalDateTime launchedBefore,
            @Param("status") ConsultationStatus status);

    // Últimos lançamentos ativos de um prestador, do mais recente: a base dos pacientes
    // recentes (os distintos são escolhidos no service). Paciente e técnica vêm junto (N+1)
    @EntityGraph(attributePaths = { "patient", "technique" })
    List<ConsultationEntity> findTop100ByProviderIdAndActiveTrueOrderByOccurredAtDescIdDesc(Long providerId);

    // Rendimento: quantidade e repasse somado dos lançamentos ativos no período do
    // lançamento ([início, fim)), por prestador e status; o service separa o rendimento das
    // faltas cobradas. Soma no banco, para o mês de todos os prestadores não carregar as
    // entidades só para somar. SQL nativo porque o payoutValue passa pelo MoneyConverter, e o
    // sum do JPQL sobre um atributo convertido não tem o tipo de retorno garantido
    @Query(nativeQuery = true, value = """
            select provider_id as providerId, status as status,
                   count(*) as consultationCount, sum(payout_value) as payoutTotal
            from consultation
            where active = true
              and launched_at >= :launchedFrom and launched_at < :launchedBefore
              and (:providerId is null or provider_id = :providerId)
            group by provider_id, status
            """)
    List<EarningsRow> sumEarnings(@Param("providerId") Long providerId,
            @Param("launchedFrom") LocalDateTime launchedFrom, @Param("launchedBefore") LocalDateTime launchedBefore);

    // Uma linha do sumEarnings (projeção pelos apelidos das colunas)
    interface EarningsRow {
        Long getProviderId();

        String getStatus();

        long getConsultationCount();

        BigDecimal getPayoutTotal();
    }
}
