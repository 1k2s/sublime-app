package br.com.senai.sublime_app.contract.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.contract.domain.ContractEntity;

public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    // Contrato vigente = ativo e com endDate >= data informada (o último dia ainda conta).
    // Um contrato ativo porém vencido não bloqueia um novo: ele aguarda a decisão do
    // administrador (prorrogar ou encerrar), e o paciente pode seguir com outro contrato.
    // "exists" em vez de "find": só importa se há algum, e não quebra se houver mais de um.
    boolean existsByPatientIdAndActiveTrueAndEndDateGreaterThanEqual(Long patientId, LocalDate date);
}
