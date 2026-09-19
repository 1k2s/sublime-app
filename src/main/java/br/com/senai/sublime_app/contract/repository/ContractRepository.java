package br.com.senai.sublime_app.contract.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.contract.domain.ContractEntity;

public interface ContractRepository extends JpaRepository<ContractEntity, Long> {
    Optional<ContractEntity> findByPatientIdAndActiveTrue(Long patientId);
}