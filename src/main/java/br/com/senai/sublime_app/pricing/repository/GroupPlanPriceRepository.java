package br.com.senai.sublime_app.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.GroupPlanPriceEntity;

public interface GroupPlanPriceRepository extends JpaRepository<GroupPlanPriceEntity, Long> {

}
