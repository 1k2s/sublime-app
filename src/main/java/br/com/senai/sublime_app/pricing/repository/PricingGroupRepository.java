package br.com.senai.sublime_app.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.senai.sublime_app.pricing.domain.PricingEntity;

public interface PricingRespository extends JpaRepository<PricingEntity, Long> {
    
}
