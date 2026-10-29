package br.com.senai.sublime_app.contract.repository;

import java.time.LocalDate;
import java.util.List;

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
}
