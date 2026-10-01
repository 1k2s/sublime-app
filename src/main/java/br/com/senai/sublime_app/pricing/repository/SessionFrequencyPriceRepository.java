package br.com.senai.sublime_app.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

public interface SessionFrequencyPriceRepository extends JpaRepository<SessionFrequencyPriceEntity, Long> {

    // Linhas vigentes (validTo = null) de um grupo. O plano vem junto (EntityGraph)
    // porque cada linha é exibida com o nome do plano — evita uma consulta por linha (N+1).
    @EntityGraph(attributePaths = "plan")
    List<SessionFrequencyPriceEntity> findByPricingGroupIdAndValidToIsNull(Long pricingGroupId);
}
