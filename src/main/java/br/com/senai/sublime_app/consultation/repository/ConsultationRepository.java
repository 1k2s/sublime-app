package br.com.senai.sublime_app.consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long>  {
    
}
