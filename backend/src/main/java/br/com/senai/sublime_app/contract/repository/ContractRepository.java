package br.com.senai.sublime_app.contract.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.senai.sublime_app.contract.domain.ContractEntity;

public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    // Contrato vigente = ativo e com endDate >= data informada (o último dia ainda conta).
    // Um contrato ativo porém vencido não bloqueia um novo: ele aguarda a decisão do
    // administrador (prorrogar ou encerrar), e o paciente pode seguir com outro contrato.
    // "exists" em vez de "find": só importa se há algum, e não quebra se houver mais de um.
    boolean existsByPatientIdAndActiveTrueAndEndDateGreaterThanEqual(Long patientId, LocalDate date);

    // Mesma regra do titular, para o beneficiário: no máximo um contrato vigente
    boolean existsByBeneficiaryIdAndActiveTrueAndEndDateGreaterThanEqual(Long beneficiaryId, LocalDate date);

    // Contratos vinculados ao paciente numa data: versões ativas que cobrem o dia, com
    // ele como titular OU beneficiário (no máximo dois). Usado pelo lançamento de
    // atendimento. @Query porque o método derivado não expressa o "ou" com o left
    // join: contrato sem beneficiário também precisa aparecer quando ele é o titular.
    @Query("""
            select c from ContractEntity c left join c.beneficiary b
            where c.active = true
              and (c.patient.id = :patientId or b.id = :patientId)
              and c.startDate <= :date and c.endDate >= :date
            """)
    List<ContractEntity> findActiveCoveringDateByHolderOrBeneficiary(@Param("patientId") Long patientId,
            @Param("date") LocalDate date);

    // Versão atual de cada contrato: a que nenhum aditivo substituiu (nenhuma versão aponta
    // para ela). Inclui a inativada, que continua na lista como "Inativo". Com o plano e os
    // itens (a técnica e a linha de preço, com o plano e o grupo dela) numa consulta só: a resposta
    // da lista lê todos, e sem isso seria uma consulta por contrato e por relação (N+1)
    @EntityGraph(attributePaths = { "plan", "items", "items.technique", "items.sessionDurationPrice",
            "items.sessionDurationPrice.plan", "items.sessionDurationPrice.pricingGroup",
            "items.sessionFrequencyPrice", "items.sessionFrequencyPrice.plan",
            "items.sessionFrequencyPrice.pricingGroup" })
    @Query("""
            select c from ContractEntity c
            where not exists (select 1 from ContractEntity next where next.previousContract = c)
            """)
    List<ContractEntity> findAllCurrentVersions();

    // Versão substituída: existe um aditivo apontando para ela (previous_contract_id é UNIQUE,
    // com índice)
    boolean existsByPreviousContractId(Long contractId);

    // A versão que substituiu esta (o aditivo seguinte), se houver
    Optional<ContractEntity> findByPreviousContractId(Long contractId);

    // A versão atual de cada contrato do titular (a conta do paciente, no billing), com os itens
    @EntityGraph(attributePaths = { "plan", "items", "items.technique", "items.sessionDurationPrice",
            "items.sessionFrequencyPrice" })
    @Query("""
            select c from ContractEntity c
            where c.patient.id = :patientId
              and not exists (select 1 from ContractEntity next where next.previousContract = c)
            """)
    List<ContractEntity> findCurrentVersionsByPatientId(@Param("patientId") Long patientId);

    // Todas as versões de todos os contratos, com os itens, numa consulta só: a lista do
    // Financeiro monta as cadeias em memória (a versão anterior já está carregada), sem uma
    // consulta por contrato (N+1)
    @EntityGraph(attributePaths = { "patient", "plan", "items", "items.technique", "items.sessionDurationPrice",
            "items.sessionFrequencyPrice" })
    @Query("select c from ContractEntity c")
    List<ContractEntity> findAllWithItems();
}
