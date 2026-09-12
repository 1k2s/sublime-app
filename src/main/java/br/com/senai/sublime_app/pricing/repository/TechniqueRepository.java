package br.com.senai.sublime_app.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;

public interface TechniqueRepository extends JpaRepository<TechniqueEntity, Long> {

}
