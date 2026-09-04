package br.com.senai.sublime_app.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.patient.domain.PatientEntity;

public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
}
